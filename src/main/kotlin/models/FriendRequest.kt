package models

data class FriendRequest(val requestId: String,
                         val requesteeId: String,
                         val receiverId: String)