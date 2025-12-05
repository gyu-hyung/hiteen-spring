package kr.jiasoft.hiteen.common.app

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
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
) : ServerHttpResponseDecorator(delegate) {

    private val mapper = jacksonObjectMapper()
    private val bufferFactory = delegate.bufferFactory()

    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {

        val modifiedBody = DataBufferUtils.join(body)
            .flatMap { dataBuffer ->

                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)

                val content = String(bytes)

                if (!content.trim().startsWith("{")) return@flatMap Mono.just(bytes)

                val json = try { mapper.readTree(content) } catch (_: Exception) {
                    return@flatMap Mono.just(bytes)
                }

                if (!json.has("success")) {
                    return@flatMap Mono.just(bytes)
                }

                principalMono.flatMap { auth ->

                    val user = auth.principal as? CustomUserDetails
                        ?: return@flatMap Mono.just(bytes)

                    Mono.deferContextual { ctx ->

                        buildMeta(user.user.id).map { baseMeta ->

                            val obj = json as ObjectNode

                            val deltaExp    = exchange.attributes["deltaExp"] as? Int ?: 0
                            val deltaPoint  = exchange.attributes["deltaPoints"] as? Int ?: 0

                            if (deltaExp == 0 && deltaPoint == 0) {
                                obj.putNull("meta")
                                return@map mapper.writeValueAsBytes(obj)
                            }

                            val metaNode = mapper.createObjectNode()
                            metaNode.put("totalExp",  baseMeta["totalExp"].toString().toInt())
                            metaNode.put("totalPoints", baseMeta["totalPoints"].toString().toInt())
                            metaNode.put("tier", baseMeta["tier"].toString().toInt())

                            if (deltaExp != 0) metaNode.put("deltaExp", deltaExp)
                            if (deltaPoint != 0) metaNode.put("deltaPoint", deltaPoint)

                            obj.set<ObjectNode>("meta", metaNode)

                            mapper.writeValueAsBytes(obj)
                        }
                    }
                }

            }
            .defaultIfEmpty(ByteArray(0))

        this.headers.remove("Content-Length")
        this.headers.set("Transfer-Encoding", "chunked")

        return super.writeWith(modifiedBody.map { bufferFactory.wrap(it) })
    }

    private fun buildMeta(userId: Long): Mono<Map<String, Any>> {
        return mono {
            val user = userRepository.findById(userId) ?: return@mono emptyMap()
            val summary = pointSummaryRepository.findSummaryByUserId(userId)
            val tier = tierRepository.findById(user.tierId)

            mapOf(
                "totalExp" to (user.expPoints),
                "totalPoints" to (summary?.totalPoint ?: 0),
                "tier" to (tier?.rankOrder ?: 0)
            )
        }
    }
}
