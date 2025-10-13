package kr.jiasoft.hiteen.feature.play.infra

import kr.jiasoft.hiteen.feature.play.domain.QuestionItemsEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface QuestionItemsRepository : CoroutineCrudRepository<QuestionItemsEntity, Long>
