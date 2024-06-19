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

package allen.town.focus.twitter.activities

import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.main_fragments.other_fragments.AccountListFragment
import allen.town.focus.twitter.databinding.ActivityAccountListBinding
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit

class AccountListActivity : BottomSheetActivity() {


    enum class Type {
        FOLLOWS,
        FOLLOWERS,
        BLOCKS,
        MUTES,
        FOLLOW_REQUESTS,
        REBLOGGED,
        FAVOURITED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAccountListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getSerializableExtra(EXTRA_TYPE) as Type
        val id: String? = intent.getStringExtra(EXTRA_ID)
        val accountLocked: Boolean = intent.getBooleanExtra(EXTRA_ACCOUNT_LOCKED, false)

        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.apply {
            when (type) {
                Type.BLOCKS -> setTitle(R.string.blocked_user)
                Type.MUTES -> setTitle(R.string.muted_users)
                Type.FOLLOW_REQUESTS -> setTitle(R.string.muted)
                Type.FOLLOWERS -> setTitle(R.string.muted)
                Type.FOLLOWS -> setTitle(R.string.muted)
                Type.REBLOGGED -> setTitle(R.string.muted)
                Type.FAVOURITED -> setTitle(R.string.muted)
            }
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        supportFragmentManager.commit {
            replace(R.id.fragment_container, AccountListFragment.newInstance(type, id, accountLocked))
        }
    }


    companion object {
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_ID = "id"
        private const val EXTRA_ACCOUNT_LOCKED = "acc_locked"

        fun newIntent(context: Context, type: Type, id: String? = null, accountLocked: Boolean = false): Intent {
            return Intent(context, AccountListActivity::class.java).apply {
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_ACCOUNT_LOCKED, accountLocked)
            }
        }
    }
}
