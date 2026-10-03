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

    /** Short verses that fit over the scene while the timer runs: strength, steadiness and peace. */
    val focus = listOf(
        Verse("joshua-1-9", "Joshua 1:9", "Be strong and courageous. Don’t be afraid. Don’t be dismayed, for Yahweh your God is with you wherever you go.", "WEB"),
        Verse("psalm-46-10", "Psalm 46:10", "Be still, and know that I am God.", "WEB"),
        Verse("proverbs-4-25", "Proverbs 4:25", "Let your eyes look straight ahead. Fix your gaze directly before you.", "WEB"),
        Verse("galatians-6-9", "Galatians 6:9", "Let’s not be weary in doing good, for we will reap in due season, if we don’t give up.", "WEB"),
        Verse("hebrews-12-1", "Hebrews 12:1", "Let’s run with perseverance the race that is set before us.", "WEB"),
        Verse("1-corinthians-15-58", "1 Corinthians 15:58", "Be steadfast, immovable, always abounding in the Lord’s work, because you know that your labor is not in vain in the Lord.", "WEB"),
        Verse("psalm-90-17", "Psalm 90:17", "Let the favor of the Lord our God be on us. Establish the work of our hands for us.", "WEB"),
        Verse("isaiah-41-10", "Isaiah 41:10", "Don’t you be afraid, for I am with you. Don’t be dismayed, for I am your God. I will strengthen you.", "WEB"),
        Verse("matthew-11-28", "Matthew 11:28", "Come to me, all you who labor and are heavily burdened, and I will give you rest.", "WEB"),
        Verse("psalm-121-1", "Psalm 121:1–2", "I will lift up my eyes to the hills. Where does my help come from? My help comes from Yahweh, who made heaven and earth.", "WEB"),
        Verse("proverbs-3-5", "Proverbs 3:5–6", "Trust in Yahweh with all your heart, and don’t lean on your own understanding. In all your ways acknowledge him, and he will make your paths straight.", "WEB"),
        Verse("2-timothy-1-7", "2 Timothy 1:7", "For God didn’t give us a spirit of fear, but of power, love, and self-control.", "WEB"),
        Verse("psalm-16-8", "Psalm 16:8", "I have set Yahweh always before me. Because he is at my right hand, I shall not be moved.", "WEB"),
        Verse("psalm-119-105", "Psalm 119:105", "Your word is a lamp to my feet, and a light for my path.", "WEB"),
        Verse("isaiah-26-3", "Isaiah 26:3", "You will keep whoever’s mind is steadfast in perfect peace, because he trusts in you.", "WEB"),
        Verse("colossians-3-2", "Colossians 3:2", "Set your mind on the things that are above, not on the things that are on the earth.", "WEB"),
        Verse("psalm-27-14", "Psalm 27:14", "Wait for Yahweh. Be strong, and let your heart take courage.", "WEB"),
        Verse("psalm-46-1", "Psalm 46:1", "God is our refuge and strength, a very present help in trouble.", "WEB"),
        Verse("nehemiah-8-10", "Nehemiah 8:10", "The joy of Yahweh is your strength.", "WEB"),
        Verse("john-10-27", "John 10:27", "My sheep hear my voice, and I know them, and they follow me.", "WEB"),
        Verse("philippians-4-13-focus", "Philippians 4:13", "I can do all things through Christ, who strengthens me.", "WEB"),
        Verse("colossians-3-23-focus", "Colossians 3:23", "And whatever you do, work heartily, as for the Lord, and not for men.", "WEB"),
        Verse("isaiah-40-31-focus", "Isaiah 40:31", "Those who wait for Yahweh will renew their strength. They will run, and not be weary.", "WEB"),
        Verse("proverbs-16-3-focus", "Proverbs 16:3", "Commit your deeds to Yahweh, and your plans shall succeed.", "WEB"),
    )

    /** Verses that close a session: rest, thanks and being found. */
    val closing = listOf(
        Verse("luke-15-7", "Luke 15:7", "There will be more joy in heaven over one sinner who repents, than over ninety-nine righteous people who need no repentance.", "WEB"),
        Verse("lamentations-3-22", "Lamentations 3:22–23", "His mercies don’t fail. They are new every morning. Great is your faithfulness.", "WEB"),
        Verse("matthew-11-28-close", "Matthew 11:28", "Come to me, all you who labor and are heavily burdened, and I will give you rest.", "WEB"),
        Verse("psalm-23-1-close", "Psalm 23:1–2", "Yahweh is my shepherd: I shall lack nothing. He makes me lie down in green pastures.", "WEB"),
        Verse("philippians-4-7", "Philippians 4:7", "And the peace of God, which surpasses all understanding, will guard your hearts and your thoughts in Christ Jesus.", "WEB"),
        Verse("psalm-90-17-close", "Psalm 90:17", "Establish the work of our hands for us. Yes, establish the work of our hands.", "WEB"),
        Verse("matthew-6-34", "Matthew 6:34", "Therefore don’t be anxious for tomorrow, for tomorrow will be anxious for itself.", "WEB"),
        Verse("1-corinthians-15-58-close", "1 Corinthians 15:58", "Your labor is not in vain in the Lord.", "WEB"),
    )

    fun byId(id: String): Verse = all.firstOrNull { it.id == id } ?: all.first()

    /** A steady pick for [seed] (for example a session id), so the same session always shows the same words. */
    fun <T> pick(list: List<T>, seed: String, salt: Int = 0): T =
        list[Math.floorMod(seed.hashCode() * 31 + salt, list.size)]
}

/** Short closing prayers, written for the app. */
object Prayers {
    val all = listOf(
        "Lord, thank you for this quiet time. Bless the work of my hands, and lead me gently into what comes next. Amen.",
        "Good Shepherd, thank you for finding me when I wander. Keep my heart close to you through the rest of this day. Amen.",
        "Father, I offer you the work I have done. Where it is weak, make it strong; where it is good, let it serve others. Amen.",
        "Jesus, thank you for the strength to keep going. Give me rest when I need it, and courage for the next step. Amen.",
        "Lord, you leave the ninety-nine to look for the one. Thank you for caring for every part of my life, even this small task. Amen.",
        "God of peace, quiet my mind and steady my heart. Let what I have done today bring you glory. Amen.",
        "Lord, thank you for this time of focus. Help me to be faithful in little things, and patient with myself. Amen.",
        "Shepherd of my soul, carry me when I am tired, and lead me beside still waters. Amen.",
        "Father, thank you that your mercies are new every morning. Help me to begin again whenever I need to. Amen.",
        "Lord, I give you my plans and my progress. Guide my next steps, and keep me from worry. Amen.",
    )
}

enum class ClosingWords(val label: String) {
    Alternate("Prayer or verse"),
    Prayer("Prayer"),
    Verse("Verse"),
    Off("Off"),
}

/** What to show when a session ends: a prayer, a verse with its reference, or nothing. */
data class Closing(val text: String, val reference: String?)

fun closingFor(choice: ClosingWords, sessionId: String): Closing? {
    val prayer = Closing(Verses.pick(Prayers.all, sessionId), null)
    val verse = Verses.pick(Verses.closing, sessionId, 7).let { Closing(it.text, "${it.reference} · ${it.translation}") }
    return when (choice) {
        ClosingWords.Off -> null
        ClosingWords.Prayer -> prayer
        ClosingWords.Verse -> verse
        ClosingWords.Alternate -> if (Math.floorMod(sessionId.hashCode(), 2) == 0) prayer else verse
    }
}

/**
 * Which verse to show while the timer runs, and how visible it is (0..1), from focused time alone,
 * so a paused session keeps the same words on screen and a resumed one carries on where it was.
 * A new verse comes every few minutes, rests on screen for a while, then fades out.
 */
object FocusVerses {
    data class Shown(val verse: Verse, val alpha: Float)

    fun intervalMs(plannedMs: Long): Long = (plannedMs / 5).coerceIn(60_000L, 5 * 60_000L)

    fun at(sessionId: String, elapsedMs: Long, plannedMs: Long): Shown {
        val interval = intervalMs(plannedMs)
        val index = (elapsedMs / interval).toInt()
        val inSlot = elapsedMs % interval
        val start = (interval * 0.04f).toLong().coerceAtMost(8_000L)
        val hold = (interval * 0.55f).toLong().coerceAtMost(75_000L)
        val fade = 2_000f
        val alpha = when {
            inSlot < start -> 0f
            inSlot < start + fade -> (inSlot - start) / fade
            inSlot < start + hold -> 1f
            inSlot < start + hold + fade -> 1f - (inSlot - start - hold) / fade
            else -> 0f
        }
        // Walk through a per-session shuffle so a session never repeats a verse until it has shown them all.
        val order = Verses.focus.indices.sortedBy { (it * 2654435761L + sessionId.hashCode()).hashCode() }
        return Shown(Verses.focus[order[index % order.size]], alpha.coerceIn(0f, 1f))
    }
}
