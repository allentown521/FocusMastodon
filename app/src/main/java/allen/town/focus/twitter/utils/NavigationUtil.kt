/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package allen.town.focus.twitter.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.PurchaseActivity

object NavigationUtil {

    @JvmStatic
    fun goToProVersion(context: Context, isAlipayRemoveAd: Boolean = false) {
        val intent = Intent(context, PurchaseActivity::class.java)
        context.startActivity(
            intent
        )
        (context as? Activity)?.overridePendingTransition(
            R.anim.retro_fragment_open_enter,
            R.anim.anim_activity_stay
        )
    }
}