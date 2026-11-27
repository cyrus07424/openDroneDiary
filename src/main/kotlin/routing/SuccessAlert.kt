package routing

import kotlinx.html.FlowContent
import kotlinx.html.div

fun FlowContent.successAlert(result: String?) {
    val message = when (result) {
        "saved" -> "保存しました"
        "deleted" -> "削除しました"
        else -> return
    }
    div(classes = "alert alert-success") {
        attributes["role"] = "alert"
        +message
    }
}
