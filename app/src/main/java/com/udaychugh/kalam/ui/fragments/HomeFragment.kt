package com.udaychugh.kalam.ui.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cocosw.bottomsheet.BottomSheet
import kotlinx.android.synthetic.main.fragment_home.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class HomeFragment : BaseFragment() {
    private lateinit var memesAdapter: MemesAdapter
    private lateinit var bs: BottomSheet.Builder
    private val memesViewModel: MemesViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val binding = DataBindingUtil.inflate<FragmentHomeBinding>(inflater, R.layout.fragment_home, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()

        initStatusObserver()
        initMemesObserver()
        initResponseObserver()
    }

    private fun initViews() {
        memesAdapter = MemesAdapter(memesCallback)
        memesAdapter.setHasStableIds(false)

        homeRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = null
            addItemDecoration(RecyclerFormatter.DoubleDividerItemDecoration(activity!!))
            adapter = memesAdapter
            AppUtils.handleHomeScrolling(this)
        }

        homeRefresh.setOnRefreshListener {
            memesAdapter.currentList?.dataSource?.invalidate()
            runDelayed(2500) { homeRefresh.isRefreshing = false }
        }
    }

    /**
     * Initialize function to observer Empty State LiveData
     */
    private fun initStatusObserver() {
        memesViewModel.showStatusLiveData.observe(this, Observer {
            when (it) {
                Status.LOADING -> {
                    emptyState.hideView()
                    homeShimmer.showView()
                    homeShimmer.startShimmerAnimation()
                }
                Status.SUCCESS -> {
                    homeShimmer.stopShimmerAnimation()

                    if (Connectivity.isConnected(activity!!)) {
                        hideViews(homeShimmer, emptyState)
                        homeRv.showView()
                    } else {
                        showEmptyState()
                    }
                }
                else -> {
                    showEmptyState()
                }
            }
        })
    }

    /**
     * Initialize observer for Memes LiveData
     */
    private fun initMemesObserver() {
        memesViewModel.fetchMemes().observe(this, Observer {
            memesAdapter.submitList(it as PagedList<ItemViewModel>)
        })
    }

    /**
     * Initialize observer for Generic Response LiveData
     */
    private fun initResponseObserver() {
        memesViewModel.genericResponseLiveData.observe(this, Observer {
            when (it.status) {
                Status.LOADING -> { Timber.e("Loading...") }

                Status.SUCCESS -> {
                    when (it.item) {
                        GenericResponse.ITEM_RESPONSE.DELETE_MEME -> toast("Meme deleted \uD83D\uDEAE️")
                        GenericResponse.ITEM_RESPONSE.REPORT_MEME -> toast("Meme reported \uD83D\uDC4A")
                        GenericResponse.ITEM_RESPONSE.FAVE_MEME -> EventBus.getDefault().post(PostMemeEvent(PostMemeEvent.TYPE.FAVORITE))
                        else -> Timber.e("Success \uD83D\uDE03")
                    }
                }

                Status.ERROR -> { toast("${it.error}. Please try again") }
            }
        })
    }

    private val memesCallback = object : MemesCallback {
        override fun onMemeClicked(view: View, meme: Meme) {
            val memeId = meme.id!!

            when(view.id) {
                R.id.memeComment -> showComments(memeId)
                R.id.memeIcon, R.id.memeUser -> showProfile(meme.memePosterID!!)

                R.id.memeFave -> {
                    AppUtils.animateView(view)
                    memesViewModel.faveMeme(memeId, getUid())
                }

                R.id.memeLike -> {
                    AppUtils.animateView(view)
                    memesViewModel.likeMeme(memeId, getUid())
                }

                else -> {
                    doAsync {
                        // Get bitmap of shown meme
                        val imageBitmap = when(view.id) {
                            R.id.memeImage, R.id.memeMore -> AppUtils.loadBitmapFromUrl(activity!!, meme.imageUrl!!)
                            else -> null
                        }

                        uiThread {
                            imageBitmap?.let {
                                if (view.id == R.id.memeMore) showBottomSheet(meme, imageBitmap)
                                else showMeme(meme, imageBitmap)
                            }
                        }
                    }
                }
            }
        }

        override fun onProfileClicked(view: View, user: User) {
            // Not used here
        }
    }

    /**
     * Launch the Profile of the meme poster
     * @param userId - ID of the user
     */
    private fun showProfile(userId: String) {
        if (userId != getUid()) {
            val i = Intent(activity, ProfileActivity::class.java)
            i.putExtra(Constants.USER_ID, userId)
            startActivity(i)
            AppUtils.slideRight(activity!!)
        }
    }

    /**
     * Launch activity to view full meme photo
     */
    private fun showMeme(meme: Meme, image: Bitmap) {
        AppUtils.saveTemporaryImage(activity!!, image)

        val i = Intent(activity, ViewMemeActivity::class.java)
        i.putExtra(Constants.PIC_URL, meme.imageUrl)
        i.putExtra(Constants.CAPTION, meme.caption)
        startActivity(i)
        AppUtils.fadeIn(activity!!)
    }

    /**
     * Show BottomSheet with extra actions
     */
    private fun showBottomSheet(meme: Meme, image: Bitmap) {
        bs = when (sessionManager.getAdminStatus()) {
            Constants.ADMIN, Constants.SUPER_ADMIN -> BottomSheet.Builder(activity!!).sheet(R.menu.main_bottomsheet_admin)
            else -> {
                if (getUid() != meme.memePosterID) {
                    BottomSheet.Builder(activity!!).sheet(R.menu.main_bottomsheet)
                } else {
                    BottomSheet.Builder(activity!!).sheet(R.menu.main_bottomsheet_me)
                }
            }
        }

        bs.listener { _, which ->
            when(which) {
                R.id.bs_share -> AppUtils.shareImage(activity!!, image)

                R.id.bs_delete -> {
                    activity!!.alert("Delete this meme?") {
                        title = "Delete Meme"
                        positiveButton("Delete") { memesViewModel.deleteMeme(meme.id!!) }
                        negativeButton("Cancel") {}
                    }.show()
                }

                R.id.bs_save -> {
                    AppUtils.requestStoragePermission(activity!!) { granted ->
                        if (granted) AppUtils.saveImage(activity!!, image)
                        else longToast("Storage permission is required to save memes")
                    }
                }

                R.id.bs_report -> showReportDialog(meme)
            }

        }.show()
    }

    /**
     * Launch the comments activity
     */
    private fun showComments(memeId: String) {
        val i = Intent(activity, CommentActivity::class.java)
        i.putExtra(Constants.MEME_ID, memeId)
        startActivity(i)
        AppUtils.slideRight(activity!!)
    }

    /**
     * Show dialog for reporting meme
     * @param meme - Meme to report
     */
    private fun showReportDialog(meme: Meme) {
        val editText = EditText(activity)
        val layout = FrameLayout(activity!!)
        layout.setPaddingRelative(45,15,45,0)
        layout.addView(editText)

        activity!!.alert("Please provide a reason for reporting") {
            customView = layout

            positiveButton("Report") {
                if (!AppUtils.validated(editText)) {
                    activity!!.toast("Please enter a reason to report")
                    return@positiveButton
                }

                val report = Report()
                report.memeId = meme.id
                report.memePosterId = meme.memePosterID
                report.reporterId = getUid()
                report.memeUrl = meme.imageUrl
                report.reason = editText.text.toString().trim()
                report.time = System.currentTimeMillis()

                memesViewModel.reportMeme(report)
            }

            negativeButton("Cancel"){}
        }.show()
    }

    /**
     * Show empty state
     */
    private fun showEmptyState() {
        homeShimmer.stopShimmerAnimation()
        hideViews(homeRv, homeShimmer)
        emptyState.showView()
    }

    /**
     * Function to get the current fragments RecyclerView
     */
    fun getRecyclerView(): RecyclerView {
        return homeRv
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.hasNewContent()) {
            EventBus.getDefault().post(PostMemeEvent(PostMemeEvent.TYPE.NEW_POST))
            memesAdapter.currentList?.dataSource?.invalidate()
            sessionManager.hasNewContent(false)
        }
    }
}

