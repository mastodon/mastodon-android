// Run: java tools/GenerateLocaleConfig.java
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class GenerateLocaleConfig{
	public static void main(String[] args) throws IOException{
		File dir=new File("../mastodon/src/main/res/");
		if(!dir.exists())
			dir=new File("mastodon/src/main/res");
		if(!dir.exists())
			throw new RuntimeException("Please run from project directory (can't find mastodon/src/main/res)");

		ArrayList<String> locales=new ArrayList<>();
		locales.add("en");

		for(File file:dir.listFiles()){
			String name=file.getName();
			if(file.isDirectory() && name.startsWith("values-")){
				if(new File(file, "strings.xml").exists()){
					locales.add(name.substring(name.indexOf('-')+1).replace("-r", "-"));
				}
			}
		}

		locales.sort(String::compareTo);
		try(OutputStreamWriter writer=new OutputStreamWriter(new FileOutputStream(new File(dir, "xml/locales_config.xml")), StandardCharsets.UTF_8)){
			writer.write("""
					<?xml version="1.0" encoding="utf-8"?>
					<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
					""");
			for(String locale : locales){
				writer.write("\t<locale android:name=\"");
				writer.write(locale);
				writer.write("\"/>\n");
			}
			writer.write("</locale-config>");
		}
	}
}