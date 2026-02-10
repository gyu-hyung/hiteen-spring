package kr.jiasoft.hiteen.feature.batch

import org.springframework.context.annotation.Profile
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile(value = ["prod"])
@RestController
@RequestMapping("/api/batch")
class BatchManualController (
    private val batchService: BatchService
){

    @PostMapping("/schoolImport")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun schoolImport(){
        batchService.schoolImport()
    }

    @PostMapping("/schoolFoodImport")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun schoolFoodImport(){
        batchService.schoolFoodImport()
    }

    @PostMapping("/timeTableImport")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun schoolTimeTableImport(){
        batchService.timeTableImport()
    }


    @PostMapping("/autoManageSeasons")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun autoManageSeasons(){
        batchService.autoManageSeasons()
    }


}