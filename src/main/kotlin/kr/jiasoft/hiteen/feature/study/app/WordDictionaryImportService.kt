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
import java.time.OffsetDateTime
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

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

        val now = OffsetDateTime.now()

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


    /**
     * 같은 type 내에서만 중복 체크하고, 없으면 새로 생성하는 메서드
     * (다른 type에 있어도 해당 type에 없으면 생성)
     */
    suspend fun importWordsForceInsert(
        words: List<String>,
        type: Int = 1,
        category: String? = "초등영어",
        status: Int = 1
    ) {
        for (raw in words) {
            val word = raw.trim().lowercase()
            if (word.isBlank()) continue

            try {
                importSingleWordForceInsert(word, null, type, category, status)
                kotlinx.coroutines.delay(500)   // API 보호

            } catch (e: Exception) {
                println("❌ [$word] 단어 처리 중 오류: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun importSingleWordForceInsert(
        word: String,
        meaning: String?,
        type: Int,
        category: String?,
        status: Int
    ) {
        // ✅ 같은 type 내에서만 중복 체크
        val existingInSameType = questionRepository
            .findByLowCaseQuestionAndDeletedAtIsNull(word, type)
            .toList()

        if (existingInSameType.isNotEmpty()) {
            println("⏭ [$word] type=$type 에 이미 존재하여 스킵")
            return
        }

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

        val now = OffsetDateTime.now()

        // ✅ 신규 INSERT
        val entity = QuestionEntity(
            type = type,
            category = category,
            question = word,
            symbol = symbol,
            sound = soundPath,
            image = imagePath,
            answer = word,
            content = meaning,
            status = status,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )

        questionRepository.save(entity)
        println("✅ [$word] type=$type 신규 단어 저장 완료")
    }


    /**
     * words.txt 파일을 읽어서 초등영어 단어들의 content(뜻)를 업데이트하는 메서드
     * 형식: 분류\t단어\t뜻
     */
    suspend fun updateElementaryWordMeanings(wordsFilePath: String, type: Int = 1) {
        val wordsFile = java.io.File(wordsFilePath)
        if (!wordsFile.exists()) {
            println("❌ 파일이 존재하지 않습니다: $wordsFilePath")
            return
        }

        // 초등영어만 필터링하여 단어-뜻 맵 생성
        val wordMeaningMap = wordsFile.readLines()
            .filter { it.startsWith("초등영어") }
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size >= 3) {
                    val word = parts[1].trim().lowercase()
                    val meaning = parts[2].trim()
                    word to meaning
                } else null
            }
            .toMap()

        println("✅ words.txt에서 초등영어 단어-뜻 ${wordMeaningMap.size}개 로드 완료")

        var updatedCount = 0
        var skippedCount = 0

        for ((word, meaning) in wordMeaningMap) {
            try {
                val existingList = questionRepository
                    .findByLowCaseQuestionAndDeletedAtIsNull(word, type)
                    .toList()

                if (existingList.isEmpty()) {
                    skippedCount++
                    continue
                }

                for (existing in existingList) {
                    // content가 비어있거나 단어와 동일한 경우에만 업데이트
                    if (existing.content.isNullOrBlank() || existing.content == existing.question) {
                        val updated = existing.copy(
                            content = meaning,
                            updatedAt = OffsetDateTime.now()
                        )
                        questionRepository.save(updated)
                        println("✅ [$word] 뜻 업데이트 완료: $meaning")
                        updatedCount++
                    } else {
                        println("⏭ [$word] 이미 뜻이 있어서 스킵: ${existing.content}")
                        skippedCount++
                    }
                }
            } catch (e: Exception) {
                println("❌ [$word] 업데이트 중 오류: ${e.message}")
            }
        }

        println("✅ 완료! 업데이트: ${updatedCount}개, 스킵: ${skippedCount}개")
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
     * /app/assets/word_img/{word}.webp 또는 .jpg 존재 여부 확인 후
     * 있으면 /assets/word_img/{word}.webp (또는 .jpg) 반환
     */
    private fun resolveImagePath(word: String): String? {
        val wordLower = word.lowercase()
        val imgDir = Paths.get(assetStorageRoot, "word_img")

        // webp 우선 확인
        val webpFile = imgDir.resolve("${wordLower}.webp").toFile()
        if (webpFile.exists()) {
            println("🌄 사진 존재함: ${wordLower}.webp")
            return "/assets/word_img/${wordLower}.webp"
        }

        // jpg 확인
        val jpgFile = imgDir.resolve("${wordLower}.jpg").toFile()
        if (jpgFile.exists()) {
            println("🌄 사진 존재함: ${wordLower}.jpg")
            return "/assets/word_img/${wordLower}.jpg"
        }

        return null
    }


    /**
     * word_img 폴더의 이미지 파일들을 스캔하여
     * question_2 테이블의 image 컬럼을 업데이트하는 메서드
     */
    suspend fun updateImagePathsFromFolder(type: Int? = null) {
        val imgDir = Paths.get(assetStorageRoot, "word_img").toFile()

        if (!imgDir.exists() || !imgDir.isDirectory) {
            println("❌ word_img 폴더가 존재하지 않습니다: ${imgDir.absolutePath}")
            return
        }

        // webp, jpg 파일 목록 가져오기
        val imageFiles = imgDir.listFiles { file ->
            file.isFile && (file.extension.lowercase() == "webp" || file.extension.lowercase() == "jpg")
        } ?: emptyArray()

        println("✅ word_img 폴더에서 ${imageFiles.size}개 이미지 파일 발견")

        var updatedCount = 0
        var skippedCount = 0
        var notFoundCount = 0

        for (imageFile in imageFiles) {
            val word = imageFile.nameWithoutExtension.lowercase()
            val imagePath = "/assets/word_img/${imageFile.name}"

            try {
                // type이 지정되면 해당 type만, 아니면 모든 type
                val existingList = if (type != null) {
                    questionRepository.findByLowCaseQuestionAndDeletedAtIsNull(word, type).toList()
                } else {
                    questionRepository.findByQuestionIgnoreCaseAndDeletedAtIsNull(word).toList()
                }

                if (existingList.isEmpty()) {
                    notFoundCount++
                    continue
                }

                for (existing in existingList) {
                    // image가 비어있거나 null인 경우에만 업데이트
                    if (existing.image.isNullOrBlank()) {
                        val updated = existing.copy(
                            image = imagePath,
                            updatedAt = OffsetDateTime.now()
                        )
                        questionRepository.save(updated)
                        println("✅ [$word] 이미지 경로 업데이트: $imagePath")
                        updatedCount++
                    } else {
                        skippedCount++
                    }
                }
            } catch (e: Exception) {
                println("❌ [$word] 이미지 업데이트 중 오류: ${e.message}")
            }
        }

        println("✅ 완료! 업데이트: ${updatedCount}개, 스킵(이미 있음): ${skippedCount}개, DB에 없음: ${notFoundCount}개")
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
