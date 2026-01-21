package kr.jiasoft.hiteen.common.app

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.common.context.MetaDeltaKeys
import kr.jiasoft.hiteen.feature.cash.infra.CashSummaryRepository
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.point.infra.PointSummaryRepository
import kr.jiasoft.hiteen.feature.user.dto.CustomUserDetails
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.security.core.Authentication
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class MetaAppendingResponseDecorator(
    delegate: ServerHttpResponse,
    private val exchange: ServerWebExchange,
    private val principalMono: Mono<Authentication>,
    private val userRepository: UserRepository,
    private val tierRepository: TierRepository,
    private val pointSummaryRepository: PointSummaryRepository,
    private val cashSummaryRepository: CashSummaryRepository,
) : ServerHttpResponseDecorator(delegate) {

    private val mapper = jacksonObjectMapper()
    private val bufferFactory = delegate.bufferFactory()

    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {

        return DataBufferUtils.join(body)
            .flatMap { dataBuffer ->

                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)

                val content = String(bytes)

                // JSON 아니면 그대로 반환
                if (!content.trim().startsWith("{")) {
                    return@flatMap super.writeWith(Mono.just(bufferFactory.wrap(bytes)))
                }

                val json = try { mapper.readTree(content) } catch (e: Exception) {
                    return@flatMap super.writeWith(Mono.just(bufferFactory.wrap(bytes)))
                }

                if (!json.has("success")) {
                    return@flatMap super.writeWith(Mono.just(bufferFactory.wrap(bytes)))
                }

                principalMono
                    .onErrorResume { Mono.empty() }
                    .flatMap { auth ->

                        val user = auth.principal as? CustomUserDetails
                            ?: return@flatMap Mono.just(bytes)

                        buildMeta(user.user.id).flatMap flatMapBytes@{ baseMeta ->

                            val obj = json as ObjectNode

                            val deltaExp   = exchange.attributes[MetaDeltaKeys.DELTA_EXP] as? Int ?: 0
                            val deltaPoint = exchange.attributes[MetaDeltaKeys.DELTA_POINT] as? Int ?: 0
                            val deltaCash = exchange.attributes[MetaDeltaKeys.DELTA_CASH] as? Int ?: 0
                            val deltaTier = exchange.attributes[MetaDeltaKeys.DELTA_TIER] as? Map<*, *>

                            // ✅ 포인트 이벤트 등 meta 주입을 원치 않는 요청은 meta를 붙이지 않는다.
                            val skipMeta = exchange.attributes[MetaDeltaKeys.SKIP_META] as? Boolean ?: false
                            if (skipMeta) {
                                return@flatMapBytes Mono.just(bytes)
                            }

                            if (deltaExp == 0 && deltaPoint == 0 && deltaCash == 0) {
                                return@flatMapBytes Mono.just(bytes)
                            }

                            val metaNode = mapper.createObjectNode()
                            metaNode.put("totalExp", (baseMeta["totalExp"] ?: 0).toString().toInt())
                            metaNode.put("totalPoints", (baseMeta["totalPoints"] ?: 0).toString().toInt())
                            metaNode.put("totalCash", (baseMeta["totalCash"] ?: 0).toString().toInt())
                            metaNode.put("tier", (baseMeta["tier"] ?: 0).toString().toInt())

                            if (deltaExp != 0) metaNode.put("deltaExp", deltaExp)
                            if (deltaPoint != 0) metaNode.put("deltaPoint", deltaPoint)
                            if (deltaCash != 0) metaNode.put("deltaCash", deltaCash)
                            if (deltaTier != null) {
                                val tierNode = mapper.createObjectNode()
                                tierNode.put("from", deltaTier["from"] as Int)
                                tierNode.put("to", deltaTier["to"] as Int)
                                metaNode.set<ObjectNode>("deltaTier", tierNode)
                            }

                            obj.set<ObjectNode>("meta", metaNode)
                            Mono.just(mapper.writeValueAsBytes(obj))
                        }
                    }
                    .defaultIfEmpty(bytes)
                    .flatMap { finalBytes ->

                        // ✅ Content-Length 재설정
                        this.headers.remove("Content-Length")
                        this.headers.contentLength = finalBytes.size.toLong()

                        super.writeWith(Mono.just(bufferFactory.wrap(finalBytes)))
                    }
            }
    }



    private fun buildMeta(userId: Long): Mono<Map<String, Any>> {
        return mono {
            val user = userRepository.findById(userId) ?: return@mono emptyMap()
            val pointSummary = pointSummaryRepository.findSummaryByUserId(userId)
            val cashSummary = cashSummaryRepository.findSummaryByUserId(userId)
            val tier = tierRepository.findById(user.tierId)

            mapOf(
                "totalExp" to (user.expPoints),
                "totalPoints" to (pointSummary?.totalPoint ?: 0),
                "totalCash" to (cashSummary?.totalCash ?: 0),
                "tier" to (tier?.rankOrder ?: 0)
            )
        }
    }
}
