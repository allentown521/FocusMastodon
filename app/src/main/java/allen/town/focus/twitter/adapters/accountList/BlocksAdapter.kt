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

package allen.town.focus.twitter.adapters.accountList

import allen.town.focus.twitter.R
import allen.town.focus.twitter.databinding.ItemBlockedUserBinding
import allen.town.focus.twitter.twittertext.emojify
import allen.town.focus.twitter.utils.AccountActionListener
import allen.town.focus.twitter.utils.BindingHolder
import allen.town.focus.twitter.utils.loadAvatar
import allen.town.focus_common.extensions.visible
import android.view.LayoutInflater
import android.view.ViewGroup

/** Displays a list of blocked accounts. */
class BlocksAdapter(
    accountActionListener: AccountActionListener,
    animateAvatar: Boolean,
    animateEmojis: Boolean,
    showBotOverlay: Boolean
) : AccountAdapter<BindingHolder<ItemBlockedUserBinding>>(
    accountActionListener = accountActionListener,
    animateAvatar = animateAvatar,
    animateEmojis = animateEmojis,
    showBotOverlay = showBotOverlay
) {

    override fun createAccountViewHolder(parent: ViewGroup): BindingHolder<ItemBlockedUserBinding> {
        val binding = ItemBlockedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BindingHolder(binding)
    }

    override fun onBindAccountViewHolder(viewHolder: BindingHolder<ItemBlockedUserBinding>, position: Int) {
        val account = accountList[position]
        val binding = viewHolder.binding
        val context = binding.root.context

        val emojifiedName = account.name.emojify(account.emojis, binding.blockedUserDisplayName, animateEmojis)
        binding.blockedUserDisplayName.text = emojifiedName
        val formattedUsername = "@${account.username}"
        binding.blockedUserUsername.text = formattedUsername

        val avatarRadius = context.resources.getDimensionPixelSize(R.dimen.avatar_radius_48dp)
        loadAvatar(account.avatar, binding.blockedUserAvatar, avatarRadius, animateAvatar)

        binding.blockedUserBotBadge.visible(showBotOverlay && account.bot)

        binding.blockedUserUnblock.setOnClickListener {
            accountActionListener.onBlock(false, account.id, position)
        }
        binding.root.setOnClickListener {
            accountActionListener.onViewAccount(account.id)
        }
    }
}
