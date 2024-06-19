package allen.town.focus.twitter.model

import android.net.Uri
import android.text.TextUtils

class HeaderPaginationList<T> : ArrayList<T>, java.io.Serializable {
    @JvmField
    var nextPageUri: Uri? = null

    @JvmField
    var prevPageUri: Uri? = null

    constructor(initialCapacity: Int) : super(initialCapacity) {}
    constructor() : super() {}
    constructor(c: Collection<T>) : super(c) {}

    fun hasPrevious(): Boolean {
        return !TextUtils.isEmpty(prevPageUri?.getQueryParameter("since_id"))
    }

    fun getPreviousCursor(): String? {
        return prevPageUri?.getQueryParameter("since_id")
    }

    fun hasNext(): Boolean {
        return !TextUtils.isEmpty(nextPageUri?.getQueryParameter("max_id"))
    }

    fun getNextCursor(): String? {
        return nextPageUri?.getQueryParameter("max_id")
    }

    companion object {
        private const val serialVersionUID = -8603601553967559275L

        /**
         *
         * @param headerPaginationList
         */
        @JvmStatic
        fun <T> copy(headerPaginationList: HeaderPaginationList<T>): HeaderPaginationList<T> {
            val headerPaginationList1: HeaderPaginationList<T> = HeaderPaginationList<T>()
            headerPaginationList1.nextPageUri = headerPaginationList.nextPageUri
            headerPaginationList1.prevPageUri = headerPaginationList.prevPageUri
            headerPaginationList1.addAll(headerPaginationList)
            return headerPaginationList1
        }

        @JvmStatic
        fun <T> copyOnlyPage(headerPaginationList: HeaderPaginationList<*>): HeaderPaginationList<T> {
            val headerPaginationList1: HeaderPaginationList<T> = HeaderPaginationList<T>()
            headerPaginationList1.nextPageUri = headerPaginationList.nextPageUri
            headerPaginationList1.prevPageUri = headerPaginationList.prevPageUri
            return headerPaginationList1
        }
    }
}