package kr.jiasoft.hiteen.feature.batch

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/batch")
class BatchManualController (
    private val batchService: BatchService
){

    @PostMapping("/schoolFoodImport")
    suspend fun schoolFoodImport(){
        batchService.schoolFoodImport()
    }

    @PostMapping("/schoolImport")
    suspend fun schoolImport(){
        batchService.schoolImport()
    }

    @PostMapping("/autoManageSeasons")
    suspend fun autoManageSeasons(){
        batchService.autoManageSeasons()
    }


}