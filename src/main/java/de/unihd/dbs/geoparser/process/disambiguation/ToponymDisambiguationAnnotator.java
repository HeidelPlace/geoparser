package de.unihd.dbs.geoparser.process.disambiguation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.GazetteerEntriesAnnotation;
import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.ResolvedLocationAnnotation;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.ResolvedLocation;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;

/**
 * This {@link Annotator} implementation adds disambiguated toponyms to an {@link Annotation} instance using a
 * {@link ToponymDisambiguator} implementation.
 * <p>
 * The assumptions made regarding available annotations is specified by the {@link ToponymDisambiguator} implementation
 * via {@link ToponymDisambiguator#requires}. E.g., it might be assumed that the annotation already contains the
 * tokenized words (in form of {@link CoreLabel}s) under the key with class {@link TokensAnnotation}, which are split
 * into sentences that are stored under the key with class {@link SentencesAnnotation}.
 * <p>
 * For each linked toponym (i.e., a {@link MentionsAnnotation} with a {@link NamedEntityTagAnnotation} value equal to
 * {@link NamedEntityType#LOCATION} and a {@link GazetteerEntriesAnnotation}) a {@link ResolvedLocationAnnotation} is
 * added if the toponym was successfully disambiguated.
 * 
 * @author lrichter
 * 
 */
public class ToponymDisambiguationAnnotator implements Annotator {

	private final ToponymDisambiguator disambiguator;

	/**
	 * Constructor to support Annotation loading via reflection.
	 * 
	 * @param name ?
	 * @param props ?
	 */
	public ToponymDisambiguationAnnotator(final String name, final Properties props) {
		throw new UnsupportedOperationException();
	}

	public ToponymDisambiguationAnnotator(final ToponymDisambiguator disambiguator) {
		this.disambiguator = disambiguator;
	}

	public ToponymDisambiguator getToponymDisambiguationModule() {
		return this.disambiguator;
	}

	@Override
	public Set<Requirement> requires() {
		final Set<Requirement> requirements = new ArraySet<>(ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT);
		requirements.addAll(disambiguator.requires());
		return Collections.unmodifiableSet(requirements);
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		final Set<Requirement> requirements = new HashSet<>();
		requirements.addAll(disambiguator.requirementsSatisfied());
		return Collections.unmodifiableSet(requirements);
	}

	@Override
	public void annotate(final Annotation annotation) {
		if (!annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			throw new RuntimeException("No sentence found in " + annotation);
		}

		for (final CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
			doOneSentence(annotation, sentence);
		}
	}

	private CoreMap doOneSentence(final Annotation annotation, final CoreMap sentence) {
		final List<CoreMap> mentions = sentence.get(CoreAnnotations.MentionsAnnotation.class);

		final List<ResolvedLocation> output = disambiguator.disambiguate(mentions, annotation, sentence);

		for (int i = 0; i < mentions.size(); i++) {
			final ResolvedLocation location = output.get(i);
			if (location == null) {
				continue;
			}
			else {
				mentions.get(i).set(ResolvedLocationAnnotation.class, location);
			}
		}

		return sentence;
	}

}
