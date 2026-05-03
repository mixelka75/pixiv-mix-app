package wtf.mxl.pixmix.shared.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class AjaxEnvelope<T>(
    val error: Boolean = false,
    val message: String = "",
    val body: T? = null,
)
