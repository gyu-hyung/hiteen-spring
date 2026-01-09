package kr.jiasoft.hiteen.feature.school.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.school.domain.SchoolClassesEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SchoolClassesRepository : CoroutineCrudRepository<SchoolClassesEntity, Long> {

    @Modifying
    @Query("UPDATE school_classes SET updated_id = -1 WHERE (:year is NULL OR year = :year)")
    suspend fun markAllForDeletionByYear(year: Int?): Int

    @Modifying
    @Query("DELETE FROM school_classes WHERE updated_id = -1 AND (:year is NULL OR year = :year)")
    suspend fun deleteMarkedForDeletionByYear(year: Int?): Int

    // ✅ 특정 학년도만 조회
    @Query("SELECT * FROM school_classes WHERE year = :year")
    fun findByYear(year: Int): Flow<SchoolClassesEntity>

    @Query("SELECT * FROM school_classes WHERE year = :year AND school_id = :schoolId ORDER BY id, grade, class_no")
    fun findBySchoolIdAndYear(schoolId: Long, year: Int): Flow<SchoolClassesEntity>

    @Query("SELECT DISTINCT school_id FROM school_classes WHERE year = :year GROUP BY school_id ORDER BY school_id")
    fun findDistinctSchoolIds(year: Int): Flow<Long>


    @Query("""
        INSERT INTO school_classes (
            code, year, school_id, school_name, school_type,
            class_name, major, grade, class_no,
            created_at, updated_at, updated_id
        )
        VALUES (
            :code, :year, :schoolId, :schoolName, :schoolType,
            :className, :major, :grade, :classNo,
            :createdAt, :updatedAt, :updatedId
        )
        ON CONFLICT (school_id, year, grade, class_no)
        DO UPDATE SET
            class_name = EXCLUDED.class_name,
            major = EXCLUDED.major,
            school_name = EXCLUDED.school_name,
            school_type = EXCLUDED.school_type,
            updated_at = EXCLUDED.updated_at,
            updated_id = EXCLUDED.updated_id
    """)
    suspend fun upsert(
        code: String,
        year: Int,
        schoolId: Long,
        schoolName: String,
        schoolType: Int,
        className: String,
        major: String,
        grade: String,
        classNo: String,
        createdAt: LocalDateTime,
        updatedAt: LocalDateTime,
        updatedId: Long,
    )

    //AND year = EXTRACT(YEAR FROM CURRENT_DATE)
    @Query("""
        SELECT DISTINCT grade
        FROM school_classes
        WHERE school_id = :schoolId
          AND deleted_at IS NULL
        ORDER BY grade
    """)
    fun findGradesBySchoolId(schoolId: Long): Flow<String>

    // ✅ 특정 학교 + 학년의 학급 목록 조회
    @Query("""
        SELECT *
        FROM school_classes
        WHERE school_id = :schoolId
          AND grade = :grade
          AND deleted_at IS NULL
          AND year = :year
        ORDER BY class_no
    """)
    fun findBySchoolIdAndGrade(
        schoolId: Long,
        grade: String,
        year: Int,
    ): Flow<SchoolClassesEntity>


}
