package de.unihd.dbs.geoparser;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.geoparser.core.Document;
import de.unihd.dbs.geoparser.core.LinkedToponym;
import de.unihd.dbs.geoparser.core.NamedEntity;
import de.unihd.dbs.geoparser.core.ResolvedToponym;
import de.unihd.dbs.geoparser.core.Toponym;
import de.unihd.dbs.geoparser.util.GeoparserUtil;
import edu.stanford.nlp.pipeline.AnnotationPipeline;

/**
 * Implementation of a geoparser that performs geoparsing of unstructured text documents.
 * <p>
 * The geoparser allows to recognize toponyms in unstructured text, link them to one or more gazetteer entries,
 * disambiguate them to unambiguous spatial references, and spatially infer the location of unresolved toponyms.
 *
 * @author lrichter
 *
 */
public class Geoparser {

	private static final Logger logger = LoggerFactory.getLogger(Geoparser.class);

	private final AnnotationPipeline recognitionPipeline;
	private final AnnotationPipeline linkingPipeline;
	private final AnnotationPipeline disambiguationPipeline;
	private final AnnotationPipeline spatialInferencePipeline;

	/**
	 * Create a {@link Geoparser} with the given {@link AnnotationPipeline}s that represent the respective geoparsing
	 * steps.
	 *
	 * @param recognitionPipeline pipeline for recognizing toponyms in a text document
	 * @param linkingPipeline pipeline for linking toponyms to matching gazetteer entries
	 * @param disambiguationPipeline pipeline for disambiguating toponyms to unique spatial references
	 * @param spatialInferencePipeline pipeline for inferring spatial location of unresolved toponyms
	 */
	public Geoparser(final AnnotationPipeline recognitionPipeline, final AnnotationPipeline linkingPipeline,
			final AnnotationPipeline disambiguationPipeline, final AnnotationPipeline spatialInferencePipeline) {
		this.recognitionPipeline = recognitionPipeline;
		this.linkingPipeline = linkingPipeline;
		this.disambiguationPipeline = disambiguationPipeline;
		this.spatialInferencePipeline = spatialInferencePipeline;
	}

	/**
	 * Create a {@link Document} instance from the given text document.
	 *
	 * @param inputText the unstructured text document
	 * @return an annotatable document containing the given text
	 */
	// Refactoring lrichter 16.03.2017: method should go to some util class, since not really the job of a geoparser
	public static Document inputTextToDocument(final String inputText) {
		return new Document(inputText);
	}

	/**
	 * Get all named entities from a text document.
	 *
	 * @param document the document to be parsed; annotations may be added during processing
	 * @return list of found named entities
	 */
	public List<NamedEntity> recognizeNamedEntities(final Document document) {
		recognitionPipeline.annotate(document);
		final List<NamedEntity> namedEntities = GeoparserUtil.getNamedEntities(document);
		logger.trace("recognized: {}", namedEntities);
		return namedEntities;
	}

	/**
	 * Get all toponyms from a text document.
	 *
	 * @param document the document to be parsed; annotations may be added during processing
	 * @return list of found toponyms
	 */
	public List<Toponym> recognizeToponyms(final Document document) {
		final List<Toponym> toponyms = GeoparserUtil.getToponyms(recognizeNamedEntities(document));
		logger.trace("recognized: {}", toponyms);
		return toponyms;
	}

	/**
	 * Link all toponyms in the given document to matching gazetteer entries.
	 *
	 * @param document the document to be parsed; annotations may be added during processing
	 * @return list of linked toponyms
	 */
	public List<LinkedToponym> linkToponyms(final Document document) {
		linkingPipeline.annotate(document);
		final List<LinkedToponym> linkedToponyms = GeoparserUtil.getLinkedToponyms(document);
		logger.trace("linked: {}", linkedToponyms);
		return linkedToponyms;
	}

	/**
	 * Disambiguate toponyms to a unique spatial reference.
	 *
	 * @param document the document to be parsed; annotations may be added during processing
	 * @return list of resolved toponyms
	 */
	public List<ResolvedToponym> disambiguateToponyms(final Document document) {
		disambiguationPipeline.annotate(document);
		final List<ResolvedToponym> resolvedToponyms = GeoparserUtil.getResolvedToponyms(document);
		logger.trace("resolved: {}", resolvedToponyms);
		return resolvedToponyms;
	}

	/**
	 * Spatially infer unlinked toponyms to a unique spatial reference.
	 *
	 * @param document the document to be parsed; annotations may be added during processing
	 * @return list of resolved toponyms
	 */
	public List<ResolvedToponym> spatiallyInferToponyms(final Document document) {
		spatialInferencePipeline.annotate(document);
		final List<ResolvedToponym> resolvedToponyms = GeoparserUtil.getResolvedToponyms(document);
		logger.trace("spatially inferred: {}", resolvedToponyms);
		return resolvedToponyms;
	}

	/**
	 *
	 * @param document the document to be parsed.
	 */
	public void geoparse(final Document document) {
		recognitionPipeline.annotate(document);
		linkingPipeline.annotate(document);
		disambiguationPipeline.annotate(document);
		spatialInferencePipeline.annotate(document);
	}

}