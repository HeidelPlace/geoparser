package de.unihd.dbs.geoparser.core;

import edu.stanford.nlp.sequences.SeqClassifierFlags;

/**
 * Supported named entity types, currently focused on Stanford NER tags.
 * 
 * @author lrichter
 *
 */
public enum NamedEntityType {

	// @formatter:off
	LOCATION("LOCATION"), 
	ORGANIZATION("ORGANIZATION"),
	PERSON("PERSON"), 
	MISC("MISC"), 
	DATE("DATE"), 
	TIME("TIME"), 
	DURATION("DURATION"),
	MONEY("MONEY"), 
	NUMBER("NUMBER"),
	PERCENT("PERCENT"), 
	SET("SET"),
	NONE(SeqClassifierFlags.DEFAULT_BACKGROUND_SYMBOL); // "O" used by Stanford NER to mark a token as non NE
	// @formatter:on

	public final String name;

	private NamedEntityType(final String name) {
		this.name = name;
	}

	/**
	 * Return the {@link NamedEntityType} with given name.
	 * 
	 * @param name the named entity type name.
	 * @return the named entity type or {@link IllegalArgumentException} if no type with the given name exists.
	 */
	public static NamedEntityType getByName(final String name) {
		for (final NamedEntityType type : NamedEntityType.values()) {
			if (type.name.equals(name)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unkown NamedEntityType '" + name + "'!");
	}

	/**
	 * Return the {@link NamedEntityType} equivalent to the given OpenNLP named entity type name.
	 * 
	 * @param name the OpenNLP named entity type name.
	 * @return the named entity type or {@link IllegalArgumentException} if no type with the given name exists.
	 */
	public static NamedEntityType fromOpenNLPtag(final String name) {
		switch (name) {
		case "location":
			return NamedEntityType.LOCATION;
		case "organization":
			return NamedEntityType.ORGANIZATION;
		case "person":
			return NamedEntityType.PERSON;
		case "percentage":
			return NamedEntityType.PERCENT;
		case "date":
			return NamedEntityType.DATE;
		case "time":
			return NamedEntityType.TIME;
		case "money":
			return NamedEntityType.MONEY;
		default:
			throw new IllegalArgumentException("Unkown OpenNLP named entity type '" + name + "'!");
		}
	}

}
