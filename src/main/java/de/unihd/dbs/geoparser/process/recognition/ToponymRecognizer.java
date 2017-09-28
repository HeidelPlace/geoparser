package de.unihd.dbs.geoparser.process.recognition;

import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

/**
 * Base class for toponym recognition modules.
 * 
 * @author lrichter
 * 
 */
public abstract class ToponymRecognizer {

	/**
	 * Requirements to be fulfilled before the {@link ToponymRecognizer} module can be run. I.e., {@link Annotator}s
	 * that produce annotations according to the requirements must have been previously run.
	 * 
	 * @return requirements for the implemented recognition module.
	 */
	public abstract Set<Requirement> requires();

	/**
	 * Requirement fulfilled by the {@link ToponymRecognizer} module. I.e., if the results returned by the module are
	 * attached via {@link ToponymRecognitionAnnotator}, annotations are produced according to the fulfilled
	 * requirements.
	 * 
	 * @return requirements fulfilled be the implemented recognition module.
	 */
	public abstract Set<Requirement> requirementsSatisfied();

	/**
	 * Recognize toponyms (and other named entities) in the given document.
	 * 
	 * @param document the source document.
	 * @return a list of {@link MentionsAnnotation} compatible entries representing found toponyms or other named
	 *         entities.
	 */
	public List<CoreMap> recognize(final Annotation document) {
		final List<CoreLabel> documentTokens = document.get(CoreAnnotations.TokensAnnotation.class);
		if (documentTokens == null) {
			throw new IllegalArgumentException("No tokens could be found!");
		}
		return recognize(documentTokens);
	}

	/**
	 * Recognize toponyms (and other named entities) in the given list of tokens.
	 * 
	 * @param tokens the tokens to scan
	 * @return a list of {@link MentionsAnnotation} compatible entries representing found toponyms or other named
	 *         entities.
	 */
	public List<CoreMap> recognize(final List<CoreLabel> tokens) {
		return recognize(tokens, null, null);
	}

	/**
	 * Recognize toponyms (and other named entities) for the given sentence.
	 * <p>
	 * The implementing class must clarify whether the tokens are changed in-place or copies are created.
	 * 
	 * @param tokens the sentence tokens.
	 * @param document the source document.
	 * @param sentence the source sentence.
	 * @return a list of {@link MentionsAnnotation} compatible entries representing found toponyms or other named
	 *         entities.
	 */
	public abstract List<CoreMap> recognize(final List<CoreLabel> tokens, final Annotation document,
			final CoreMap sentence);

}
