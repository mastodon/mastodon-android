package org.joinmastodon.android.ui;

import android.content.Context;
import android.os.Build;
import android.util.LruCache;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.annotation.Nullable;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.ui.text.FuriganaSpan;
import org.joinmastodon.android.ui.text.LinkSpan;

import java.util.ArrayList;
import java.util.List;

public class FuriganaHelper {

	private static final LruCache<Integer, SpannableStringBuilder> cache =
			new LruCache<>(200);

	private static volatile Tokenizer tokenizer;

	private static Tokenizer getTokenizer() {
		if (tokenizer == null) {
			synchronized (FuriganaHelper.class) {
				if (tokenizer == null) {
					tokenizer = new Tokenizer();
				}
			}
		}
		return tokenizer;
	}

	private static String prepareTextForTokenizer(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isKanji(c) || isKana(c)) sb.append(c);
            else sb.append(' ');
        }
        return sb.toString();
    }

	public static SpannableStringBuilder annotateKanjiWithFurigana(Context context, CharSequence input, @Nullable String language) {
		if (input == null || input.length() == 0) return new SpannableStringBuilder("");

		if (!containsJapanese(input)) return new SpannableStringBuilder(input);

		if ("zh".equals(language) || "zh-cn".equals(language) || "zh-tw".equals(language)) {
			return new SpannableStringBuilder(input);
		}

		boolean showFurigana = GlobalUserPreferences.showFurigana;

		boolean useHiragana = GlobalUserPreferences.convertKanaToHiragana;

		if (!showFurigana) {
			return new SpannableStringBuilder(input);
		}

		int hash = input.hashCode();
		SpannableStringBuilder cached = cache.get(hash);
		if (cached != null) return new SpannableStringBuilder(cached);

		SpannableStringBuilder ssb = new SpannableStringBuilder(input);
		String text = input.toString();
		String prepared = prepareTextForTokenizer(text);
		List<Token> tokens = getTokenizer().tokenize(prepared);
		int offset = 0;

		for (Token token : tokens) {
			String surface = token.getSurface();
			String reading = token.getReading();

			if (reading != null && containsKanji(surface)) {
				if (surface.contains("〇")) {
					offset += surface.length();
					continue;
				}

				if (useHiragana) reading = katakanaToHiragana(reading);

				List<FuriganaSplitter.Part> parts = FuriganaSplitter.splitSurfaceReading(surface, reading);
				for (FuriganaSplitter.Part part : parts) {
					int start = text.indexOf(part.text, offset);
					if (start >= 0) {
						int end = start + part.text.length();

						if (part.furigana != null && !part.furigana.isEmpty()) {
							FuriganaSpan fs = new FuriganaSpan(part.furigana, 0.8f);
							if (input instanceof Spanned spanned){
								fs.captureVisualsFrom(spanned, start, end, context);
							}
							ssb.setSpan(fs, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						}

						offset = end;
					}
				}
			} else {
				offset += surface.length();
			}
		}

		cache.put(hash, new SpannableStringBuilder(ssb));
		return ssb;
	}

	public static boolean containsJapanese(CharSequence text) {
		if (text == null) return false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (isKanji(c) || isKana(c)) return true;
		}
		return false;
	}

	private static boolean isKana(char c) {
		return (c >= 0x3040 && c <= 0x309F) || (c >= 0x30A0 && c <= 0x30FF);
	}

	private static boolean isKanji(char c) {
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
		}
		return false;
	}

	private static boolean containsKanji(String text) {
		return text.matches(".*[\\p{IsHan}].*");
	}

	private static String katakanaToHiragana(String katakana) {
		StringBuilder sb = new StringBuilder();
		for (char c : katakana.toCharArray()) {
			if (c >= 0x30A1 && c <= 0x30F6) {
				sb.append((char) (c - 0x60));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static void preload() {
		new Thread(() -> {
			try {
				getTokenizer().tokenize("テスト");
			} catch (Exception ignored) {}
		}).start();
	}

	public static void clearCache() {
		cache.evictAll();
	}

	public static class FuriganaSplitter {

		public static class Part {
			public final String text;
			public final String furigana;
			public Part(String text, String furigana) {
				this.text = text;
				this.furigana = furigana;
			}
		}

		public static List<Part> splitSurfaceReading(String surface, String reading) {
			List<Part> parts = new ArrayList<>();
			if (surface == null || surface.isEmpty() || reading == null || reading.isEmpty()) {
				parts.add(new Part(surface, null));
				return parts;
			}

			int sLen = surface.length();
			int rLen = reading.length();

			int firstKanji = -1, lastKanji = -1;
			for (int i = 0; i < sLen; i++) {
				if (isKanji(surface.charAt(i))) {
					if (firstKanji == -1) firstKanji = i;
					lastKanji = i;
				}
			}
			if (firstKanji == -1) {
				parts.add(new Part(surface, null));
				return parts;
			}

			int prefixKanaCount = 0;
			for (int i = 0; i < firstKanji; i++) {
				if (isKana(surface.charAt(i))) prefixKanaCount++;
			}

			int tailKanaCount = 0;
			for (int i = sLen - 1; i > lastKanji; i--) {
				if (isKana(surface.charAt(i))) tailKanaCount++;
				else break;
			}

			int readStart = Math.min(prefixKanaCount, rLen);
			int readEnd = Math.max(readStart, rLen - tailKanaCount);

			String coreReading = "";
			if (readStart < readEnd) {
				coreReading = reading.substring(readStart, readEnd);
			} else {
				coreReading = "";
			}

			if (firstKanji > 0) {
				parts.add(new Part(surface.substring(0, firstKanji), null));
			}

			String kanjiBlock = surface.substring(firstKanji, lastKanji + 1);
			if (coreReading != null && !coreReading.isEmpty()) {
				parts.add(new Part(kanjiBlock, coreReading));
			} else {
				parts.add(new Part(kanjiBlock, null));
			}

			if (lastKanji < sLen - 1) {
				parts.add(new Part(surface.substring(lastKanji + 1), null));
			}

			return parts;
		}

		private static boolean isKana(char c) {
			return (c >= 0x3040 && c <= 0x309F) || (c >= 0x30A0 && c <= 0x30FF);
		}

		private static boolean isKanji(char c) {
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
				return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
			}
			return false;
		}
	}
}
