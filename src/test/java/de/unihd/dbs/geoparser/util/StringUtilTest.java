package de.unihd.dbs.geoparser.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class StringUtilTest {

	@Test
	public void testIsSingleCapitalLetter() {
		final String str = "A";
		assertTrue(StringUtil.isSingleCapitalLetter(str));
	}

	@Test
	public void testIsSingleCapitalLetterUmlaut() {
		final String str = "Ü";
		assertTrue(StringUtil.isSingleCapitalLetter(str));
	}

	@Test
	public void testIsSingleCapitalLetterNonCapital() {
		final String str = "a";
		assertFalse(StringUtil.isSingleCapitalLetter(str));
	}

	@Test
	public void testIsSingleCapitalLetterWord() {
		final String str = "Ab";
		assertFalse(StringUtil.isSingleCapitalLetter(str));
	}

	@Test
	public void testIsSingleCapitalLetterTwoCaptialLetters() {
		final String str = "AB";
		assertFalse(StringUtil.isSingleCapitalLetter(str));
	}

	@Test
	public void testIsSingleCapitalLetterNotCaptialLetterWord() {
		final String str = "aB";
		assertFalse(StringUtil.isSingleCapitalLetter(str));
	}

	@Test
	public void testIsSingleCapitalLetterNumber() {
		final String str = "9";
		assertFalse(StringUtil.isSingleCapitalLetter(str));
	}

}
