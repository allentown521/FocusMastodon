package allen.town.focus.twitter.api.requests.filter

import allen.town.focus.twitter.model.Account
import allen.town.focus.twitter.model.Poll
import twitter4j.Status


data class FavoriteEvent(val statusId: String, val favourite: Boolean) : Event
data class ReblogEvent(val statusId: String, val reblog: Boolean) : Event
data class BookmarkEvent(val statusId: String, val bookmark: Boolean) : Event
data class MuteConversationEvent(val statusId: String, val mute: Boolean) : Event
data class UnfollowEvent(val accountId: String) : Event
data class BlockEvent(val accountId: String) : Event
data class MuteEvent(val accountId: String) : Event
data class StatusDeletedEvent(val statusId: String) : Event
data class StatusComposedEvent(val status: Status) : Event
data class StatusScheduledEvent(val status: Status) : Event
data class StatusEditedEvent(val originalId: String, val status: Status) : Event
data class ProfileEditedEvent(val newProfileData: Account) : Event
data class PreferenceChangedEvent(val preferenceKey: String) : Event
data class PollVoteEvent(val statusId: String, val poll: Poll) : Event
data class DomainMuteEvent(val instance: String) : Event
data class AnnouncementReadEvent(val announcementId: String) : Event
data class PinEvent(val statusId: String, val pinned: Boolean) : Event
