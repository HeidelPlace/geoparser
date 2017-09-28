package de.unihd.dbs.geoparser.process.linking;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.GazetteerEntriesAnnotation;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.process.recognition.ToponymRecognitionAnnotator;

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
 * This {@link Annotator} implementation adds linked toponyms to an {@link Annotation} instance using a
 * {@link ToponymLinker} implementation.
 * <p>
 * The assumptions made regarding available annotations is specified by the {@link ToponymLinker} implementation via
 * {@link ToponymLinker#requires}. E.g., it might be assumed that the annotation already contains the tokenized words
 * (in form of {@link CoreLabel}s) under the key with class {@link TokensAnnotation}, which are split into sentences
 * that are stored under the key with class {@link SentencesAnnotation}.
 * <p>
 * For each toponym (i.e., a {@link MentionsAnnotation} with a {@link NamedEntityTagAnnotation} value equal to
 * {@link NamedEntityType#LOCATION}), a {@link GazetteerEntriesAnnotation} is added if the toponym could be linked to
 * gazetteer entries.
 * 
 * @author lrichter
 * 
 */
public class ToponymLinkingAnnotator implements Annotator {

	private final ToponymLinker linker;

	public static final String TOPONYM_LINKING = "toponym_linking";
	public static final Requirement TOPONYM_LINKING_REQUIREMENT = new Requirement(TOPONYM_LINKING);

	/**
	 * Constructor to support Annotation loading via reflection.
	 * 
	 * @param name ?
	 * @param props ?
	 */
	public ToponymLinkingAnnotator(final String name, final Properties props) {
		throw new UnsupportedOperationException();
	}

	public ToponymLinkingAnnotator(final ToponymLinker linker) {
		this.linker = linker;
	}

	public ToponymLinker getToponymLinkingModule() {
		return this.linker;
	}

	@Override
	public Set<Requirement> requires() {
		final Set<Requirement> requirements = new ArraySet<>(
				ToponymRecognitionAnnotator.TOPONYM_RECOGNITION_REQUIREMENT);
		requirements.addAll(linker.requires());
		return Collections.unmodifiableSet(requirements);
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		final Set<Requirement> requirements = new ArraySet<>(TOPONYM_LINKING_REQUIREMENT);
		requirements.addAll(linker.requirementsSatisfied());
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

		final List<List<Place>> output = linker.link(mentions, annotation, sentence);

		for (int i = 0; i < mentions.size(); i++) {
			final List<Place> link = output.get(i);
			if (link == null) {
				continue;
			}
			else {
				mentions.get(i).set(GazetteerEntriesAnnotation.class, link);
			}
		}

		return sentence;
	}

}
