package models

data class Event(val type: String,
                 val id: Int,
                 val moduleCode: String? = null,
                 val moduleName: String? = null,
                 val title: String? = null,
                 val seatNo: Int? = null,
                 val location: String,
                 val startTime: Long,
                 val endTime: Long,
                 val deletedInvite: ArrayList<User>? = null,
                 val going: ArrayList<User>? = null,
                 val notGoing: ArrayList<User>? = null)