/* Copyright 2018 charlag
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

package allen.town.focus.twitter.di

import allen.town.focus.twitter.activities.AccountListActivity
import allen.town.focus.twitter.activities.BottomSheetActivity
import allen.town.focus.twitter.activities.WhiteToolbarActivity
import allen.town.focus.twitter.activities.filters.EditFilterActivity
import allen.town.focus.twitter.activities.filters.FiltersActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by charlag on 3/24/18.
 */

@Module
abstract class ActivitiesModule {

    @ContributesAndroidInjector
    abstract fun contributesBaseActivity(): BottomSheetActivity


    @ContributesAndroidInjector
    abstract fun contributesAccountListActivity(): AccountListActivity


    @ContributesAndroidInjector
    abstract fun contributesFiltersActivity(): FiltersActivity


    @ContributesAndroidInjector
    abstract fun contributesEditFilterActivity(): EditFilterActivity
}
