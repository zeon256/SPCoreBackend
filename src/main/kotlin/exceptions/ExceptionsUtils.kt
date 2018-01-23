package exceptions

class DuplicateFound(msg:String): Exception(msg)
class AlreadyFriends(msg:String): Exception(msg)
class UserDoesntExist(msg:String): Exception(msg)
class CannotAddSelfAsFriend(msg:String): Exception(msg)
class PersonAlreadySentRequest(msg:String): Exception(msg)

data class ErrorMsg(val msg:String,
                    val code:Int,
                    val docLinks: String? = null)

const val WRONG_SPICE_CRENDENTIALS = 4156
const val LOCKED_OUT_BY_SP = 1237
const val MISSING_JWT = 8456
const val DUPLICATE_FOUND = 5640
const val ALREADY_FRIENDS = 8974
const val DATABASE_ERROR = 7456
const val BAD_REQUEST = 7651
const val NOT_FRIENDS = 4568
const val CANNOT_ADD_SELF_AS_FRIEND = 8791
const val OTHER_PARTY_ALREADY_SENT_REQ = 7777
const val CAP_REACHED = 4786
const val NOT_EVENT_HOST = 7980
