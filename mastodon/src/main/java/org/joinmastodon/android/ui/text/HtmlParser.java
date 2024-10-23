package org.joinmastodon.android.ui.text;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import com.twitter.twittertext.Regex;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.FilterResult;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.V;

public class HtmlParser{
	private static final String TAG="HtmlParser";
	private static final String VALID_URL_PATTERN_STRING =
					"(" +                                                            //  $1 total match
						"(" + Regex.URL_VALID_PRECEDING_CHARS + ")" +                        //  $2 Preceding character
						"(" +                                                          //  $3 URL
						"(https?://)" +                                             //  $4 Protocol (optional)
						"(" + Regex.URL_VALID_DOMAIN + ")" +                               //  $5 Domain(s)
						"(?::(" + Regex.URL_VALID_PORT_NUMBER + "))?" +                    //  $6 Port number (optional)
						"(/" +
						Regex.URL_VALID_PATH + "*+" +
						")?" +                                                       //  $7 URL Path and anchor
						"(\\?" + Regex.URL_VALID_URL_QUERY_CHARS + "*" +                   //  $8 Query String
						Regex.URL_VALID_URL_QUERY_ENDING_CHARS + ")?" +
						")" +
					")";
	public static final Pattern URL_PATTERN=Pattern.compile(VALID_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
	private static final Pattern INVITE_LINK_PATH=Pattern.compile("/invite/[a-z\\d]+$", Pattern.CASE_INSENSITIVE);
	private static Pattern EMOJI_CODE_PATTERN=Pattern.compile(":([\\w]+):");

	private HtmlParser(){}

	/**
	 * Parse HTML and custom emoji into a spanned string for display.
	 * Supported tags: <ul>
	 * <li>&lt;a class="hashtag | mention | (none)"></li>
	 * <li>&lt;span class="invisible | ellipsis"></li>
	 * <li>&lt;br/></li>
	 * <li>&lt;p></li>
	 * </ul>
	 * @param source Source HTML
	 * @param emojis Custom emojis that are present in source as <code>:code:</code>
	 * @return a spanned string
	 */
	public static SpannableStringBuilder parse(String source, List<Emoji> emojis, List<Mention> mentions, List<Hashtag> tags, String accountID, Object parentObject, Context context){
		class SpanInfo{
			public Object span;
			public int start;
			public Element element;

			public SpanInfo(Object span, int start, Element element){
				this.span=span;
				this.start=start;
				this.element=element;
			}
		}

		Map<String, String> idsByUrl=mentions.stream().distinct().collect(Collectors.toMap(m->m.url, m->m.id));
		// Hashtags in remote posts have remote URLs, these have local URLs so they don't match.
//		Map<String, String> tagsByUrl=tags.stream().collect(Collectors.toMap(t->t.url, t->t.name));
		Map<String, Hashtag> tagsByTag=tags.stream().distinct().collect(Collectors.toMap(t->t.name.toLowerCase(), Function.identity()));
		Map<String, Mention> mentionsByID=mentions.stream().distinct().collect(Collectors.toMap(m->m.id, Function.identity()));

		final SpannableStringBuilder ssb=new SpannableStringBuilder();
		Jsoup.parseBodyFragment(source).body().traverse(new NodeVisitor(){
			private final ArrayList<SpanInfo> openSpans=new ArrayList<>();
			private boolean lastElementWasBlock=false;

			private boolean isInsidePre(){
				for(SpanInfo si:openSpans){
					if(si.span instanceof CodeBlockSpan)
						return true;
				}
				return false;
			}

			private boolean isInsideBlockquote(){
				for(SpanInfo si:openSpans){
					if(si.span instanceof BlockQuoteSpan)
						return true;
				}
				return false;
			}

			@SuppressLint("DefaultLocale")
			@Override
			public void head(@NonNull Node node, int depth){
				if(node instanceof TextNode textNode){
					if(lastElementWasBlock){
						lastElementWasBlock=false;
						if(!textNode.text().trim().isEmpty()){
							ssb.append('\n');
							ssb.append("\n", new SpacerSpan(1, V.dp(8)), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					}
					if(isInsidePre()){
						ssb.append(textNode.getWholeText().stripTrailing());
					}else{
						String text=textNode.text();
						if(ssb.length()==0 || ssb.charAt(ssb.length()-1)=='\n')
							text=text.stripLeading();
						ssb.append(text);
					}
				}else if(node instanceof Element el){
					if(lastElementWasBlock || (el.isBlock() && !"li".equals(el.nodeName()) && !"ul".equals(el.nodeName()) && !"ol".equals(el.nodeName()) && ssb.length()>0 && ssb.charAt(ssb.length()-1)!='\n')){
						lastElementWasBlock=false;
						ssb.append('\n');
						ssb.append("\n", new SpacerSpan(1, V.dp(8)), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					switch(el.nodeName()){
						case "a" -> {
							Object linkObject=null;
							String href=el.attr("href");
							LinkSpan.Type linkType;
							if(el.hasClass("hashtag")){
								String text=el.text();
								if(text.startsWith("#")){
									linkType=LinkSpan.Type.HASHTAG;
									href=text.substring(1);
									linkObject=tagsByTag.get(text.substring(1).toLowerCase());
								}else{
									linkType=LinkSpan.Type.URL;
								}
							}else if(el.hasClass("mention")){
								String id=idsByUrl.get(href);
								if(id!=null){
									linkType=LinkSpan.Type.MENTION;
									href=id;
									linkObject=mentionsByID.get(id);
								}else{
									linkType=LinkSpan.Type.URL;
								}
							}else{
								linkType=LinkSpan.Type.URL;
							}
							openSpans.add(new SpanInfo(new LinkSpan(href, null, linkType, accountID, linkObject, parentObject), ssb.length(), el));
						}
						case "br" -> ssb.append('\n');
						case "span" -> {
							if(el.hasClass("invisible")){
								openSpans.add(new SpanInfo(new InvisibleSpan(), ssb.length(), el));
							}
						}
						case "b", "strong" -> openSpans.add(new SpanInfo(new StyleSpan(Typeface.BOLD), ssb.length(), el));
						case "i", "em" -> openSpans.add(new SpanInfo(new StyleSpan(Typeface.ITALIC), ssb.length(), el));
						case "s", "del" -> openSpans.add(new SpanInfo(new StrikethroughSpan(), ssb.length(), el));
						case "code" -> {
							if(!isInsidePre()){
								openSpans.add(new SpanInfo(new MonospaceSpan(context), ssb.length(), el));
								ssb.append(" ", new SpacerSpan(V.dp(4), 0), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
							}
						}
						case "pre" -> openSpans.add(new SpanInfo(new CodeBlockSpan(context), ssb.length(), el));
						case "li" -> {
							Element parent=el.parent();
							if(parent==null)
								return;

							if(ssb.length()>0 && ssb.charAt(ssb.length()-1)!='\n')
								ssb.append('\n');
							String markerText;
							if("ol".equals(parent.nodeName())){
								markerText=String.format("%d.", (parent.hasAttr("start") ? safeParseInt(parent.attr("start")) : 1)+el.elementSiblingIndex());
							}else{
								markerText="•";
							}
							openSpans.add(new SpanInfo(new ListItemMarkerSpan(markerText), ssb.length(), el));
							StringBuilder copyableText=new StringBuilder();
							for(SpanInfo si:openSpans){
								if(si.span instanceof ListItemMarkerSpan ims){
									copyableText.append(ims.text);
								}
							}
							copyableText.append(' ');
							ssb.append(copyableText.toString(), new InvisibleSpan(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
						case "blockquote" -> {
							if(ssb.length()>0 && ssb.charAt(ssb.length()-1)!='\n')
								ssb.append('\n');
							openSpans.add(new SpanInfo(new BlockQuoteSpan(context, !isInsideBlockquote()), ssb.length(), el));
						}
					}
				}
			}

			@Override
			public void tail(@NonNull Node node, int depth){
				if(node instanceof Element el){
					String name=el.nodeName();
					lastElementWasBlock|=el.isBlock();
					if("span".equals(name) && el.hasClass("ellipsis")){
						ssb.append("…", new DeleteWhenCopiedSpan(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					if(!openSpans.isEmpty()){
						SpanInfo si=openSpans.get(openSpans.size()-1);
						if(si.element==el){
							if(si.span!=null){
								if(si.span instanceof MonospaceSpan){
									ssb.append(" ", new SpacerSpan(V.dp(4), 0), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
								}
								ssb.setSpan(si.span, si.start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
							}
							openSpans.remove(openSpans.size()-1);
						}
					}
				}
			}
		});
		int trailingTrimLength=0;
		for(int i=ssb.length()-1;i>=0 && Character.isWhitespace(ssb.charAt(i));i--){
			trailingTrimLength++;
		}
		if(trailingTrimLength>0)
			ssb.replace(ssb.length()-trailingTrimLength, ssb.length(), "");
		if(!emojis.isEmpty())
			parseCustomEmoji(ssb, emojis);
		return ssb;
	}

	private static int safeParseInt(String s){
		try{
			return Integer.parseInt(s);
		}catch(NumberFormatException x){
			return 0;
		}
	}

	public static void parseCustomEmoji(SpannableStringBuilder ssb, List<Emoji> emojis){
		Map<String, Emoji> emojiByCode =
			emojis.stream()
			.collect(
				Collectors.toMap(e->e.shortcode, Function.identity(), (emoji1, emoji2) -> {
					// Ignore duplicate shortcodes and just take the first, it will be
					// the same emoji anyway
					return emoji1;
				})
			);

		Matcher matcher=EMOJI_CODE_PATTERN.matcher(ssb);
		int spanCount=0;
		CustomEmojiSpan lastSpan=null;
		while(matcher.find()){
			Emoji emoji=emojiByCode.get(matcher.group(1));
			if(emoji==null)
				continue;
			ssb.setSpan(lastSpan=new CustomEmojiSpan(emoji), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			spanCount++;
		}
		if(spanCount==1 && ssb.getSpanStart(lastSpan)==0 && ssb.getSpanEnd(lastSpan)==ssb.length()){
			ssb.append(' '); // To fix line height
		}
	}

	public static SpannableStringBuilder parseCustomEmoji(String text, List<Emoji> emojis){
		SpannableStringBuilder ssb=new SpannableStringBuilder(text);
		parseCustomEmoji(ssb, emojis);
		return ssb;
	}

	public static void setTextWithCustomEmoji(TextView view, String text, List<Emoji> emojis){
		if(!EMOJI_CODE_PATTERN.matcher(text).find()){
			view.setText(text);
			return;
		}
		view.setText(parseCustomEmoji(text, emojis));
		UiUtils.loadCustomEmojiInTextView(view);
	}

	public static String strip(String html){
		return Jsoup.clean(html, Safelist.none());
	}

	public static String stripAndRemoveInvisibleSpans(String html){
		Document doc=Jsoup.parseBodyFragment(html);
		doc.body().select("span.invisible").remove();
		Cleaner cleaner=new Cleaner(Safelist.none().addTags("br", "p"));
		StringBuilder sb=new StringBuilder();
		cleaner.clean(doc).body().traverse(new NodeVisitor(){
			@Override
			public void head(Node node, int depth){
				if(node instanceof TextNode tn){
					sb.append(tn.text());
				}else if(node instanceof Element el){
					if("br".equals(el.tagName())){
						sb.append('\n');
					}
				}
			}

			@Override
			public void tail(Node node, int depth){
				if(node instanceof Element el && "p".equals(el.tagName()) && el.nextSibling()!=null){
					sb.append("\n\n");
				}
			}
		});
		return sb.toString();
	}

	public static CharSequence parseLinks(String text){
		Matcher matcher=URL_PATTERN.matcher(text);
		if(!matcher.find()) // Return the original string if there are no URLs
			return text;
		SpannableStringBuilder ssb=new SpannableStringBuilder(text);
		do{
			String url=matcher.group(3);
			if(TextUtils.isEmpty(matcher.group(4)))
				url="http://"+url;
			ssb.setSpan(new LinkSpan(url, null, LinkSpan.Type.URL, null, null, null), matcher.start(3), matcher.end(3), 0);
		}while(matcher.find()); // Find more URLs
		return ssb;
	}

	public static void applyFilterHighlights(Context context, SpannableStringBuilder text, List<FilterResult> filters){
		int fgColor=UiUtils.getThemeColor(context, R.attr.colorM3Error);
		int bgColor=UiUtils.getThemeColor(context, R.attr.colorM3ErrorContainer);
		for(FilterResult filter:filters){
			if(!filter.filter.isActive())
				continue;;
			for(String word:filter.keywordMatches){
				Matcher matcher=Pattern.compile("\\b"+Pattern.quote(word)+"\\b", Pattern.CASE_INSENSITIVE).matcher(text);
				while(matcher.find()){
					ForegroundColorSpan fg=new ForegroundColorSpan(fgColor);
					BackgroundColorSpan bg=new BackgroundColorSpan(bgColor);
					text.setSpan(bg, matcher.start(), matcher.end(), 0);
					text.setSpan(fg, matcher.start(), matcher.end(), 0);
				}
			}
		}
	}

	public static boolean isValidInviteUrl(String url){
		return url.startsWith("https://") && INVITE_LINK_PATH.matcher(url).find();
	}

	public static String normalizeDomain(String domain){
		if(domain.startsWith("www."))
			domain=domain.substring(4);
		return domain;
	}
}
