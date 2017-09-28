package de.unihd.dbs.geoparser.core;

/**
 * Penn Treebank part-of-speech tags for English text.
 * <ul>
 * <li>General Source: http://paula.petcu.tm.ro/init/default/post/opennlp-part-of-speech-tags
 * <li>Source: https://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html
 * </ul>
 * 
 * @author lrichter
 *
 */
public enum PartOfSpeechPTBType {

	// @formatter:off
	COORDINATING_CONJUNCTION("CC"), 
	CARDINAL_NUMBER("CD"),
	DETERMINER("DT"),
	EXISTENTIAL_THERE("EX"), 
	FOREIGN_WORD("FW"), 
	PREPOSITION_OR_SUBORDINATING_CONJUNCTION("IN"), 
	ADJECTIVE("JJ"),
	ADJECTIVE_COMPARATIVE("JJR"), 
	ADJECTIVE_SUPERLATIVE("JJS"), 
	LIST_ITEM_MARKER("LS"), 
	MODAL("MD"),
	NOUN_SINGULAR_OR_MASS("NN"),
	NOUN_LURAL("NNS"), 
	PROPER_NOUN_SINGULAR("NNP"), 
	PROPER_NOUN_PLURAL("NNPS"),
	PREDETERMINER("PDT"), 
	POSSESSIVE_ENDING("POS"), 
	PERSONAL_PRONOUN("PRP"), 
	POSSESSIVE_PRONOUN("PRP$"), 
	ADVERB("RB"),
	ADVERB_COMPARATIVE("RBR"), 
	ADVERB_SUPERLATIVE("RBS"), 
	PARTICLE("RP"), 
	SYMBOL("SYM"), 
	TO("TO"), 
	INTERJECTION("UH"),
	VERB_BASE_FORM("VB"), 
	VERB_PAST_TENSE("VBD"), 
	VERB_GERUND_OR_PRESENT_PARTICIPLE("VBG"), 
	VERB_PAST_PARTICIPLE("VBN"),
	VERB_NON_3RD_PERSON_SINGULAR_PRESENT("VBP"), 
	VERB_3RD_PERSON_SINGULAR_PRESENT("VBZ"), 
	WH_DETERMINER("WDT"),
	WH_PRONOUN("WP"), 
	POSSESSIVE_WH_PRONOUN("WP$"), 
	WH_ADVERB("WRB"), 
	LEFT_ROUND_BRACKET("-LRB-"),
	RIGHT_ROUND_BRACKET("-RRB-"), 
	LEFT_SQUARE_BRACKET("-LSB-"), 
	RIGHT_SQUARE_BRACKET("-RSB-"),
	LEFT_CURLY_BRACKET("-LCB-"), 
	RIGHT_CURLY_BRACKET("-LRB-"),
	COMMA(","),
	SEMICOLON(":"),	// for ':', ';'
	SENTENCE_TERMINATOR("."); // for '.', '?', '!'
	// @formatter:on

	public final String name;

	private PartOfSpeechPTBType(final String name) {
		this.name = name;
	}

	/**
	 * Return the {@link PartOfSpeechPTBType} with given code.
	 * 
	 * @param code the PTB POS code.
	 * @return the matching {@link PartOfSpeechPTBType} or {@link IllegalArgumentException} if no type with the given
	 *         code exists.
	 */
	public static PartOfSpeechPTBType getByCode(final String code) {
		for (final PartOfSpeechPTBType type : PartOfSpeechPTBType.values()) {
			if (type.name.equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unkown PartOfSpeechPTBType `" + code + "`!");
	}

}
