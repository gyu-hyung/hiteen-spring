package kr.jiasoft.hiteen.feature.notification.app

import kr.jiasoft.hiteen.feature.notification.dto.PushTemplateGroupResponse
import kr.jiasoft.hiteen.feature.notification.dto.PushTemplateInfo
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.PushTemplateGroup
import org.springframework.stereotype.Service

@Service
class NotificationTemplateService {

    fun getPushTemplatesGrouped(group: PushTemplateGroup?): List<PushTemplateGroupResponse> {
        val templates = PushTemplate.entries
            .filter { group == null || it.group == group }

        return templates
            .groupBy { it.group }
            .entries
            .sortedBy { it.key.ordinal }
            .map { (g, list) ->
                PushTemplateGroupResponse(
                    groupCode = g.code,
                    groupTitle = g.title,
                    templates = list
                        .sortedBy { it.code }
                        .map { t ->
                            PushTemplateInfo(
                                code = t.code,
                                title = t.title,
                                message = t.message,
                            )
                        }
                )
            }
    }
}
