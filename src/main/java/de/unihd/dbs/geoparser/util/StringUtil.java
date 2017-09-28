package de.unihd.dbs.geoparser.util;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Collection of some String related utility methods.
 *
 * @author lrichter
 *
 */
public class StringUtil {

	// My own stuff
	private final static Pattern singleCaptialLetterPattern = Pattern.compile("(?U)^\\p{Lu}$");

	/**
	 * Check if the string is a single, upper-case letter.
	 * 
	 * @param str the string to check
	 * @return true if the string is a single upper-case letter, false otherwise
	 */
	public static boolean isSingleCapitalLetter(final String str) {
		return singleCaptialLetterPattern.matcher(str).matches();
	}

	/**
	 * Check if the string is a single colon character.
	 * 
	 * @param str the string to check
	 * @return true if the string is a single colon character, false otherwise
	 */
	public static boolean isColon(final String str) {
		return str.length() == 1 && str.charAt(0) == '.';
	}

	private final static Pattern whiteSpacesOnlyPattern = Pattern.compile("^[\\s\\h]+$");

	/**
	 * Check if the string is only consists of white spaces (also includes non-breaking white spaces).
	 * 
	 * @param str the string to check
	 * @return true if the string only consists of white spaces, false otherwise
	 */
	public static boolean isWhiteSpaceOnly(final String str) {
		return whiteSpacesOnlyPattern.matcher(str).matches();
	}

	// --- FROM stanford ner ---
	/**
	 * A case-insensitive variant of {@link Collection#contains}.
	 * 
	 * @param collection Collection&lt;String&gt;
	 * @param str the string to check
	 * @return true if str case-insensitively matches a string in collection
	 */
	public static boolean containsIgnoreCase(final Collection<String> collection, final String str) {
		for (final String squote : collection) {
			if (squote.equalsIgnoreCase(str))
				return true;
		}
		return false;
	}

	/**
	 * Check if the string begins with an upper-case letter.
	 *
	 * @param str the string to check
	 * @return true if the string is capitalized, false otherwise
	 */
	public static boolean isCapitalized(final String str) {
		return !str.isEmpty() && Character.isUpperCase(str.charAt(0));
	}

	private final static Pattern acronymPattern = Pattern.compile("(?U)^[\\p{Lu}]+$");

	/**
	 * Check if the String looks like an acronym.
	 *
	 * @param str the string to check.
	 * @return true if the string is an acronym, false otherwise
	 */
	public static boolean isAcronym(final String str) {
		return acronymPattern.matcher(str).matches();
	}

	// --- FROM opennlp.tools.util.StringUtil ---

	/**
	 * Determines if the specified character is a whitespace.
	 *
	 * A character is considered a whitespace when one of the following conditions is meet:
	 *
	 * <ul>
	 * <li>Its a {@link Character#isWhitespace(int)} whitespace.</li>
	 * <li>Its a part of the Unicode Zs category ({@link Character#SPACE_SEPARATOR}).</li>
	 * </ul>
	 *
	 * <code>Character.isWhitespace(int)</code> does not include no-break spaces. In OpenNLP no-break spaces are also
	 * considered as white spaces.
	 *
	 * @param charCode the character to check
	 * @return true if white space otherwise false
	 */
	public static boolean isWhitespace(final char charCode) {
		return Character.isWhitespace(charCode) || Character.getType(charCode) == Character.SPACE_SEPARATOR;
	}

	/**
	 * Determines if the specified character is a whitespace.
	 *
	 * A character is considered a whitespace when one of the following conditions is meet:
	 *
	 * <ul>
	 * <li>Its a {@link Character#isWhitespace(int)} whitespace.</li>
	 * <li>Its a part of the Unicode Zs category ({@link Character#SPACE_SEPARATOR}).</li>
	 * </ul>
	 *
	 * <code>Character.isWhitespace(int)</code> does not include no-break spaces. In OpenNLP no-break spaces are also
	 * considered as white spaces.
	 *
	 * @param charCode the character to check
	 * @return true if white space otherwise false
	 */
	public static boolean isWhitespace(final int charCode) {
		return Character.isWhitespace(charCode) || Character.getType(charCode) == Character.SPACE_SEPARATOR;
	}

	/**
	 * Converts to lower case independent of the current locale via {@link Character#toLowerCase(char)} which uses
	 * mapping information from the UnicodeData file.
	 *
	 * @param string the string to convert
	 * @return lower cased String
	 */
	public static String toLowerCase(final CharSequence string) {

		final char lowerCaseChars[] = new char[string.length()];

		for (int i = 0; i < string.length(); i++) {
			lowerCaseChars[i] = Character.toLowerCase(string.charAt(i));
		}

		return new String(lowerCaseChars);
	}

	/**
	 * Converts to upper case independent of the current locale via {@link Character#toUpperCase(char)} which uses
	 * mapping information from the UnicodeData file.
	 *
	 * @param string the string to convert
	 * @return upper cased String
	 */
	public static String toUpperCase(final CharSequence string) {
		final char upperCaseChars[] = new char[string.length()];

		for (int i = 0; i < string.length(); i++) {
			upperCaseChars[i] = Character.toUpperCase(string.charAt(i));
		}

		return new String(upperCaseChars);
	}
}
