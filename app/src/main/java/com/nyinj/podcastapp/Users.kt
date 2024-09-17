data class Users(
    var name: String? = null,
    var email: String? = null,
    var uid: String? = null,
    val description: String = "",
    var isFollowed: Boolean = false
)