package com.example.eventnest

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileUtilsTest {

    @Test
    fun testGetInitials_MultipleWords() {
        assertEquals("JD", ProfileActivity.getInitials("John Doe"))
        assertEquals("AB", ProfileActivity.getInitials("Alice Bob Charlie"))
        assertEquals("RS", ProfileActivity.getInitials("Rahul Sharma"))
    }

    @Test
    fun testGetInitials_SingleWord() {
        assertEquals("AL", ProfileActivity.getInitials("Alice"))
        assertEquals("BO", ProfileActivity.getInitials("Bob"))
        assertEquals("A", ProfileActivity.getInitials("A"))
    }

    @Test
    fun testGetInitials_WhitespaceAndBlank() {
        assertEquals("U", ProfileActivity.getInitials(""))
        assertEquals("U", ProfileActivity.getInitials("   "))
        assertEquals("JD", ProfileActivity.getInitials("   John    Doe   "))
    }
}
