package com.udaychugh.kalam.data.wrappers

import com.udaychugh.kalam.data.models.Comment
import io.reactivex.Observable

data class ObservableComment(
        val id: String,
        val comment: Observable<Comment>
) {}