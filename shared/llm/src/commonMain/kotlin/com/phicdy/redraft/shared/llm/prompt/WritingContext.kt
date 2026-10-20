package com.phicdy.redraft.shared.llm.prompt

/** What the user is writing. Chosen in the UI; defaults fit everyday work chat. */
data class WritingContext(
    val purpose: WritingPurpose = WritingPurpose.Chat,
    val politeness: Politeness = Politeness.Neutral,
)

enum class WritingPurpose {
    /** Business email. */
    Email,

    /** Slack, Teams and similar work chat. */
    Chat,

    /** Design docs, reports, PR descriptions and other written documents. */
    Document,
}

enum class Politeness {
    Casual,
    Neutral,
    Formal,
}
