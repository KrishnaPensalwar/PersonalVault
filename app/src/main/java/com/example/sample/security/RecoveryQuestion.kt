package com.example.sample.security

/**
 * Fixed preset list of recovery questions used to reset a forgotten app PIN.
 * No free-text custom questions — keeps the recovery flow simple and avoids
 * users picking an easily-guessable question.
 */
enum class RecoveryQuestion(val id: Int, val text: String) {
    FIRST_PET(0, "What was the name of your first pet?"),
    MOTHERS_MAIDEN_NAME(1, "What is your mother's maiden name?"),
    FIRST_SCHOOL(2, "What was the name of your first school?"),
    FAVORITE_BOOK(3, "What is your favorite book?"),
    BIRTH_CITY(4, "What city were you born in?");

    companion object {
        fun fromId(id: Int): RecoveryQuestion? = entries.find { it.id == id }
    }
}
