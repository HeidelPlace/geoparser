package de.unihd.dbs.geoparser.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;

/**
 * Provider for language specific stop words.
 * 
 * @author lrichter
 *
 */
// XXX:StopwordAnnotator for Stanford NLP:
// https://github.com/jconwell/coreNlp/blob/master/src/main/java/intoxicant/analytics/coreNlp/StopwordAnnotator.java
public class StopWordProvider {

	private static final Logger logger = LoggerFactory.getLogger(StopWordProvider.class);

	public static final String CONFIG_STOPWORD_PATHS_LABEL = "stopwords.paths";
	public static final String STOPWORD_LANGUAGE_MARKER = "language=";
	public static final String COMMENT_MARKER = "#";

	private final Map<String, Set<String>> stopWordsPerLanguage;

	/**
	 * Create an instance of {@link StopWordProvider} with an empty list of stop words.
	 */
	public StopWordProvider() {
		stopWordsPerLanguage = new HashMap<>();
	}

	/**
	 * Create an instance of {@link StopWordProvider} by loading all stop word lists into memory, which are located at
	 * the paths specified in the given GeoParser configuration via the label {@link #CONFIG_STOPWORD_PATHS_LABEL}.
	 * 
	 * @param config the GeoParser configuration.
	 * @throws IOException if reading the stop-word files failed.
	 * @throws UnknownConfigLabelException if the configuration string label is invalid.
	 * @throws URISyntaxException if something went wrong with getting the URI for a file.
	 */
	public StopWordProvider(final GeoparserConfig config)
			throws IOException, UnknownConfigLabelException, URISyntaxException {
		this();
		Objects.requireNonNull(config);
		init(config.getConfigStringByLabel(CONFIG_STOPWORD_PATHS_LABEL));
	}

	private void init(final String stopWordFilePaths) throws IOException, URISyntaxException {
		logger.debug("Initializing StopWordProvider with stopWordFilePaths '" + stopWordFilePaths + "'.");

		final String[] paths = stopWordFilePaths.split(",");
		for (final String path : paths) {
			addStopWordsFromFile(Paths.get(getClass().getClassLoader().getResource(path).toURI()));
		}

		logger.debug(
				"Successfully initialized StopWordProvider with " + stopWordsPerLanguage.size() + " stop word lists.");
	}

	/**
	 * Check if the given word is a stop word in the given language.
	 * 
	 * @param language the language of the word.
	 * @param word the word to check.
	 * @return <code>true</code>, if the word is a stop word, <code>false</code> otherwise.
	 */
	public boolean isStopWord(final String language, final String word) {
		return getStopWordsForLanguage(language).contains(word);
	}

	/**
	 * Check if a stop word list exists for the given language.
	 * 
	 * @param language the language to check.
	 * @return <code>true</code>, if a stop word list exists for the given language, <code>false</code> otherwise.
	 */
	public boolean isLanguageCovered(final String language) {
		return stopWordsPerLanguage.containsKey(language);
	}

	/**
	 * Get all languages for which stop word lists exist.
	 * 
	 * @return the list of languages.
	 */
	public Set<String> getLanguages() {
		return stopWordsPerLanguage.keySet();
	}

	/**
	 * Get the stop word list for the given language.
	 * 
	 * @param language the language for which to get the stop word list.
	 * @return the stop word list for the language.
	 */
	public Set<String> getStopWordsForLanguage(final String language) {
		validateLanguageIsCovered(language);
		return stopWordsPerLanguage.get(language);
	}

	private void validateLanguageIsCovered(final String language) {
		if (!isLanguageCovered(language)) {
			throw new IllegalArgumentException("language `" + language + "` is not covered!");
		}
	}

	/**
	 * Add a stop word list that is loaded from the given path.
	 * <p>
	 * The file may contain comment lines starting with {@link #COMMENT_MARKER}. These lines are ignored. <br>
	 * The first non-comment line must start with the {@link #STOPWORD_LANGUAGE_MARKER}, followed by a non-empty
	 * language string.
	 * 
	 * @param path the path from which to load the stop word list.
	 * @throws IOException if an error occurred while processing the stop word list.
	 */
	public void addStopWordsFromFile(final Path path) throws IOException {
		final List<String> stopWords = Files.lines(path).filter(line -> !line.startsWith(COMMENT_MARKER))
				.collect(Collectors.toList());

		final String firstLine = stopWords.remove(0);
		if (!firstLine.startsWith(STOPWORD_LANGUAGE_MARKER)) {
			throw new IllegalArgumentException("Error when processing stopwords-file '" + path
					+ "'! The stop word file must start with a language marker " + STOPWORD_LANGUAGE_MARKER
					+ "<some_language_code>.");
		}
		else {
			final String language = firstLine.substring(STOPWORD_LANGUAGE_MARKER.length());
			addStopWords(language, Collections.unmodifiableSet(new HashSet<>(stopWords)));
		}
	}

	/**
	 * Add a stop word list for the given language.
	 * <p>
	 * If a stop word list already exists for the language, it is overwritten.
	 * 
	 * @param language the language for which a stop word list should be stored.
	 * @param stopWords the stop word list.
	 */
	public void addStopWords(final String language, final Set<String> stopWords) {
		if (language.isEmpty()) {
			throw new IllegalArgumentException("The language marker must not be an empty string!");
		}

		stopWordsPerLanguage.put(language, Collections.unmodifiableSet(new HashSet<>(stopWords)));
	}

	/**
	 * Remove the stop word list for the given language.
	 * 
	 * @param language the language for which the stop word list should be removed.
	 */
	public void removeStopWords(final String language) {
		validateLanguageIsCovered(language);
		stopWordsPerLanguage.remove(language);
	}

}
