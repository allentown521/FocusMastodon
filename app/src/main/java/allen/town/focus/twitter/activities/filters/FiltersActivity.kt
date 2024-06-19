package allen.town.focus.twitter.activities.filters

import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.WhiteToolbarActivity
import allen.town.focus.twitter.api.requests.filter.Filter
import allen.town.focus.twitter.databinding.ActivityFiltersBinding
import allen.town.focus.twitter.di.Injectable
import allen.town.focus.twitter.di.ViewModelFactory
import allen.town.focus.twitter.utils.viewBinding
import allen.town.focus_common.extensions.hide
import allen.town.focus_common.extensions.show
import allen.town.focus_common.extensions.visible
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class FiltersActivity : WhiteToolbarActivity(), FiltersListener, Injectable {
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val binding by viewBinding(ActivityFiltersBinding::inflate)
    private val viewModel: FiltersViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.run {
            // Back button
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.addFilterButton.setOnClickListener {
            launchEditFilterActivity()
        }

        binding.swipeRefreshLayout.setOnRefreshListener { loadFilters() }

        setTitle(R.string.pref_title_timeline_filters)
    }

    override fun onResume() {
        super.onResume()
        loadFilters()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.progressBar.visible(state.loadingState == FiltersViewModel.LoadingState.LOADING)
                binding.swipeRefreshLayout.isRefreshing =
                    state.loadingState == FiltersViewModel.LoadingState.LOADING
                binding.addFilterButton.visible(state.loadingState == FiltersViewModel.LoadingState.LOADED)

                when (state.loadingState) {
                    FiltersViewModel.LoadingState.INITIAL, FiltersViewModel.LoadingState.LOADING -> binding.messageView.hide()
                    FiltersViewModel.LoadingState.ERROR_NETWORK -> {
                        binding.messageView.setup(
                            R.drawable.elephant_offline,
                            R.string.error_network
                        ) {
                            loadFilters()
                        }
                        binding.messageView.show()
                    }
                    FiltersViewModel.LoadingState.ERROR_OTHER -> {
                        binding.messageView.setup(
                            R.drawable.elephant_error,
                            R.string.error_generic
                        ) {
                            loadFilters()
                        }
                        binding.messageView.show()
                    }
                    FiltersViewModel.LoadingState.LOADED -> {
                        if (state.filters.isEmpty()) {
                            binding.messageView.setup(
                                R.drawable.elephant_friend_empty,
                                R.string.empty_list,
                                null
                            )
                            binding.messageView.show()
                        } else {
                            binding.messageView.hide()
                            binding.filtersList.adapter =
                                FiltersAdapter(this@FiltersActivity, state.filters)
                        }
                    }
                }
            }
        }
    }

    private fun loadFilters() {
        viewModel.load()
    }

    private fun launchEditFilterActivity(filter: Filter? = null) {
        val intent = Intent(this, EditFilterActivity::class.java).apply {
            if (filter != null) {
                putExtra(EditFilterActivity.FILTER_TO_EDIT, filter)
            }
        }
        startActivity(intent)
    }

    override fun deleteFilter(filter: Filter) {
        viewModel.deleteFilter(filter, binding.root)
    }

    override fun updateFilter(updatedFilter: Filter) {
        launchEditFilterActivity(updatedFilter)
    }
}
