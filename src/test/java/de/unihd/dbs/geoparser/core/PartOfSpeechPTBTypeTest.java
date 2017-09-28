package de.unihd.dbs.geoparser.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class PartOfSpeechPTBTypeTest {

	@Test
	public void testGetByName() {
		final PartOfSpeechPTBType expectedType = PartOfSpeechPTBType.PROPER_NOUN_SINGULAR;
		final PartOfSpeechPTBType actualType = PartOfSpeechPTBType.getByCode(expectedType.name);
		assertThat(actualType, equalTo(expectedType));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetByUnknownName() {
		PartOfSpeechPTBType.getByCode("Some weird name");
	}

}
