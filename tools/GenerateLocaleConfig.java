// Run: java tools/GenerateLocaleConfig.java
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.*;

public class GenerateLocaleConfig{
	public static void main(String[] args) throws IOException{
		File dir=new File("../mastodon/src/main/res/");
		if(!dir.exists())
			dir=new File("mastodon/src/main/res");
		if(!dir.exists())
			throw new RuntimeException("Please run from project directory (can't find mastodon/src/main/res)");

		ArrayList<String> locales=new ArrayList<>(), rawLocales=new ArrayList<>();
		locales.add("en");

		for(File file:dir.listFiles()){
			String name=file.getName();
			if(file.isDirectory() && name.startsWith("values-")){
				if(new File(file, "strings.xml").exists()){
					locales.add(name.substring(name.indexOf('-')+1).replace("-r", "-"));
					rawLocales.add(name.substring(name.indexOf('-')+1));
				}
			}
		}

		locales.sort(String::compareTo);
		rawLocales.sort(String::compareTo);
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

		File buildGradle=new File(dir, "../../../build.gradle");
		ArrayList<String> buildGradleLines=new ArrayList<>();
		try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(buildGradle)))){
			String line;
			while((line=reader.readLine())!=null){
				if(line.trim().startsWith("resConfigs")){
					line=line.substring(0, line.indexOf('r'))+"resConfigs ";
					line+=rawLocales.stream().map(l->'"'+l+'"').collect(Collectors.joining(", "));
				}
				buildGradleLines.add(line);
			}
		}

		try(OutputStreamWriter writer=new OutputStreamWriter(new FileOutputStream(buildGradle))){
			for(String line:buildGradleLines){
				writer.write(line);
				writer.write('\n');
			}
		}
	}
}