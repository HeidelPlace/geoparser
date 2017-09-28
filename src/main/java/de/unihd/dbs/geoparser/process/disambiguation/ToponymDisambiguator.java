package de.unihd.dbs.geoparser.process.disambiguation;

import java.util.List;
import java.util.Set;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.ResolvedLocationAnnotation;
import de.unihd.dbs.geoparser.core.ResolvedLocation;

import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

/**
 * Base class for toponym disambiguation modules.
 * 
 * @author lrichter
 * 
 */
public abstract class ToponymDisambiguator {

	/**
	 * Requirements to be fulfilled before the {@link ToponymDisambiguator} module can be run. I.e., {@link Annotator}s
	 * that produce annotations according to the requirements must have been previously run.
	 * 
	 * @return requirements for the implemented disambiguation module.
	 */
	public abstract Set<Requirement> requires();

	/**
	 * Requirement fulfilled by the {@link ToponymDisambiguator} module. I.e., if the results returned by the module are
	 * attached via {@link ToponymDisambiguationAnnotator}, annotations are produced according to the fulfilled
	 * requirements.
	 * 
	 * @return requirements fulfilled be the implemented disambiguation module.
	 */
	public abstract Set<Requirement> requirementsSatisfied();

	/**
	 * Disambiguate toponyms without document knowledge.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to disambiguate contained toponyms.
	 * @return a list of {@link ResolvedLocationAnnotation} entries representing the disambiguated toponyms.
	 *         Undisambiguated or non-toponym entities are represented by a <code>null</code> entry. Consequently, the
	 *         input list has the same size as the output list.
	 */
	public List<ResolvedLocation> disambiguate(final List<CoreMap> namedEntities) {
		return disambiguate(namedEntities, null);
	}

	/**
	 * Disambiguate toponyms.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to disambiguate contained toponyms.
	 * @param document the source document.
	 * @return a list of {@link ResolvedLocationAnnotation} entries representing the disambiguated toponyms.
	 *         Undisambiguated or non-toponym entities are represented by a <code>null</code> entry. Consequently, the
	 *         input list has the same size as the output list.
	 */
	public List<ResolvedLocation> disambiguate(final List<CoreMap> namedEntities, final Annotation document) {
		return disambiguate(namedEntities, document, null);
	}

	/**
	 * Disambiguate toponyms.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to disambiguate contained toponyms.
	 * @param document the source document.
	 * @param sentence the source sentence.
	 * @return a list of {@link ResolvedLocationAnnotation} entries representing the disambiguated toponyms.
	 *         Undisambiguated or non-toponym entities are represented by a <code>null</code> entry. Consequently, the
	 *         input list has the same size as the output list.
	 */
	public abstract List<ResolvedLocation> disambiguate(final List<CoreMap> namedEntities, final Annotation document,
			final CoreMap sentence);

}
