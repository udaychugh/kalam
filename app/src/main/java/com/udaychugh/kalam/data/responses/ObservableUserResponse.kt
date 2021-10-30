package com.udaychugh.kalam.data.responses

import com.udaychugh.kalam.data.Status
import com.udaychugh.kalam.data.wrappers.ObservableUser

data class ObservableUserResponse (
        val status: Status,
        val user: ObservableUser?,
        val error: String?
) {
    companion object {
        fun loading(): ObservableUserResponse = ObservableUserResponse(Status.LOADING, null, null)

        fun success(user: ObservableUser): ObservableUserResponse
                = ObservableUserResponse(Status.SUCCESS, user, null)

        fun error(error: String): ObservableUserResponse = ObservableUserResponse(Status.ERROR, null, error)
    }
}