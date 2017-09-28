package de.unihd.dbs.geoparser.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.NamedEntityType;

public class NamedEntityTypeTest {

	@Test
	public void testGetNamedEntityTypeByName() {
		final NamedEntityType expectedType = NamedEntityType.LOCATION;
		final NamedEntityType actualType = NamedEntityType.getByName(expectedType.name);
		assertThat(actualType, equalTo(expectedType));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNamedEntityTypeByUnkownName() {
		final String unkownEntityType = "some weird type";
		NamedEntityType.getByName(unkownEntityType);
	}

	@Test
	public void testFromOpenNLPTag() {
		assertThat(NamedEntityType.fromOpenNLPtag("location"), equalTo(NamedEntityType.LOCATION));
		assertThat(NamedEntityType.fromOpenNLPtag("organization"), equalTo(NamedEntityType.ORGANIZATION));
		assertThat(NamedEntityType.fromOpenNLPtag("person"), equalTo(NamedEntityType.PERSON));
		assertThat(NamedEntityType.fromOpenNLPtag("percentage"), equalTo(NamedEntityType.PERCENT));
		assertThat(NamedEntityType.fromOpenNLPtag("date"), equalTo(NamedEntityType.DATE));
		assertThat(NamedEntityType.fromOpenNLPtag("time"), equalTo(NamedEntityType.TIME));
		assertThat(NamedEntityType.fromOpenNLPtag("money"), equalTo(NamedEntityType.MONEY));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFromOpenNLPTagWithUnknownTag() {
		NamedEntityType.fromOpenNLPtag("some weird type");
	}

}
