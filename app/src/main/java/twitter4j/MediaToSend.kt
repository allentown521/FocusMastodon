package twitter4j

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaToSend(
    val id: String,
    var processed: Boolean
) : Parcelable
