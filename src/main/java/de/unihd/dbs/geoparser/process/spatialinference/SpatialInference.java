package de.unihd.dbs.geoparser.process.spatialinference;

import java.util.List;
import java.util.Set;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.GazetteerEntriesAnnotation;
import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.ResolvedLocationAnnotation;
import de.unihd.dbs.geoparser.core.ResolvedLocation;

import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

/**
 * Base class for spatial inference modules.
 * 
 * @author lrichter
 * 
 */
public abstract class SpatialInference {

	/**
	 * Requirements to be fulfilled before the {@link SpatialInference} module can be run. I.e., {@link Annotator}s that
	 * produce annotations according to the requirements must have been previously run.
	 * 
	 * @return requirements for the implemented spatial inference module.
	 */
	public abstract Set<Requirement> requires();

	/**
	 * Requirement fulfilled by the {@link SpatialInference} module. I.e., if the results returned by the module are
	 * attached via {@link SpatialInferenceAnnotator}, annotations are produced according to the fulfilled requirements.
	 * 
	 * @return requirements fulfilled be the implemented inference module.
	 */
	public abstract Set<Requirement> requirementsSatisfied();

	/**
	 * Spatially infer toponym locations without document knowledge.
	 * <p>
	 * Only those toponym locations are inferred, for which no {@link GazetteerEntriesAnnotation} exists.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to spatially infer contained toponyms.
	 * @return a list of {@link ResolvedLocationAnnotation} entries representing the spatially infer toponyms. Already
	 *         resolved or non-toponym entities are represented by a <code>null</code> entry. Consequently, the input
	 *         list has the same size as the output list.
	 */
	public List<ResolvedLocation> inferSpatially(final List<CoreMap> namedEntities) {
		return inferSpatially(namedEntities, null);
	}

	/**
	 * Spatially infer toponym locations.
	 * <p>
	 * Only those toponym locations are inferred, for which no {@link GazetteerEntriesAnnotation} exists.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to spatially infer contained toponyms.
	 * @param document the source document.
	 * @return a list of {@link ResolvedLocationAnnotation} entries representing the spatially infer toponyms. Already
	 *         resolved or non-toponym entities are represented by a <code>null</code> entry. Consequently, the input
	 *         list has the same size as the output list.
	 */
	public List<ResolvedLocation> inferSpatially(final List<CoreMap> namedEntities, final Annotation document) {
		return inferSpatially(namedEntities, document, null);
	}

	/**
	 * Spatially infer toponym locations.
	 * <p>
	 * The implementing class must clarify whether the named entities are changed in-place or copies are created.
	 * <p>
	 * Only those toponym locations are inferred, for which no {@link GazetteerEntriesAnnotation} exists.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to spatially infer contained toponyms.
	 * @param document the source document.
	 * @param sentence the source sentence.
	 * @return a list of {@link ResolvedLocationAnnotation} entries representing the spatially infer toponyms. Already
	 *         resolved or non-toponym entities are represented by a <code>null</code> entry. Consequently, the input
	 *         list has the same size as the output list..
	 */
	public abstract List<ResolvedLocation> inferSpatially(final List<CoreMap> namedEntities, final Annotation document,
			final CoreMap sentence);

}
