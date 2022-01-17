package org.joinmastodon.android.ui.text;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.joinmastodon.android.model.Emoji;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;

public class HtmlParser{
	private static final String TAG="HtmlParser";
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
	public static SpannableStringBuilder parse(String source, List<Emoji> emojis){
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

		final SpannableStringBuilder ssb=new SpannableStringBuilder();
		Jsoup.parseBodyFragment(source).body().traverse(new NodeVisitor(){
			private final ArrayList<SpanInfo> openSpans=new ArrayList<>();

			@Override
			public void head(@NonNull Node node, int depth){
				if(node instanceof TextNode){
					ssb.append(((TextNode) node).text());
				}else if(node instanceof Element){
					Element el=(Element)node;
					switch(el.nodeName()){
						case "a" -> {
							LinkSpan.Type linkType;
							if(el.hasClass("hashtag")){
								linkType=LinkSpan.Type.HASHTAG;
							}else if(el.hasClass("mention")){
								linkType=LinkSpan.Type.MENTION;
							}else{
								linkType=LinkSpan.Type.URL;
							}
							openSpans.add(new SpanInfo(new LinkSpan(el.attr("href"), null, linkType), ssb.length(), el));
						}
						case "br" -> ssb.append('\n');
						case "span" -> {
							if(el.hasClass("invisible")){
								openSpans.add(new SpanInfo(new InvisibleSpan(), ssb.length(), el));
							}
						}
					}
				}
			}

			@Override
			public void tail(@NonNull Node node, int depth){
				if(node instanceof Element){
					Element el=(Element)node;
					if("span".equals(el.nodeName()) && el.hasClass("ellipsis")){
						ssb.append('…');
					}else if("p".equals(el.nodeName())){
						if(node.nextSibling()!=null)
							ssb.append("\n\n");
					}else if(!openSpans.isEmpty()){
						SpanInfo si=openSpans.get(openSpans.size()-1);
						if(si.element==el){
							ssb.setSpan(si.span, si.start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
							openSpans.remove(openSpans.size()-1);
						}
					}
				}
			}
		});
		if(!emojis.isEmpty())
			parseCustomEmoji(ssb, emojis);
		return ssb;
	}

	public static void parseCustomEmoji(SpannableStringBuilder ssb, List<Emoji> emojis){
		Map<String, Emoji> emojiByCode=emojis.stream().collect(Collectors.toMap(e->e.shortcode, Function.identity()));
		Matcher matcher=EMOJI_CODE_PATTERN.matcher(ssb);
		while(matcher.find()){
			Emoji emoji=emojiByCode.get(matcher.group(1));
			if(emoji==null)
				continue;
			ssb.setSpan(new CustomEmojiSpan(emoji), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}
}
