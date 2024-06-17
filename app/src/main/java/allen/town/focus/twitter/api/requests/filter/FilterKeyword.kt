package allen.town.focus.twitter.api.requests.filter

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FilterKeyword(
    val id: String,
    val keyword: String,
    @SerializedName("whole_word") val wholeWord: Boolean
) : Parcelable
