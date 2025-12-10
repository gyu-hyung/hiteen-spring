package kr.jiasoft.hiteen.feature.study.app

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kr.jiasoft.hiteen.feature.study.domain.QuestionEntity
import kr.jiasoft.hiteen.feature.study.dto.DictionaryEntry
import kr.jiasoft.hiteen.feature.study.dto.DictionaryPhonetic
import kr.jiasoft.hiteen.feature.study.dto.ExcelWordRow
import kr.jiasoft.hiteen.feature.study.infra.QuestionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime

@Service
class WordDictionaryImportService(
    private val questionRepository: QuestionRepository,
    @Value("\${app.asset.storage-root}")
    private val assetStorageRoot: String,      // 예: /app/assets
    webClientBuilder: WebClient.Builder
) {

    // https://api.dictionaryapi.dev/api/v2/entries/en/{word}
    private val dictClient: WebClient = webClientBuilder
        .baseUrl("https://api.dictionaryapi.dev")
        .build()

    /**
     * 단어 리스트를 받아서
     *  - dictionaryapi.dev 호출
     *  - 미국식 mp3 다운로드 (/app/assets/sound/{word}.mp3)
     *  - 이미지 존재 확인 (/app/assets/word_img/{word}.webp)
     *  - question_1에 insert
     */
//    suspend fun importWords(
//        words: List<String>,
//        type: Int = 1,
//        category: String? = "초등영어",
//        status: Int = 1
//    ) {
//        for (raw in words) {
//            val word = raw.trim().lowercase()
//            if (word.isBlank()) continue
//
//            try {
//                importSingleWord(word, type, category, status)
//                kotlinx.coroutines.delay(500)   // 0.5초
//
//            } catch (e: Exception) {
//                println("❌ [$word] 단어 처리 중 오류: ${e.message}")
//                e.printStackTrace()
//            }
//        }
//    }



    suspend fun importExcelWords(
        rows: List<ExcelWordRow>,
        type: Int = 1,
        category: String? = "초등영어",
        status: Int = 1
    ) {
        for (row in rows) {
            val word = row.word.trim().lowercase()
            val meaning = row.meaning?.trim()

            if (word.isBlank()) continue

            try {
                importSingleWord(word, meaning, type, category, status)
                kotlinx.coroutines.delay(500)   // ✅ API 보호

            } catch (e: Exception) {
                println("❌ [$word] 단어 처리 중 오류: ${e.message}")
                e.printStackTrace()
            }
        }
    }



    private suspend fun importSingleWord(
        word: String,
        meaning: String?,
        type: Int,
        category: String?,
        status: Int
    ) {
        // 1) 사전 API 호출
        val entry = fetchDictionaryEntry(word) ?: run {
            println("⚠ [$word] dictionaryapi.dev 결과 없음")
            return
        }

        val bestPhonetic = chooseBestPhonetic(entry)
        val symbol = resolvePhoneticSymbol(entry, bestPhonetic)
        val audioUrl = bestPhonetic?.audio

        val soundPath = downloadAndResolveSound(word, audioUrl)
        val imagePath = resolveImagePath(word)

        val now = LocalDateTime.now()

        val existingList = questionRepository
            .findByLowCaseQuestionAndDeletedAtIsNull(word, type)
            .toList()

        // ✅ 기존 데이터 있으면 → sound/image만 갱신
        if (existingList.isNotEmpty()) {
            existingList.forEach { existing ->
                val updated = existing.copy(
                    sound = soundPath ?: existing.sound,
                    image = imagePath ?: existing.image,
                    updatedAt = now
                )
                questionRepository.save(updated)
            }

            println("🔁 [$word] 기존 단어 ${existingList.size}건 갱신 완료")
            return
        }

        // ✅ 신규 INSERT (뜻 content 포함)
        val entity = QuestionEntity(
            type = type,
            category = category,
            question = word,
            symbol = symbol,
            sound = soundPath,
            image = imagePath,
            answer = word,
            content = meaning,   // ✅ 뜻 저장
            status = status,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )

        questionRepository.save(entity)
        println("✅ [$word] 신규 단어 저장 완료 (뜻 포함)")
    }



    // ==========================
    //  사전 API 호출 & 파싱
    // ==========================

    private suspend fun fetchDictionaryEntry(word: String): DictionaryEntry? {
        return dictClient.get()
            .uri("/api/v2/entries/en/{word}", word)
            .retrieve()
            .bodyToMono<List<DictionaryEntry>>()
            .map { it.firstOrNull() }
            .onErrorResume { e ->
                println("❌ dictionaryapi.dev 호출 실패 [$word]: ${e.message}")
                Mono.justOrEmpty(null)
            }
            .awaitSingleOrNull()
    }

    /**
     * 미국식 우선으로 phonetic 선택
     */
    private fun chooseBestPhonetic(entry: DictionaryEntry): DictionaryPhonetic? {
        val phoneticsWithAudio = entry.phonetics.filter { !it.audio.isNullOrBlank() }
        if (phoneticsWithAudio.isEmpty()) return null

        // 1순위: audio 경로에 -us 또는 us.mp3 포함된 것
        val us = phoneticsWithAudio.firstOrNull {
            val url = it.audio!!.lowercase()
            url.contains("-us") || url.contains("us.mp3")
        }

        return us ?: phoneticsWithAudio.first()
    }

    /**
     * 발음기호 문자열 결정
     * - entry.phonetic 우선
     * - 없으면 선택된 phonetic.text
     */
    private fun resolvePhoneticSymbol(entry: DictionaryEntry, chosen: DictionaryPhonetic?): String? {
        return entry.phonetic
            ?: chosen?.text
    }

    // ==========================
    //  파일 경로 / 다운로드 처리
    // ==========================

    /**
     * 미국식 mp3를 /app/assets/sound/{word}.mp3 로 저장 후
     * 실제 파일이 있으면 /assets/sound/{word}.mp3를 반환
     */
    private suspend fun downloadAndResolveSound(word: String, audioUrl: String?): String? {
        if (audioUrl.isNullOrBlank()) return null

        val fileName = "${word.lowercase()}.mp3"
        val soundDir: Path = Paths.get(assetStorageRoot, "sound")

        if (!Files.exists(soundDir)) {
            Files.createDirectories(soundDir)
        }

        val soundFile = soundDir.resolve(fileName)

        // 이미 파일이 있다면 그대로 사용
        if (!Files.exists(soundFile)) {
            try {
                val bytes = dictClient.get()
                    .uri(audioUrl)
                    .retrieve()
                    .bodyToMono(ByteArray::class.java)
                    .awaitSingleOrNull()

                if (bytes == null || bytes.isEmpty()) {
                    println("⚠ [$word] 오디오 다운로드 실패(빈 데이터)")
                    return null
                }
//
                Files.write(
                    soundFile,
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                )

                println("🎵 [$word] 오디오 파일 저장: $soundFile")
            } catch (e: Exception) {
                println("❌ [$word] 오디오 다운로드 중 오류: ${e.message}")
                return null
            }
        } else {
            println("ℹ [$word] 이미 존재하는 오디오 파일 사용: $soundFile")
        }

        return if (Files.exists(soundFile)) "/assets/sound/$fileName" else null
    }

    /**
     * /app/assets/word_img/{word}.webp 존재 여부 확인 후
     * 있으면 /assets/word_img/{word}.webp 반환
     */
    private fun resolveImagePath(word: String): String? {
        val fileName = "${word.lowercase()}.webp"
        val imgFile = Paths.get(assetStorageRoot, "word_img", fileName).toFile()

        return if (imgFile.exists()) {
            println("🌄 사진 존재함: $fileName")
            "/assets/word_img/$fileName"
        } else {
            null
        }
    }

    /**
     * id 생성 로직은 실제 사용 중인 방식에 맞춰 수정
     *  - 시퀀스 호출
     *  - Snowflake
     *  - UUID → Long 변환 등
     */
    private fun generateQuestionId(): Long {
        // TODO: 실제 ID 생성 로직으로 교체
        return System.currentTimeMillis()
    }
}
