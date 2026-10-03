package com.lostsheep.focus.story

data class Verse(val id: String, val reference: String, val text: String, val translation: String)

/** Verses for reflection. Texts other than Matthew 18 are from the public-domain World English Bible. */
object Verses {
    val all = listOf(
        Verse(
            "matthew-18-12",
            "Matthew 18:12–13",
            "If a man has a hundred sheep and one of them wanders away, what will he do? Won’t he leave the ninety-nine others on the hills and go out to search for the one that is lost? And if he finds it, I tell you the truth, he will rejoice over it more than over the ninety-nine that didn’t wander away!",
            "",
        ),
        Verse("psalm-23-1", "Psalm 23:1–3", "Yahweh is my shepherd: I shall lack nothing. He makes me lie down in green pastures. He leads me beside still waters. He restores my soul.", "WEB"),
        Verse("john-10-11", "John 10:11", "I am the good shepherd. The good shepherd lays down his life for the sheep.", "WEB"),
        Verse("colossians-3-23", "Colossians 3:23", "And whatever you do, work heartily, as for the Lord, and not for men.", "WEB"),
        Verse("isaiah-40-31", "Isaiah 40:31", "But those who wait for Yahweh will renew their strength. They will mount up with wings like eagles. They will run, and not be weary. They will walk, and not faint.", "WEB"),
        Verse("proverbs-16-3", "Proverbs 16:3", "Commit your deeds to Yahweh, and your plans shall succeed.", "WEB"),
        Verse("philippians-4-13", "Philippians 4:13", "I can do all things through Christ, who strengthens me.", "WEB"),
    )

    fun byId(id: String): Verse = all.firstOrNull { it.id == id } ?: all.first()
}
