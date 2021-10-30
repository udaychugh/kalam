package com.udaychugh.kalam.data.responses

import com.udaychugh.kalam.data.Status

data class GenericResponse (
        val status: Status,
        val success: Boolean? = null,
        val error: String? = null,
        val item: ITEM_RESPONSE? = null,
        val value: String? = null
) {
    companion object {
        fun loading(): GenericResponse = GenericResponse(Status.LOADING)

        fun success(success: Boolean, item: ITEM_RESPONSE? = null, value: String? = null): GenericResponse
                = GenericResponse(Status.SUCCESS, success, item = item, value =  value)

        fun error(error: String): GenericResponse = GenericResponse(Status.ERROR, error =  error)
    }

    enum class ITEM_RESPONSE {
        DELETE_MEME,
        REPORT_MEME,
        DELETE_FAVE,
        POST_COMMENT,
        DELETE_COMMENT,
        UPDATE_AVATAR,
        FAVE_MEME,
        POST_MEME
    }
}