package org.joinmastodon.android;

import org.joinmastodon.android.ui.utils.UiUtils;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class NumberAbbreviationTests{
	public NumberAbbreviationTests(){
		Locale.setDefault(Locale.US);
	}

	@Test
	public void does_not_abbreviate_numbers_under_1000(){
		assertEquals("999", UiUtils.abbreviateNumber(999L));
	}

	@Test
	public void formats_thousands_correctly_for_1000(){
		assertEquals("1K", UiUtils.abbreviateNumber(1000L));
	}

	@Test
	public void truncates_decimals_for_1051(){
		assertEquals("1K", UiUtils.abbreviateNumber(1051L));
	}

	@Test
	public void truncates_decimals_for_2999(){
		assertEquals("2.9K", UiUtils.abbreviateNumber(2999L));
	}

	@Test
	public void truncates_decimals_for_9999(){
		assertEquals("9.9K", UiUtils.abbreviateNumber(9999L));
	}

	@Test
	public void truncates_decimals_for_10501(){
		assertEquals("10K", UiUtils.abbreviateNumber(10501L));
	}

	@Test
	public void truncates_decimals_for_11000(){
		assertEquals("11K", UiUtils.abbreviateNumber(11000L));
	}

	@Test
	public void truncates_decimals_for_99999(){
		assertEquals("99K", UiUtils.abbreviateNumber(99999L));
	}

	@Test
	public void truncates_decimals_for_100501(){
		assertEquals("100K", UiUtils.abbreviateNumber(100501L));
	}

	@Test
	public void truncates_decimals_for_101000(){
		assertEquals("101K", UiUtils.abbreviateNumber(101000L));
	}

	@Test
	public void truncates_decimals_for_999999(){
		assertEquals("999K", UiUtils.abbreviateNumber(999999L));
	}

	@Test
	public void truncates_decimals_for_2999999(){
		assertEquals("2.9M", UiUtils.abbreviateNumber(2999999L));
	}

	@Test
	public void truncates_decimals_for_9999999(){
		assertEquals("9.9M", UiUtils.abbreviateNumber(9999999L));
	}
}
