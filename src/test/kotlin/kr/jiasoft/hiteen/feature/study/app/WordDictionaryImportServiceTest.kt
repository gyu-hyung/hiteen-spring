package kr.jiasoft.hiteen.feature.study.app

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



}