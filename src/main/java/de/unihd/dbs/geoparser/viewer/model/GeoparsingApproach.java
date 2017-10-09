package de.unihd.dbs.geoparser.viewer.model;

import de.unihd.dbs.geoparser.Geoparser;

public class GeoparsingApproach {

	public enum RecognitionModule {
		STANFORD_NER, OPEN_NLP, GAZETTEER_LOOKUP
	}

	public enum LinkingModule {
		GAZETTEER_LOOKUP_EXACT
	}

	public enum DisambiguationModule {
		FIRST_MATCH_DISAMBIGUATION, HIGHEST_POPULATION_DISAMBIGUATION, HIGHEST_ADMIN_LEVEL_DISAMBIGUATION,
		CONTEXT_TOPONYM_DISAMBIGUATION, ADVANCED_CONTEXT_TOPONYM_DISAMBIGUATION, WIKIPEDIA_LOCATION_NETWORK_DISAMBIGUATOR
	}

	public final GeoparsingApproach.RecognitionModule recognitionModule;
	public final GeoparsingApproach.LinkingModule linkingModule;
	public final GeoparsingApproach.DisambiguationModule disambiguationModule;
	public Geoparser geoparser;

	public GeoparsingApproach(final GeoparsingApproach.RecognitionModule recognitionModule,
			final GeoparsingApproach.LinkingModule linkingModule,
			final GeoparsingApproach.DisambiguationModule disambiguationModule) {
		this.recognitionModule = recognitionModule;
		this.linkingModule = linkingModule;
		this.disambiguationModule = disambiguationModule;
	}
}