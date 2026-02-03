package kr.jiasoft.hiteen.feature.study.app

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.study.dto.ExcelWordRow
import kr.jiasoft.hiteen.feature.study.infra.QuestionRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


import java.io.FileInputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook


@SpringBootTest
class WordDictionaryImportServiceTest {

    @Autowired
    lateinit var wordDictionaryImportService: WordDictionaryImportService

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Test
    fun `단어 import`() = runBlocking {
        val words = listOf(
            "apple",
            "banana",
            "cat",
            "dog",
            "egg",
            "fish",
            "house",
            "milk",
            "school",
            "tree"
        )

//        wordDictionaryImportService.importWords(words)

    }


//    @Test
//    fun `기존 단어 import`() = runBlocking {
//
//        val questions = questionRepository.findBySoundIsNull().map { it.question }.toList()
//
//        wordDictionaryImportService.importWords(questions)
//
//
//
//    }

    @Test
    fun `엑셀 초등 단어 import`() = runBlocking {

        val filePath = "/Users/jogyuhyeong/Downloads/하이틴영단어목록_20250916.xlsx"   // ✅ 네 엑셀 경로
        val fis = FileInputStream(filePath)
        val workbook = XSSFWorkbook(fis)
        val sheet = workbook.getSheet("전체")
            ?: throw IllegalStateException("엑셀에 '전체' 시트가 존재하지 않습니다.")


        val rows = mutableListOf<ExcelWordRow>()

        // ✅ 1행은 헤더 → 2행부터 시작
        for (row in sheet.drop(1)) {

            val a = row.getCell(0)?.toString()?.trim()   // A열 (과목분류코드)
            val word = row.getCell(2)?.toString()?.trim()       // C열 (영단어)
            val meaning = row.getCell(3)?.toString()?.trim()    // D열 (뜻)

            // ✅ A열 비어있는 것만 = 초등
            if (a.isNullOrBlank() && !word.isNullOrBlank()) {
                rows.add(
                    ExcelWordRow(
                        word = word.lowercase(),
                        meaning = meaning
                    )
                )
            }
        }

        workbook.close()

        println("✅ 엑셀에서 추출된 초등 단어 수: ${rows.size}")

        // ✅ 기존 서비스 로직 재사용
        wordDictionaryImportService.importExcelWords(rows, 1, "초등영어")
    }


    @Test
    fun `words txt에만 있는 단어 import`() = runBlocking {
        // DB에 없는 143개 단어 (words.txt에만 있는 단어들)
        val wordsToAdd = listOf(
            "amaxing", "angry", "anybody", "anyone", "april", "atlantic", "august", "avoard",
            "bakery", "beach", "because", "begin", "believe", "bored", "bottle", "break",
            "cash", "china", "circle", "classic", "coin", "confucius", "cook", "country",
            "cxchange", "damage", "danger", "dark", "daughter", "december", "die", "dirty",
            "domestic", "duck", "early", "earth", "easy", "excite", "fall", "februauy",
            "filter", "flash", "foggy", "full", "future", "giant", "grandfather", "grandmother",
            "guide", "guility", "hate", "heal", "hell", "history", "hobby", "hole", "homework",
            "honesty", "hornor", "hungry", "imagine", "impossible", "inside", "january", "july",
            "june", "knock", "lesson", "light", "lock", "loser", "magical", "may", "mediterranean",
            "military", "miss", "mission", "mix", "mouse", "muslim", "neither", "north", "northern",
            "november", "october", "offend", "once", "original", "outside", "pack", "pass", "plant",
            "playground", "pleasure", "position", "prize", "promise", "quick", "ready", "rent",
            "report", "self", "senior", "september", "shake", "shout", "shower", "shut", "slide",
            "snake", "soldier", "somebody", "someone", "south", "southern", "spider", "spring",
            "straight", "subway", "sudden", "sunlight", "survive", "than", "theater", "tired",
            "tour", "travel", "triangle", "trouble", "underwater", "untill(=till)", "vegetable",
            "village", "voice", "wave", "wedding", "weight", "wide", "winter"
        )

        println("✅ 추가할 단어 수: ${wordsToAdd.size}")

        val rows = wordsToAdd.map { ExcelWordRow(word = it, meaning = null) }

        // type=1로 저장 (초등영어)
        wordDictionaryImportService.importExcelWords(rows, 1, "초등영어")
    }


    @Test
    fun `import all words from words txt as elementary english`() = runBlocking {
        // words.txt 파일 읽기 - 초등영어만 필터링
        val wordsFile = java.io.File("src/main/kotlin/kr/jiasoft/hiteen/context/words.txt")
        val words = wordsFile.readLines()
            .filter { it.startsWith("초등영어") }  // 초등영어만 필터
            .map { it.split("\t").getOrNull(1)?.trim()?.lowercase() ?: "" }
            .filter { it.isNotBlank() }
            .distinct()

        println("✅ words.txt 초등영어 유니크 단어 수: ${words.size}")

        // 같은 type 내 중복 체크 후 type=1 초등영어로 생성
        wordDictionaryImportService.importWordsForceInsert(words, 1, "초등영어")
    }


    @Test
    fun `update elementary word meanings from words txt`() = runBlocking {
        // words.txt에서 초등영어 단어들의 뜻을 업데이트
        val wordsFilePath = "src/main/kotlin/kr/jiasoft/hiteen/context/words.txt"
        wordDictionaryImportService.updateElementaryWordMeanings(wordsFilePath, 1)
    }


    @Test
    fun `update image paths from word_img folder`() = runBlocking {
        // word_img 폴더의 이미지 파일들을 스캔하여 image 컬럼 업데이트
        // type=1 (초등영어)만 업데이트
        wordDictionaryImportService.updateImagePathsFromFolder(2)
        wordDictionaryImportService.updateImagePathsFromFolder(3)
    }



    @Test
    fun `check images not in db`() = runBlocking {
        // word_img 폴더에 있지만 DB에 없는 단어 확인
        val imgDir = java.io.File("/Users/jogyuhyeong/assets/word_img")

        if (!imgDir.exists()) {
            println("❌ word_img 폴더가 존재하지 않습니다: ${imgDir.absolutePath}")
            return@runBlocking
        }

        val imageFiles = imgDir.listFiles { file ->
            file.isFile && (file.extension.lowercase() == "webp" || file.extension.lowercase() == "jpg")
        } ?: emptyArray()

        println("✅ word_img 폴더 이미지 파일 수: ${imageFiles.size}")

        val imageWords = imageFiles.map { it.nameWithoutExtension.lowercase() }.toSet()

        // DB에서 모든 단어 조회
        val dbWords = questionRepository.findAll()
            .toList()
            .filter { it.deletedAt == null }
            .map { it.question.lowercase() }
            .toSet()

        println("✅ DB 단어 수: ${dbWords.size}")

        // word_img에는 있지만 DB에 없는 단어
        val imgOnlyWords = imageWords - dbWords

        println("\n=== word_img에는 있지만 DB에 없는 단어 (${imgOnlyWords.size}개) ===")
        imgOnlyWords.sorted().forEach { println(it) }
    }



}