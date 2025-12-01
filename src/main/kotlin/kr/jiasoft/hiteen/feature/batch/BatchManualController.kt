package kr.jiasoft.hiteen.feature.batch

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/batch")
class BatchManualController (
    private val batchService: BatchService
){

    @PostMapping("/schoolFoodImport")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun schoolFoodImport(){
        batchService.schoolFoodImport()
    }

    @PostMapping("/schoolImport")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun schoolImport(){
        batchService.schoolImport()
    }

    @PostMapping("/autoManageSeasons")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun autoManageSeasons(){
        batchService.autoManageSeasons()
    }


}