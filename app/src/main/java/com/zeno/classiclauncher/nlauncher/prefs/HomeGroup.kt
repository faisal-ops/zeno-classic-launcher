package com.zeno.classiclauncher.nlauncher.prefs

import java.util.UUID

/** Horizontal slot in the home shortcut strip (shortcuts sit in the centre). */
enum class HomeGroupSide {
    LEFT,
    RIGHT,
    ;

    companion object {
        fun fromStored(raw: String?): HomeGroupSide =
            if (raw.equals("RIGHT", ignoreCase = true)) RIGHT else LEFT
    }
}

/** Named collection of apps shown on the home screen (not drawer folders). */
data class HomeGroup(
    val id: String,
    val title: String,
    val packageNames: List<String> = emptyList(),
    val side: HomeGroupSide = HomeGroupSide.LEFT,
)

object HomeGroupIds {
    const val PREFIX = "com.zeno.classiclauncher.slot.homegroup."

    fun newId(): String = PREFIX + UUID.randomUUID().toString().replace("-", "")

    fun isHomeGroupId(id: String): Boolean = id.startsWith(PREFIX)
}

/** At most two groups; if both share a side, assign LEFT and RIGHT. */
fun List<HomeGroup>.normalizedAtMostTwo(): List<HomeGroup> {
    val two = take(2)
    if (two.size <= 1) return two
    val a = two[0]
    val b = two[1]
    return if (a.side != b.side) {
        two
    } else {
        listOf(a.copy(side = HomeGroupSide.LEFT), b.copy(side = HomeGroupSide.RIGHT))
    }
}
