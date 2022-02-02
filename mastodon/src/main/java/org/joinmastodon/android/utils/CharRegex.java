package org.joinmastodon.android.utils;

import java.util.regex.Pattern;

/**
 * from https://github.com/Richienb/char-regex/blob/master/index.js
 */
public class CharRegex{
	// Used to compose unicode character classes.
	private static final String astralRange = "\\ud800-\\udfff";
	private static final String comboMarksRange = "\\u0300-\\u036f";
	private static final String comboHalfMarksRange = "\\ufe20-\\ufe2f";
	private static final String comboSymbolsRange = "\\u20d0-\\u20ff";
	private static final String comboMarksExtendedRange = "\\u1ab0-\\u1aff";
	private static final String comboMarksSupplementRange = "\\u1dc0-\\u1dff";
	private static final String comboRange = comboMarksRange + comboHalfMarksRange + comboSymbolsRange + comboMarksExtendedRange + comboMarksSupplementRange;
	private static final String varRange = "\\ufe0e\\ufe0f";


	// Used to compose unicode capture groups.
	private static final String astral = "["+astralRange+"]";
	private static final String combo = "["+comboRange+"]";
	private static final String fitz = "\\ud83c[\\udffb-\\udfff]";
	private static final String modifier = "(?:"+combo+"|"+fitz+")";
	private static final String nonAstral = "[^"+astralRange+"]";
	private static final String regional = "(?:\\ud83c[\\udde6-\\uddff]){2}";
	private static final String surrogatePair = "[\\ud800-\\udbff][\\udc00-\\udfff]";
	private static final String zeroWidthJoiner = "\\u200d";
	private static final String blackFlag = "(?:\\ud83c\\udff4\\udb40\\udc67\\udb40\\udc62\\udb40(?:\\udc65|\\udc73|\\udc77)\\udb40(?:\\udc6e|\\udc63|\\udc6c)\\udb40(?:\\udc67|\\udc74|\\udc73)\\udb40\\udc7f)";

	// Used to compose unicode regexes.
	private static final String optModifier = modifier+"?";
	private static final String optVar = "["+varRange+"]?";
	private static final String optJoin = "(?:"+zeroWidthJoiner+"(?:"+nonAstral+"|"+regional+"|"+surrogatePair+")"+optVar + optModifier+")*";
	private static final String seq = optVar + optModifier + optJoin;
	private static final String nonAstralCombo = nonAstral+combo+"?";
	private static final String symbol = "(?:"+blackFlag+"|"+nonAstralCombo+"|"+combo+"|"+regional+"|"+surrogatePair+"|"+astral+")";

	public static final Pattern REGEX=Pattern.compile(fitz+"(?="+fitz+")|"+symbol + seq);
//	public static final Pattern REGEX=Pattern.compile("\\ud83c[\\udffb-\\udfff](?=\\ud83c[\\udffb-\\udfff])|(?:(?:\\ud83c\\udff4\\udb40\\udc67\\udb40\\udc62\\udb40(?:\\udc65|\\udc73|\\udc77)\\udb40(?:\\udc6e|\\udc63|\\udc6c)\\udb40(?:\\udc67|\\udc74|\\udc73)\\udb40\\udc7f)|[^\\ud800-\\udfff][\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff\\u1ab0-\\u1aff\\u1dc0-\\u1dff]?|[\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff\\u1ab0-\\u1aff\\u1dc0-\\u1dff]|(?:\\ud83c[\\udde6-\\uddff]){2}|[\\ud800-\\udbff][\\udc00-\\udfff]|[\\ud800-\\udfff])[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff\\u1ab0-\\u1aff\\u1dc0-\\u1dff]|\\ud83c[\\udffb-\\udfff])?(?:\\u200d(?:[^\\ud800-\\udfff]|(?:\\ud83c[\\udde6-\\uddff]){2}|[\\ud800-\\udbff][\\udc00-\\udfff])[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe2f\\u20d0-\\u20ff\\u1ab0-\\u1aff\\u1dc0-\\u1dff]|\\ud83c[\\udffb-\\udfff])?)*");
}
