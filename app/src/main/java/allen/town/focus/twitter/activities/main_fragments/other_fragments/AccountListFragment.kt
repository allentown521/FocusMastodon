/* Copyright 2017 Andrew Dawson
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package allen.town.focus.twitter.activities.main_fragments.other_fragments

import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.AccountListActivity.Type
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager
import allen.town.focus.twitter.adapters.accountList.AccountAdapter
import allen.town.focus.twitter.adapters.accountList.BlocksAdapter
import allen.town.focus.twitter.adapters.accountList.MutesAdapter
import allen.town.focus.twitter.api.MastodonApi
import allen.town.focus.twitter.api.requests.accounts.TimelineAccount
import allen.town.focus.twitter.databinding.FragmentAccountListBinding
import allen.town.focus.twitter.di.Injectable
import allen.town.focus.twitter.model.Relationship
import allen.town.focus.twitter.utils.AccountActionListener
import allen.town.focus.twitter.utils.HttpHeaderLink
import allen.town.focus.twitter.utils.LinkListener
import allen.town.focus.twitter.utils.viewBinding
import allen.town.focus.twitter.views.EndlessOnScrollListener
import allen.town.focus_common.extensions.hide
import allen.town.focus_common.extensions.show
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import at.connyduck.calladapter.networkresult.fold
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class AccountListFragment :
    Fragment(R.layout.fragment_account_list),
    AccountActionListener,
    LinkListener,
    Injectable {

    @Inject
    lateinit var api: MastodonApi


    private val binding by viewBinding(FragmentAccountListBinding::bind)

    private lateinit var type: Type
    private var id: String? = null

    private lateinit var scrollListener: EndlessOnScrollListener
    private lateinit var adapter: AccountAdapter<*>
    private var fetching = false
    private var bottomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = requireArguments().getSerializable(ARG_TYPE) as Type
        id = requireArguments().getString(ARG_ID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(view.context)
        binding.recyclerView.layoutManager = layoutManager
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                view.context,
                DividerItemDecoration.VERTICAL
            )
        )

        binding.swipeRefreshLayout.setOnRefreshListener { fetchAccounts() }

        val pm = PreferenceManager.getDefaultSharedPreferences(view.context)
        val animateAvatar = false
        val animateEmojis = false
        val showBotOverlay = true

        adapter = when (type) {
            Type.BLOCKS -> BlocksAdapter(this, animateAvatar, animateEmojis, showBotOverlay)
            Type.MUTES -> MutesAdapter(this, animateAvatar, animateEmojis, showBotOverlay)
            Type.FOLLOW_REQUESTS -> {
                BlocksAdapter(this, animateAvatar, animateEmojis, showBotOverlay)
            }
            else -> BlocksAdapter(this, animateAvatar, animateEmojis, showBotOverlay)
        }
        if (binding.recyclerView.adapter == null) {
            binding.recyclerView.adapter = adapter
        }

        scrollListener = object : EndlessOnScrollListener(layoutManager) {
            override fun onLoadMore(totalItemsCount: Int, view: RecyclerView) {
                if (bottomId == null) {
                    return
                }
                fetchAccounts(bottomId)
            }
        }

        binding.recyclerView.addOnScrollListener(scrollListener)

        fetchAccounts()
    }

    override fun onViewTag(tag: String) {
    }

    override fun onViewAccount(id: String) {
        ProfilePager.start(context,id)
    }

    override fun onViewUrl(url: String) {
    }

    override fun onMute(mute: Boolean, id: String, position: Int, notifications: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!mute) {
                    api.unmuteAccount(id)
                } else {
                    api.muteAccount(id, notifications)
                }
                onMuteSuccess(mute, id, position, notifications)
            } catch (_: Throwable) {
                onMuteFailure(mute, id, notifications)
            }
        }
    }

    private fun onMuteSuccess(muted: Boolean, id: String, position: Int, notifications: Boolean) {
        val mutesAdapter = adapter as MutesAdapter
        if (muted) {
            mutesAdapter.updateMutingNotifications(id, notifications, position)
            return
        }
        val unmutedUser = mutesAdapter.removeItem(position)

        if (unmutedUser != null) {
            Snackbar.make(binding.recyclerView, R.string.confirmation_unmuted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    mutesAdapter.addItem(unmutedUser, position)
                    onMute(true, id, position, notifications)
                }
                .show()
        }
    }

    private fun onMuteFailure(mute: Boolean, accountId: String, notifications: Boolean) {
        val verb = if (mute) {
            if (notifications) {
                "mute (notifications = true)"
            } else {
                "mute (notifications = false)"
            }
        } else {
            "unmute"
        }
        Log.e(TAG, "Failed to $verb account id $accountId")
    }

    override fun onBlock(block: Boolean, id: String, position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!block) {
                    api.unblockAccount(id)
                } else {
                    api.blockAccount(id)
                }
                onBlockSuccess(block, id, position)
            } catch (_: Throwable) {
                onBlockFailure(block, id)
            }
        }
    }

    private fun onBlockSuccess(blocked: Boolean, id: String, position: Int) {
        if (blocked) {
            return
        }
        val blocksAdapter = adapter as BlocksAdapter
        val unblockedUser = blocksAdapter.removeItem(position)

        if (unblockedUser != null) {
            Snackbar.make(
                binding.recyclerView,
                R.string.confirmation_unblocked,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.undo) {
                    blocksAdapter.addItem(unblockedUser, position)
                    onBlock(true, id, position)
                }
                .show()
        }
    }

    private fun onBlockFailure(block: Boolean, accountId: String) {
        val verb = if (block) {
            "block"
        } else {
            "unblock"
        }
        Log.e(TAG, "Failed to $verb account accountId $accountId")
    }

    override fun onRespondToFollowRequest(
        accept: Boolean,
        accountId: String,
        position: Int
    ) {
    }

    private fun onRespondToFollowRequestSuccess(position: Int) {
    }

    private suspend fun getFetchCallByListType(fromId: String?): Response<List<TimelineAccount>> {
        return when (type) {
            Type.FOLLOWS -> {
                api.blocks(fromId)
            }
            Type.FOLLOWERS -> {
                api.blocks(fromId)
            }
            Type.BLOCKS -> api.blocks(fromId)
            Type.MUTES -> api.mutes(fromId)
            Type.FOLLOW_REQUESTS -> api.blocks(fromId)
            Type.REBLOGGED -> {
                api.blocks(fromId)
            }
            Type.FAVOURITED -> {
                api.blocks(fromId)
            }
        }
    }

    private fun requireId(type: Type, id: String?): String {
        return requireNotNull(id) { "id must not be null for type " + type.name }
    }

    private fun fetchAccounts(fromId: String? = null) {
        if (fetching) {
            return
        }
        fetching = true
        binding.swipeRefreshLayout.isRefreshing = true

        if (fromId != null) {
            binding.recyclerView.post { adapter.setBottomLoading(true) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = getFetchCallByListType(fromId)

                if (!response.isSuccessful) {
                    onFetchAccountsFailure(Exception(response.message()))
                    return@launch
                }

                val accountList = response.body()

                if (accountList == null) {
                    onFetchAccountsFailure(Exception(response.message()))
                    return@launch
                }

                val linkHeader = response.headers()["Link"]
                onFetchAccountsSuccess(accountList, linkHeader)
            } catch (exception: IOException) {
                onFetchAccountsFailure(exception)
            }
        }
    }

    private fun onFetchAccountsSuccess(accounts: List<TimelineAccount>, linkHeader: String?) {
        adapter.setBottomLoading(false)
        binding.swipeRefreshLayout.isRefreshing = false

        val links = HttpHeaderLink.parse(linkHeader)
        val next = HttpHeaderLink.findByRelationType(links, "next")
        val fromId = next?.uri?.getQueryParameter("max_id")

        if (adapter.itemCount > 0) {
            adapter.addItems(accounts)
        } else {
            adapter.update(accounts)
        }

        if (adapter is MutesAdapter) {
            fetchRelationships(accounts.map { it.id })
        }

        bottomId = fromId

        fetching = false

        if (adapter.itemCount == 0) {
            binding.messageView.show()
            binding.messageView.setup(
                R.drawable.elephant_friend_empty,
                R.string.empty_list,
                null
            )
        } else {
            binding.messageView.hide()
        }
    }

    private fun fetchRelationships(ids: List<String>) {
        lifecycleScope.launch {
            api.relationships(ids)
                .fold(::onFetchRelationshipsSuccess) { throwable ->
                    Log.e(TAG, "Fetch failure for relationships of accounts: $ids", throwable)
                }
        }
    }

    private fun onFetchRelationshipsSuccess(relationships: List<Relationship>) {
        val mutesAdapter = adapter as MutesAdapter
        val mutingNotificationsMap = HashMap<String, Boolean>()
        relationships.map { mutingNotificationsMap.put(it.id, it.mutingNotifications) }
        mutesAdapter.updateMutingNotificationsMap(mutingNotificationsMap)
    }

    private fun onFetchAccountsFailure(throwable: Throwable) {
        fetching = false
        binding.swipeRefreshLayout.isRefreshing = false
        Log.e(TAG, "Fetch failure", throwable)

        if (adapter.itemCount == 0) {
            binding.messageView.show()
            if (throwable is IOException) {
                binding.messageView.setup(R.drawable.elephant_offline, R.string.error_network) {
                    binding.messageView.hide()
                    this.fetchAccounts(null)
                }
            } else {
                binding.messageView.setup(R.drawable.elephant_error, R.string.error_generic) {
                    binding.messageView.hide()
                    this.fetchAccounts(null)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AccountList" // logging tag
        private const val ARG_TYPE = "type"
        private const val ARG_ID = "id"
        private const val ARG_ACCOUNT_LOCKED = "acc_locked"

        fun newInstance(
            type: Type,
            id: String? = null,
            accountLocked: Boolean = false
        ): AccountListFragment {
            return AccountListFragment().apply {
                arguments = Bundle(3).apply {
                    putSerializable(ARG_TYPE, type)
                    putString(ARG_ID, id)
                    putBoolean(ARG_ACCOUNT_LOCKED, accountLocked)
                }
            }
        }
    }
}
