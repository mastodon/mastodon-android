// run: java tools/VerifyTranslatedStringFormatting.java
// Reads all localized strings and makes sure they contain valid formatting placeholders matching the original English strings to avoid crashes.

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class VerifyTranslatedStringFormatting{
	// %[argument_index$][flags][width][.precision][t]conversion
	private static final String formatSpecifier="%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
	private static final Pattern fsPattern=Pattern.compile(formatSpecifier);

	private static HashMap<String, List<String>> placeholdersInStrings=new HashMap<>();
	private static int errorCount=0;

	public static void main(String[] args) throws Exception{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		DocumentBuilder builder=factory.newDocumentBuilder();
		Document doc;
		try(FileInputStream in=new FileInputStream("mastodon/src/main/res/values/strings.xml")){
			doc=builder.parse(in);
		}
		NodeList list=doc.getDocumentElement().getChildNodes(); // why does this stupid NodeList thing exist at all?
		for(int i=0;i<list.getLength();i++){
			if(list.item(i) instanceof Element el){
				String name=el.getAttribute("name");
				String value;
				if("string".equals(el.getTagName())){
					value=el.getTextContent();
				}else if("plurals".equals(el.getTagName())){
					value=el.getElementsByTagName("item").item(0).getTextContent();
				}else{
					System.out.println("Warning: unexpected tag "+name);
					continue;
				}
				ArrayList<String> placeholders=new ArrayList<>();
				Matcher matcher=fsPattern.matcher(value);
				while(matcher.find()){
					placeholders.add(matcher.group());
				}
				placeholdersInStrings.put(name, placeholders);
			}
		}
		for(File file:new File("mastodon/src/main/res").listFiles()){
			if(file.getName().startsWith("values-")){
				File stringsXml=new File(file, "strings.xml");
				if(stringsXml.exists()){
					processFile(stringsXml);
				}
			}
		}
		if(errorCount>0){
			System.err.println("Found "+errorCount+" problems in localized strings");
			System.exit(1);
		}
	}

	private static void processFile(File file) throws Exception{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		DocumentBuilder builder=factory.newDocumentBuilder();
		Document doc;
		try(FileInputStream in=new FileInputStream(file)){
			doc=builder.parse(in);
		}
		NodeList list=doc.getDocumentElement().getChildNodes();
		for(int i=0;i<list.getLength();i++){
			if(list.item(i) instanceof Element el){
				String name=el.getAttribute("name");
				String value;
				if("string".equals(el.getTagName())){
					value=el.getTextContent();
					if(!verifyString(value, placeholdersInStrings.get(name))){
						errorCount++;
						System.out.println(file+": string "+name+" is missing placeholders");
					}
				}else if("plurals".equals(el.getTagName())){
					NodeList items=el.getElementsByTagName("item");
					for(int j=0;j<items.getLength();j++){
						Element item=(Element)items.item(j);
						value=item.getTextContent();
						String quantity=item.getAttribute("quantity");
						if(!verifyString(value, placeholdersInStrings.get(name))){
							// Some languages use zero/one/two for just these numbers so they may skip the placeholder
							// still make sure that there's no '%' characters to avoid crashes
							if(List.of("zero", "one", "two").contains(quantity) && !value.contains("%")){
								continue;
							}
							errorCount++;
							System.out.println(file+": string "+name+"["+quantity+"] is missing placeholders");
						}
					}
				}else{
					System.out.println("Warning: unexpected tag "+name);
					continue;
				}
			}
		}
	}

	private static boolean verifyString(String str, List<String> placeholders){
		for(String placeholder:placeholders){
			if(placeholder.equals("%,d")){
				// %,d and %d are interchangeable but %,d provides nicer formatting
				if(!str.contains(placeholder) && !str.contains("%d"))
					return false;
			}else if(!str.contains(placeholder)){
				return false;
			}
		}
		return true;
	}
}