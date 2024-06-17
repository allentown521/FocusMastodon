package allen.town.focus.twitter.adapters

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

import allen.town.focus.twitter.activities.media_viewer.image.ImageFragment

class ImagePagerAdapter(fm: FragmentManager, private val urls: Array<String>) : FragmentPagerAdapter(fm) {

    private val fragments = mutableListOf<ImageFragment>()

    init {
        urls.indices.mapTo(fragments) { ImageFragment.getInstance(it, urls[it]) }
    }

    override fun getItem(i: Int): ImageFragment {
        return fragments[i]
    }

    override fun getCount(): Int {
        return fragments.size
    }
}
