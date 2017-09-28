package de.unihd.dbs.geoparser.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.util.StopWordProvider;

public class StopWordProviderTest {

	private static StopWordProvider stopWordProvider;

	@BeforeClass
	public static void setUpBeforeClass() throws IOException, UnknownConfigLabelException, URISyntaxException {
		stopWordProvider = new StopWordProvider(new GeoparserConfig());
	}

	@Test
	public void testIsStopWord() {
		final String language = "english";
		final String word = "a";
		assertTrue(stopWordProvider.isStopWord(language, word));
	}

	@Test
	public void testIsNotStopWord() {
		final String language = "english";
		final String word = "Heidelberg";
		assertFalse(stopWordProvider.isStopWord(language, word));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIsStopWordWithUnknownLanguage() {
		final String language = "klingon";
		final String word = "Heidelberg";
		stopWordProvider.isStopWord(language, word);
	}

	@Test
	public void testIsLanguageCovered() {
		final String language = "english";
		assertTrue(stopWordProvider.isLanguageCovered(language));
	}

	@Test
	public void testIsLanguageNotCovered() {
		final String language = "klingon";
		assertFalse(stopWordProvider.isLanguageCovered(language));
	}

	@Test
	public void testGetLanguages() {
		final Set<String> expectedLanguages = new HashSet<>(Arrays.asList("english", "german"));
		final Set<String> actualLanguages = stopWordProvider.getLanguages();
		assertThat(actualLanguages, equalTo(expectedLanguages));
	}

	@Test
	public void testGetStopWordsForLanguage() {
		final String language = "english";
		final Set<String> stopWords = stopWordProvider.getStopWordsForLanguage(language);
		assertThat(stopWords, not(empty()));
	}

	@Test
	public void testRemoveStopWords() throws IOException, UnknownConfigLabelException, URISyntaxException {
		final StopWordProvider stopWordProvider = new StopWordProvider(new GeoparserConfig());
		final String language = "english";
		stopWordProvider.removeStopWords(language);
		assertFalse(stopWordProvider.isLanguageCovered(language));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddStopWordsWithoutLanguage() {
		stopWordProvider.addStopWords("", null);
	}

}
