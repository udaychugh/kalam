package com.udaychugh.kalam.data.wrappers

import com.udaychugh.kalam.data.models.Meme
import io.reactivex.Observable

data class ObservableMeme(
        override val id: String,
        val meme: Observable<Meme>
): ItemViewModel {
}