package de.unihd.dbs.geoparser.process.recognition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.core.GeoparsingAnnotations;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.PartOfSpeechPTBType;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.GazetteerQuery;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter.MatchMode;
import de.unihd.dbs.geoparser.gazetteer.query.QueryFilter;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.MultiPatternMatcher;
import edu.stanford.nlp.ling.tokensregex.SequenceMatchResult;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;

/**
 * Implementation of {@link ToponymRecognizer} that extracts named entities from unstructured text documents using
 * gazetteer lookups and some simple linguistic rules. Stop-word filtering is supported rudimentarily.
 *
 * @author lrichter
 *
 */
// Stanford CoreNLP annotator implementing jMWE for detecting Multi-Word Expressions / collocations:
// https://github.com/toliwa/CoreNLP-jMWE
public class GazetteerLookupRecognizer extends ToponymRecognizer {

	private static final Logger logger = LoggerFactory.getLogger(GazetteerLookupRecognizer.class);

	private final Gazetteer gazetteer;
	private Set<String> stopWords;
	private boolean filterStopWords;

	public GazetteerLookupRecognizer(final Gazetteer gazetteer) {
		this.gazetteer = gazetteer;
		this.stopWords = new HashSet<>();
		this.filterStopWords = false;
	}

	public void setStopWords(final Set<String> stopWords) {
		Objects.requireNonNull(stopWords);
		this.stopWords = stopWords;
	}

	public void setFilterStopWords(final boolean filterStopWords) {
		this.filterStopWords = filterStopWords;
	}

	@Override
	public Set<Requirement> requires() {
		return Annotator.TOKENIZE_AND_SSPLIT;
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.unmodifiableSet(new ArraySet<>(ToponymRecognitionAnnotator.TOPONYM_RECOGNITION_REQUIREMENT,
				Annotator.NER_REQUIREMENT, ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT));
	}

	@Override
	/**
	 * <b>Note:</b>: The tokens are annotated in-place!
	 */
	public List<CoreMap> recognize(final List<CoreLabel> tokens, final Annotation document, final CoreMap sentence) {
		final List<CoreMap> toponyms = new ArrayList<>();
		List<List<CoreLabel>> candidates = buildCandidates(tokens);

		if (filterStopWords) {
			candidates = candidates.stream().filter(candidate -> !stopWords.contains(tokensToString(candidate)))
					.collect(Collectors.toList());
			logger.debug("stop-word filtered candidates: " + candidates);
		}

		for (final List<CoreLabel> candidate : candidates) {
			final String candidateText = tokensToString(candidate);

			final List<Place> foundPlaces = runExactNameMatchQuery(candidateText);

			if (foundPlaces.size() > 0) {
				candidate.forEach(token -> token.set(CoreAnnotations.NamedEntityTagAnnotation.class,
						NamedEntityType.LOCATION.name));
				toponyms.add(buildMention(candidateText, candidate, NamedEntityType.LOCATION, foundPlaces));
			}
		}

		return toponyms;
	}

	@SuppressWarnings("unused")
	private static final EnumSet<PartOfSpeechPTBType> properNounTags = EnumSet
			.of(PartOfSpeechPTBType.PROPER_NOUN_SINGULAR, PartOfSpeechPTBType.PROPER_NOUN_PLURAL);

	@SuppressWarnings("unchecked")
	private static List<List<CoreLabel>> buildCandidates(final List<CoreLabel> tokens) {
		final List<List<CoreLabel>> candidates = new ArrayList<>();

		// final List<CoreLabel> properNouns = tokens.stream()
		// .filter(token -> properNounTags.contains(
		// PartOfSpeechPTBType.getByName(token.get(CoreAnnotations.PartOfSpeechAnnotation.class))))
		// .collect(Collectors.toList());
		// logger.debug("proper nouns: " + properNouns);
		// properNouns.forEach(properNoun -> candidates.add(Arrays.asList(properNoun)));

		// documentation for TokensRegex: http://nlp.stanford.edu/software/tokensregex.shtml
		final List<TokenSequencePattern> tokenSequencePatterns = Arrays.asList(
				TokenSequencePattern.compile("[tag:/NNP|NNPS/]+"), // one or more proper nouns
				// TokenSequencePattern.compile("[tag:/NNP|NNPS/]{1,2}[!{tag:/NNP|NNPS/}]"), // one or two proper nouns
				TokenSequencePattern.compile("[tag:/NNP|NNPS/]+[/of/][tag:/NNP|NNPS/]") // consecutive proper nouns
		);
		final MultiPatternMatcher<CoreMap> multiMatcher = TokenSequencePattern
				.getMultiPatternMatcher(tokenSequencePatterns);
		final List<SequenceMatchResult<CoreMap>> matchedSequences = multiMatcher.findNonOverlapping(tokens);
		matchedSequences.forEach(match -> candidates.add((List<CoreLabel>) match.groupNodes()));

		return candidates;
	}

	private static String tokensToString(final List<CoreLabel> tokens) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			sb.append(tokens.get(i).word());
			if (i < tokens.size() - 1) {
				sb.append(tokens.get(i).after());
			}
		}

		return sb.toString();
	}

	private List<Place> runExactNameMatchQuery(final String name) {
		return runQuery(Arrays.asList(new PlaceNamePlaceFilter(name, null, EnumSet.noneOf(NameFlag.class), true,
				MatchMode.EXACT, null, false)));
	}

	private List<Place> runQuery(final List<QueryFilter<Place>> filter) {
		final GazetteerQuery<Place> query = new GazetteerQuery<>(filter);
		return gazetteer.getPlaces(query);
	}

	private static CoreMap buildMention(final String mentionText, final List<CoreLabel> tokens, final NamedEntityType type,
			final List<Place> linkedGazetteerEntries) {
		final CoreMap mention = new ArrayCoreMap();

		mention.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, tokens.get(0).beginPosition());
		mention.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, tokens.get(tokens.size() - 1).endPosition());
		mention.set(CoreAnnotations.TokensAnnotation.class, tokens);
		mention.set(CoreAnnotations.TokenBeginAnnotation.class, tokens.get(0).index());
		mention.set(CoreAnnotations.TokenEndAnnotation.class, tokens.get(tokens.size() - 1).index());
		mention.set(CoreAnnotations.TextAnnotation.class, mentionText);
		mention.set(CoreAnnotations.NamedEntityTagAnnotation.class, type.name);
		mention.set(GeoparsingAnnotations.GazetteerEntriesAnnotation.class, linkedGazetteerEntries);

		return mention;
	}

}
