package org.joinmastodon.android.api;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import android.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

/**
 * RFC 9651 parser and serializer
 */
public class StructuredHttpHeaders{
	private static final Pattern NON_BASE64=Pattern.compile("[^a-zA-Z0-9+/=]");
	private static final Pattern VALID_TOKEN=Pattern.compile("^[a-zA-Z*][a-zA-Z0-9!#$%&'*+.^_`|~:/-]*$");
	private static final Pattern VALID_KEY=Pattern.compile("^[a-z*][a-z0-9_.*-]*$");
	private static final DecimalFormat SERIALIZATION_DECIMAL=(DecimalFormat) NumberFormat.getNumberInstance(Locale.ROOT);

	static{
		SERIALIZATION_DECIMAL.applyPattern("#.#");
		SERIALIZATION_DECIMAL.setMinimumFractionDigits(1);
		SERIALIZATION_DECIMAL.setMaximumFractionDigits(3);
		SERIALIZATION_DECIMAL.setRoundingMode(RoundingMode.DOWN);
	}

	public static List<ItemOrInnerList> parseList(String header){
		StringScanner scanner=new StringScanner(header);
		scanner.discardLeadingSP();
		List<ItemOrInnerList> result=parseList(scanner);
		scanner.discardLeadingSP();
		if(!scanner.isEmpty())
			throw new IllegalArgumentException("Expected end of input");
		return result;
	}

	public static Map<String, ItemOrInnerList> parseDictionary(String header){
		StringScanner scanner=new StringScanner(header);
		scanner.discardLeadingSP();
		Map<String, ItemOrInnerList> result=parseDictionary(scanner);
		scanner.discardLeadingSP();
		if(!scanner.isEmpty())
			throw new IllegalArgumentException("Expected end of input");
		return result;
	}

	public static Item parseItem(String header){
		StringScanner scanner=new StringScanner(header);
		scanner.discardLeadingSP();
		Item result=parseItem(scanner);
		scanner.discardLeadingSP();
		if(!scanner.isEmpty())
			throw new IllegalArgumentException("Expected end of input");
		return result;
	}

	public static String serialize(List<ItemOrInnerList> list){
		StringBuilder sb=new StringBuilder();
		serializeList(sb, list);
		return sb.toString();
	}

	public static String serialize(Map<String, ItemOrInnerList> dict){
		StringBuilder sb=new StringBuilder();
		serializeDictionary(sb, dict);
		return sb.toString();
	}

	public static String serialize(Item item){
		StringBuilder sb=new StringBuilder();
		serializeItem(sb, item);
		return sb.toString();
	}

	// region Serializer internals

	// 4.1.1. Serializing a List
	private static void serializeList(StringBuilder out, List<ItemOrInnerList> list){
		int remain=list.size();
		for(ItemOrInnerList il:list){
			if(il instanceof Item item){
				serializeItem(out, item);
			}else if(il instanceof InnerList innerList){
				serializeInnerList(out, innerList);
			}
			remain--;
			if(remain>0)
				out.append(", ");
		}
	}

	// 4.1.1.1. Serializing an Inner List
	private static void serializeInnerList(StringBuilder out, InnerList innerList){
		out.append('(');
		int remain=innerList.size();
		for(Item item:innerList){
			serializeItem(out, item);
			remain--;
			if(remain>0)
				out.append(' ');
		}
		out.append(')');
		serializeParameters(out, innerList.parameters);
	}

	// 4.1.1.2. Serializing Parameters
	private static void serializeParameters(StringBuilder out, Map<String, BareItem> params){
		params.forEach((key, value)->{
			out.append(';');
			serializeKey(out, key);
			if(!(value instanceof BareItem.BooleanItem bi && bi.value)){
				out.append('=');
				serializeBareItem(out, value);
			}
		});
	}

	// 4.1.1.3. Serializing a Key
	private static void serializeKey(StringBuilder out, String key){
		if(!VALID_KEY.matcher(key).find())
			throw new IllegalArgumentException("Invalid key '"+key+"'");
		out.append(key);
	}

	// 4.1.2. Serializing a Dictionary
	private static void serializeDictionary(StringBuilder out, Map<String, ItemOrInnerList> dict){
		int[] remain=new int[]{dict.size()};
		dict.forEach((key, value)->{
			serializeKey(out, key);
			if(value instanceof Item i && i.item instanceof BareItem.BooleanItem bi && bi.value){
				serializeParameters(out, i.parameters);
			}else{
				out.append('=');
				if(value instanceof InnerList il){
					serializeInnerList(out, il);
				}else if(value instanceof Item item){
					serializeItem(out, item);
				}
			}
			remain[0]--;
			if(remain[0]>0)
				out.append(", ");
		});
	}

	// 4.1.3. Serializing an Item
	private static void serializeItem(StringBuilder out, Item item){
		serializeBareItem(out, item.item);
		serializeParameters(out, item.parameters);
	}

	// 4.1.3.1. Serializing a Bare Item
	private static void serializeBareItem(StringBuilder out, BareItem item){
		if(Objects.requireNonNull(item) instanceof BareItem.IntegerItem i){
			serializeInteger(out, i.value);
		}else if(item instanceof BareItem.DecimalItem i){
			serializeDecimal(out, i.value);
		}else if(item instanceof BareItem.StringItem i){
			serializeString(out, i.value);
		}else if(item instanceof BareItem.TokenItem i){
			serializeToken(out, i.value);
		}else if(item instanceof BareItem.ByteSequenceItem i){
			serializeByteSequence(out, i.value);
		}else if(item instanceof BareItem.BooleanItem i){
			serializeBoolean(out, i.value);
		}else if(item instanceof BareItem.DateItem i){
			serializeDate(out, i.value);
		}else if(item instanceof BareItem.DisplayStringItem i){
			serializeDisplayString(out, i.value);
		}
	}

	// 4.1.4. Serializing an Integer
	private static void serializeInteger(StringBuilder out, long value){
		if(value<-999_999_999_999_999L || value>999_999_999_999_999L)
			throw new IllegalArgumentException();
		out.append(value);
	}

	// 4.1.5. Serializing a Decimal
	private static void serializeDecimal(StringBuilder out, double value){
		if((int)(Math.abs(value)*10000)%10==5){
			if((int)(Math.abs(value)*1000)%2==1){
				value+=0.0009*Math.signum(value);
			}
		}
		if(value<=-1_000_000_000_000.0 || value>=1_000_000_000_000.0)
			throw new IllegalArgumentException();
		out.append(SERIALIZATION_DECIMAL.format(value));
	}

	// 4.1.6. Serializing a String
	private static void serializeString(StringBuilder out, String value){
		out.append('"');
		for(char c:value.toCharArray()){
			if(c<=0x1f || c>=0x7f)
				throw new IllegalArgumentException();
			if(c=='"' || c=='\\')
				out.append('\\');
			out.append(c);
		}
		out.append('"');
	}

	// 4.1.7. Serializing a Token
	private static void serializeToken(StringBuilder out, String value){
		if(!VALID_TOKEN.matcher(value).find())
			throw new IllegalArgumentException("Invalid token '"+value+"'");
		out.append(value);
	}

	// 4.1.8. Serializing a Byte Sequence
	private static void serializeByteSequence(StringBuilder out, byte[] value){
		out.append(':');
		out.append(Base64.encodeToString(value, 0));
		out.append(':');
	}

	// 4.1.9. Serializing a Boolean
	private static void serializeBoolean(StringBuilder out, boolean value){
		out.append('?');
		out.append(value ? '1' : '0');
	}

	// 4.1.10. Serializing a Date
	private static void serializeDate(StringBuilder out, Instant value){
		out.append('@');
		serializeInteger(out, value.getEpochSecond());
	}

	// 4.1.11. Serializing a Display String
	private static void serializeDisplayString(StringBuilder out, String value){
		char[] hex="0123456789abcdef".toCharArray();
		out.append("%\"");
		byte[] byteArray=value.getBytes(StandardCharsets.UTF_8);
		for(byte _b:byteArray){
			int b=(int)_b & 0xFF;
			if(b==0x25 || b==0x22 || b<=0x1f || b>=0x7f){
				out.append('%');
				out.append(hex[b >> 4]);
				out.append(hex[b & 0xf]);
			}else{
				out.append((char)b);
			}
		}
		out.append('"');
	}

	// endregion
	// region Parser internals

	// 4.2.1. Parsing a List
	private static List<ItemOrInnerList> parseList(StringScanner in){
		List<ItemOrInnerList> members=new ArrayList<>();
		while(!in.isEmpty()){
			members.add(parseItemOrInnerList(in));
			in.discardLeadingOWS();
			if(in.isEmpty())
				return members;
			if(in.consume()!=',')
				throw new IllegalArgumentException("Expected ','");
			in.discardLeadingOWS();
			if(in.isEmpty())
				throw new IllegalArgumentException("Trailing ',' in list");
		}
		return members;
	}

	// 4.2.1.1. Parsing an Item or Inner List
	private static ItemOrInnerList parseItemOrInnerList(StringScanner in){
		if(in.peek()=='(')
			return parseInnerList(in);
		return parseItem(in);
	}

	// 4.2.1.2. Parsing an Inner List
	private static InnerList parseInnerList(StringScanner in){
		if(in.consume()!='(')
			throw new IllegalArgumentException("Expected '('");
		InnerList innerList=new InnerList();
		while(!in.isEmpty()){
			in.discardLeadingSP();
			if(in.peek()==')'){
				in.consume();
				innerList.parameters=parseParameters(in);
				return innerList;
			}
			Item item=parseItem(in);
			innerList.add(item);
			char first=in.peek();
			if(first!=' ' && first!=')')
				throw new IllegalArgumentException("Expected ' ' or ')'");
		}
		throw new IllegalArgumentException("End of inner list not found");
	}

	// 4.2.2. Parsing a Dictionary
	private static Map<String, ItemOrInnerList> parseDictionary(StringScanner in){
		Map<String, ItemOrInnerList> dictionary=new LinkedHashMap<>();
		while(!in.isEmpty()){
			String thisKey=parseKey(in);
			ItemOrInnerList member;
			if(!in.isEmpty() && in.peek()=='='){
				in.consume();
				member=parseItemOrInnerList(in);
			}else{
				member=new Item(BareItem.BooleanItem.TRUE, parseParameters(in));
			}
			dictionary.put(thisKey, member);
			in.discardLeadingOWS();
			if(in.isEmpty())
				return dictionary;
			if(in.consume()!=',')
				throw new IllegalArgumentException("Expected ','");
			in.discardLeadingOWS();
			if(in.isEmpty())
				throw new IllegalArgumentException("Trailing ',' in dictionary");
		}
		return dictionary;
	}

	// 4.2.3. Parsing an Item
	private static Item parseItem(StringScanner in){
		return new Item(parseBareItem(in), parseParameters(in));
	}

	// 4.2.3.1. Parsing a Bare Item
	private static BareItem parseBareItem(StringScanner in){
		char first=in.peek();
		if(first=='-' || isDigit(first)){
			Number number=parseIntegerOrDecimal(in);
			if(number instanceof Long l){
				return new BareItem.IntegerItem(l);
			}else if(number instanceof Double d){
				return new BareItem.DecimalItem(d);
			}
			throw new IllegalArgumentException();
		}else if(first=='"'){
			return new BareItem.StringItem(parseString(in));
		}else if(first=='*' || isAlpha(first)){
			return new BareItem.TokenItem(parseToken(in));
		}else if(first==':'){
			return new BareItem.ByteSequenceItem(parseByteSequence(in));
		}else if(first=='?'){
			return parseBoolean(in) ? BareItem.BooleanItem.TRUE : BareItem.BooleanItem.FALSE;
		}else if(first=='@'){
			return new BareItem.DateItem(parseDate(in));
		}else if(first=='%'){
			return new BareItem.DisplayStringItem(parseDisplayString(in));
		}
		throw new IllegalArgumentException("Unrecognized item type '"+first+"'");
	}

	// 4.2.3.2. Parsing Parameters
	private static Map<String, BareItem> parseParameters(StringScanner in){
		Map<String, BareItem> parameters=new LinkedHashMap<>();
		while(!in.isEmpty()){
			if(in.peek()!=';')
				break;
			in.consume();
			in.discardLeadingSP();
			String paramKey=parseKey(in);
			BareItem paramValue=BareItem.BooleanItem.TRUE;
			if(in.remaining()>0 && in.peek()=='='){
				in.consume();
				paramValue=parseBareItem(in);
			}
			parameters.put(paramKey, paramValue);
		}
		return parameters;
	}

	// 4.2.3.3. Parsing a Key
	private static String parseKey(StringScanner in){
		char first=in.peek();
		if(!isLowercaseAlpha(first) && first!='*')
			throw new IllegalArgumentException();

		StringBuilder outputString=new StringBuilder();
		while(!in.isEmpty()){
			first=in.peek();
			if(!isLowercaseAlpha(first) && !isDigit(first) && first!='*' && first!='_' && first!='-' && first!='.')
				return outputString.toString();
			outputString.append(in.consume());
		}
		return outputString.toString();
	}

	// 4.2.4. Parsing an Integer or Decimal
	private static Number parseIntegerOrDecimal(StringScanner in){
		boolean isInteger=true;
		int sign=1;
		StringBuilder inputNumber=new StringBuilder();
		if(in.peek()=='-'){
			in.consume();
			sign=-1;
		}
		if(in.isEmpty())
			throw new IllegalArgumentException("Empty integer");
		char first=in.peek();
		if(!isDigit(first))
			throw new IllegalArgumentException();
		while(!in.isEmpty()){
			char c=in.consume();
			if(isDigit(c)){
				inputNumber.append(c);
			}else if(isInteger && c=='.'){
				if(inputNumber.length()>12)
					throw new IllegalArgumentException();
				inputNumber.append(c);
				isInteger=false;
			}else{
				in.offset--;
				break;
			}
			if((isInteger && inputNumber.length()>15) || (!isInteger && inputNumber.length()>16))
				throw new IllegalArgumentException("Number too long");
		}
		if(isInteger){
			return Long.parseLong(inputNumber.toString())*sign;
		}
		if(inputNumber.charAt(inputNumber.length()-1)=='.')
			throw new IllegalArgumentException();
		if(inputNumber.length()-inputNumber.indexOf(".")>4)
			throw new IllegalArgumentException();
		return Double.parseDouble(inputNumber.toString())*sign;
	}

	// 4.2.5. Parsing a String
	private static String parseString(StringScanner in){
		StringBuilder outputString=new StringBuilder();
		if(in.consume()!='"')
			throw new IllegalArgumentException();
		while(!in.isEmpty()){
			char c=in.consume();
			if(c=='\\'){
				if(in.isEmpty())
					throw new IllegalArgumentException("Unexpected end of input");
				char nextChar=in.consume();
				if(nextChar!='"' && nextChar!='\\')
					throw new IllegalArgumentException("Expected '\"' or '\\'");
				outputString.append(nextChar);
			}else if(c=='"'){
				return outputString.toString();
			}else if(c<=0x1f || c>=0x7f){
				throw new IllegalArgumentException();
			}else{
				outputString.append(c);
			}
		}
		throw new IllegalArgumentException("Expected '\"'");
	}

	// 4.2.6. Parsing a Token
	private static String parseToken(StringScanner in){
		char first=in.peek();
		if(!isAlpha(first) && first!='*')
			throw new IllegalArgumentException("Expected alpha or '*'");
		StringBuilder outputString=new StringBuilder();
		while(!in.isEmpty()){
			first=in.peek();
			if(!isTChar(first) && first!=':' && first!='/')
				return outputString.toString();
			outputString.append(in.consume());
		}
		return outputString.toString();
	}

	// 4.2.7. Parsing a Byte Sequence
	private static byte[] parseByteSequence(StringScanner in){
		if(in.consume()!=':')
			throw new IllegalArgumentException("Expected ':'");
		if(in.s.charAt(in.s.length()-1)!=':') // "If there is not a ":" character before the end of input_string, fail parsing". Feels awkward
			throw new IllegalArgumentException();
		String b64Content=in.s.substring(in.offset, in.s.indexOf(':', in.offset));
		in.offset+=b64Content.length();
		if(in.consume()!=':')
			throw new IllegalArgumentException();
		if(NON_BASE64.matcher(b64Content).find())
			throw new IllegalArgumentException("Invalid base64 '"+b64Content+"'");
		return Base64.decode(b64Content, 0);
	}

	// 4.2.8. Parsing a Boolean
	private static boolean parseBoolean(StringScanner in){
		if(in.consume()!='?')
			throw new IllegalArgumentException("Expected '?'");
		char c=in.consume();
		if(c=='1')
			return true;
		if(c=='0')
			return false;
		throw new IllegalArgumentException("Expected '1' or '0'");
	}

	// 4.2.9. Parsing a Date
	private static Instant parseDate(StringScanner in){
		if(in.consume()!='@')
			throw new IllegalArgumentException("Expected '@'");
		if(parseIntegerOrDecimal(in) instanceof Long l){
			return Instant.ofEpochSecond(l);
		}
		throw new IllegalArgumentException("Expected an integer");
	}

	// 4.2.10. Parsing a Display String
	private static String parseDisplayString(StringScanner in){
		if(in.consume()!='%')
			throw new IllegalArgumentException("Expected '%'");
		if(in.consume()!='"')
			throw new IllegalArgumentException("Expected '\"'");
		ByteArrayOutputStream byteArray=new ByteArrayOutputStream();
		while(!in.isEmpty()){
			char c=in.consume();
			if(c<=0x1f || c>=0x7f)
				throw new IllegalArgumentException();
			if(c=='%'){
				if(in.remaining()<2)
					throw new IllegalArgumentException();
				char hexH=in.consume(), hexL=in.consume();
				if(((hexH<'0' || hexH>'9') && (hexH<'a' || hexH>'f')) || ((hexL<'0' || hexL>'9') && (hexL<'a' || hexL>'f')))
					throw new IllegalArgumentException("Invalid hex");
				byteArray.write(Integer.parseInt(hexH+""+hexL, 16));
			}else if(c=='"'){
				try{
					return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(byteArray.toByteArray())).toString();
				}catch(CharacterCodingException x){
					throw new IllegalArgumentException(x);
				}
			}else{
				byteArray.write(c);
			}
		}
		throw new IllegalArgumentException("Expected '\"'");
	}

	// endregion
	// region Utilities

	private static boolean isDigit(char c){
		return c>='0' && c<='9';
	}

	private static boolean isAlpha(char c){
		return (c>='a' && c<='z') || (c>='A' && c<='Z');
	}

	private static boolean isLowercaseAlpha(char c){
		return c>='a' && c<='z';
	}

	private static boolean isTChar(char c){
		return isAlpha(c) || isDigit(c) || c=='!' || c=='#' || c=='$' || c=='%' || c=='&' || c=='\'' || c=='*' || c=='+' || c=='-' || c=='.' || c=='^' || c=='_' || c=='`' || c=='|' || c=='~';
	}

	// endregion

	public record Item(BareItem item, Map<String, BareItem> parameters) implements ItemOrInnerList{
		public static Item of(Object value){
			return new Item(BareItem.of(value), Map.of());
		}

		static Item ofDisplayString(String value){
			return new Item(new BareItem.DisplayStringItem(value), Map.of());
		}

		static Item ofToken(String value){
			return new Item(new BareItem.TokenItem(value), Map.of());
		}

		public Item withParam(String key, Object value){
			Map<String, BareItem> params=parameters.isEmpty() ? new LinkedHashMap<>() : parameters;
			params.put(key, BareItem.of(value));
			return parameters.isEmpty() ? new Item(item, params) : this;
		}
	}

	public sealed interface ItemOrInnerList{}

	public static final class InnerList extends ArrayList<Item> implements ItemOrInnerList{
		public Map<String, BareItem> parameters;

		public InnerList(int initialCapacity){
			super(initialCapacity);
		}

		public InnerList(){}

		public InnerList(@NonNull Collection<? extends Item> c){
			super(c);
		}

		public InnerList(@NonNull Collection<? extends Item> c, Map<String, BareItem> parameters){
			super(c);
			this.parameters=parameters;
		}

		public InnerList withParam(String key, Object value){
			if(parameters==null || parameters.isEmpty())
				parameters=new LinkedHashMap<>();
			parameters.put(key, BareItem.of(value));
			return this;
		}
	}

	public sealed interface BareItem{
		record IntegerItem(long value) implements BareItem{}
		record DecimalItem(double value) implements BareItem{}
		record StringItem(String value) implements BareItem{}
		record DisplayStringItem(String value) implements BareItem{}
		record TokenItem(String value) implements BareItem{}
		record ByteSequenceItem(byte[] value) implements BareItem{}
		record BooleanItem(boolean value) implements BareItem{
			public static BooleanItem TRUE=new BooleanItem(true);
			public static BooleanItem FALSE=new BooleanItem(false);
		}
		record DateItem(Instant value) implements BareItem{}

		static BareItem ofInteger(long value){
			return new IntegerItem(value);
		}

		static BareItem ofDecimal(double value){
			return new DecimalItem(value);
		}

		static BareItem ofString(String value){
			return new StringItem(value);
		}

		static BareItem ofDisplayString(String value){
			return new DisplayStringItem(value);
		}

		static BareItem ofToken(String value){
			return new TokenItem(value);
		}

		static BareItem ofByteSequence(byte[] value){
			return new ByteSequenceItem(value);
		}

		static BareItem ofBoolean(boolean value){
			return value ? BooleanItem.TRUE : BooleanItem.FALSE;
		}

		static BareItem ofDate(Instant value){
			return new DateItem(value);
		}

		static BareItem of(Object value){
			if(Objects.requireNonNull(value) instanceof BareItem bi){
				return bi;
			}else if(value instanceof Integer i){
				return ofInteger(i);
			}else if(value instanceof Long l){
				return ofInteger(l);
			}else if(value instanceof Float f){
				return ofDecimal(f);
			}else if(value instanceof Double d){
				return ofDecimal(d);
			}else if(value instanceof String s){
				return ofString(s);
			}else if(value instanceof byte[] ba){
				return ofByteSequence(ba);
			}else if(value instanceof Boolean b){
				return ofBoolean(b);
			}else if(value instanceof Instant i){
				return ofDate(i);
			}else{
				throw new IllegalArgumentException("Unexpected object type "+value.getClass());
			}
		}
	}

	private static class StringScanner{
		public final String s;
		public int offset=0;

		public StringScanner(String s){
			this.s=s;
		}

		public boolean isEmpty(){
			return offset==s.length();
		}

		public char peek(){
			if(s.length()==offset)
				throw new IllegalArgumentException();
			return s.charAt(offset);
		}

		public char consume(){
			return s.charAt(offset++);
		}

		public void discardLeadingOWS(){
			while(offset<s.length()){
				char c=s.charAt(offset);
				if(c!=' ' && c!='\t')
					break;
				offset++;
			}
		}

		public void discardLeadingSP(){
			while(offset<s.length() && s.charAt(offset)==' '){
				offset++;
			}
		}

		public int remaining(){
			return s.length()-offset;
		}
	}
}
