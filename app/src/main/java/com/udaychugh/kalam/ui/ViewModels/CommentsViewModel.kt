package com.udaychugh.kalam.ui.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch

class CommentsViewModel constructor(private val repository: CommentsRepository): ViewModel() {
    private val _genericResponseLiveData = MutableLiveData<GenericResponse>()
    val genericResponseLiveData: MutableLiveData<GenericResponse>
        get() = _genericResponseLiveData

    private val _showEmptyStateLiveData = MutableLiveData<Boolean>()
    val showEmptyStateLiveData: MutableLiveData<Boolean>
        get() = _showEmptyStateLiveData

    init {
        _showEmptyStateLiveData.value = false
    }

    /**
     * Function to post new comment
     * @param comment - The comment model
     */
    fun postComment(comment: Comment) {
        viewModelScope.launch {
            _genericResponseLiveData.value = GenericResponse.loading()

            when (val result = repository.postComment(comment)) {
                is Result.Success -> {
                    _genericResponseLiveData.value = GenericResponse.success(result.data, item = GenericResponse.ITEM_RESPONSE.POST_COMMENT)
                }

                is Result.Error -> {
                    _genericResponseLiveData.value = GenericResponse.error(result.error)
                }
            }
        }
    }

    /**
     * Function to delete comment
     * @param memeId - ID of the meme
     * @param commentId - ID of the comment
     */
    fun deleteComment(memeId: String, commentId: String) {
        viewModelScope.launch {
            _genericResponseLiveData.value = GenericResponse.loading()

            when (val result = repository.deleteComment(memeId, commentId)) {
                is Result.Success -> {
                    _genericResponseLiveData.value = GenericResponse.success(result.data,
                            item = GenericResponse.ITEM_RESPONSE.DELETE_COMMENT,
                            value = commentId)
                }

                is Result.Error -> {
                    _genericResponseLiveData.value = GenericResponse.error(result.error)
                }
            }
        }
    }

    /**
     * Function to fetch all comments for a Meme
     */
    fun fetchComments(memeId: String): LiveData<PagedList<ObservableComment>> {
        val pagingConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(5)
                .build()

        val commentFactory = CommentsDataSource.Factory(repository, viewModelScope, memeId) {
            _showEmptyStateLiveData.value = it
        }

        return LivePagedListBuilder<String, ObservableComment>(commentFactory, pagingConfig).build()
    }
}