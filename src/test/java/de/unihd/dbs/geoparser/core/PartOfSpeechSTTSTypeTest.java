package de.unihd.dbs.geoparser.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import org.junit.Test;

public class PartOfSpeechSTTSTypeTest {

	@Test
	public void testGetByName() {
		final PartOfSpeechSTTSType expectedType = PartOfSpeechSTTSType.PROPER_NOUN;
		final PartOfSpeechSTTSType actualType = PartOfSpeechSTTSType.getByName(expectedType.name);
		assertThat(actualType, equalTo(expectedType));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetByUnknownName() {
		PartOfSpeechSTTSType.getByName("Some weird name");
	}

}
