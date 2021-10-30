package com.udaychugh.kalam.data.wrappers

import com.udaychugh.kalam.data.models.User
import io.reactivex.Observable

data class ObservableUser (
        override val id: String,
        val user: Observable<User>
): ItemViewModel {}