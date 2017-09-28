package de.unihd.dbs.geoparser.viewer.model;

import java.util.ArrayList;
import java.util.List;

import de.unihd.dbs.geoparser.core.Document;
import de.unihd.dbs.geoparser.core.LinkedToponym;
import de.unihd.dbs.geoparser.core.NamedEntity;
import de.unihd.dbs.geoparser.core.ResolvedToponym;

public class GeoparsingResult {
	public final String inputText;
	public final Document document;
	public final List<NamedEntity> foundEntities = new ArrayList<>();
	public final List<LinkedToponym> linkedToponyms = new ArrayList<>();
	public final List<ResolvedToponym> resolvedToponyms = new ArrayList<>();

	public GeoparsingResult(final String inputText) {
		this.inputText = inputText;
		this.document = new Document(inputText);
	}

}