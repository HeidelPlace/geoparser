package de.unihd.dbs.geoparser.process.linking;

import java.util.List;
import java.util.Set;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.GazetteerEntriesAnnotation;
import de.unihd.dbs.geoparser.gazetteer.models.Place;

import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

/**
 * Base class for toponym linking modules.
 * 
 * @author lrichter
 * 
 */
public abstract class ToponymLinker {

	/**
	 * Requirements to be fulfilled before the {@link ToponymLinker} module can be run. I.e., {@link Annotator}s that
	 * produce annotations according to the requirements must have been previously run.
	 * 
	 * @return requirements for the implemented linking module.
	 */
	public abstract Set<Requirement> requires();

	/**
	 * Requirement fulfilled by the {@link ToponymLinker} module. I.e., if the results returned by the module are
	 * attached via {@link ToponymLinkingAnnotator}, annotations are produced according to the fulfilled requirements.
	 * 
	 * @return requirements fulfilled be the implemented linking module.
	 */
	public abstract Set<Requirement> requirementsSatisfied();

	/**
	 * Link toponyms to gazetteer entries with no document knowledge.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to link contained toponyms.
	 * @return a list of {@link GazetteerEntriesAnnotation} entries representing the linked toponyms. Unlinked or
	 *         non-toponym entities are represented by a <code>null</code> entry. Consequently, the input list has the
	 *         same size as the output list.
	 */
	public List<List<Place>> link(final List<CoreMap> namedEntities) {
		return link(namedEntities, null);
	}

	/**
	 * Link toponyms to gazetteer entries.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to link contained toponyms.
	 * @param document the source document.
	 * @return a list of {@link GazetteerEntriesAnnotation} entries representing the linked toponyms. Unlinked or
	 *         non-toponym entities are represented by a <code>null</code> entry. Consequently, the input list has the
	 *         same size as the output list.
	 */
	public List<List<Place>> link(final List<CoreMap> namedEntities, final Annotation document) {
		return link(namedEntities, document, null);
	}

	/**
	 * Link toponyms to gazetteer entries.
	 * 
	 * @param namedEntities the {@link MentionsAnnotation}, from which to link contained toponyms.
	 * @param document the source document.
	 * @param sentence the source sentence.
	 * @return a list of {@link GazetteerEntriesAnnotation} entries representing the linked toponyms. Unlinked or
	 *         non-toponym entities are represented by a <code>null</code> entry. Consequently, the input list has the
	 *         same size as the output list.
	 */
	public abstract List<List<Place>> link(final List<CoreMap> namedEntities, final Annotation document,
			final CoreMap sentence);

}
