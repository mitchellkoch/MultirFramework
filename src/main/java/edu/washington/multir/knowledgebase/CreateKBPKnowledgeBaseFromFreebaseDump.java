package edu.washington.multir.knowledgebase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.washington.multirframework.util.BufferedIOUtils;

public class CreateKBPKnowledgeBaseFromFreebaseDump {
	
	private static String freebaseDumpPath = "/scratch2/resources/freebase-rdf-2014-02-23-00-00.gz";
	private static String outputFile = "/homes/gws/jgilme1/KBPMultir/NewKnowledgeBase/FromFBDump/Filtered-FB-Dump-All.tsv.gz";
	private static String entitiesOutputFile = "/homes/gws/jgilme1/KBPMultir/NewKnowledgeBase/FromFBDump/entities.tsv.gz";
	private static String relevantRelationsFile = "/homes/gws/jgilme1/KBPMultir/NewKnowledgeBase/FromFBDump/relevantFBRelations";
	
	public static Pattern MidPattern = Pattern.compile("<http://rdf\\.freebase.com/ns/(m\\..+?)>");
	public static Pattern GidPattern = Pattern.compile("<http://rdf\\.freebase.com/ns/(g\\..+?)>");
	public static Pattern typePattern = Pattern.compile("<http://rdf\\.freebase.com/ns/(.+?)>");
	public static Pattern namePattern = Pattern.compile("(.+?)@en");
	public static Pattern datePattern = Pattern.compile("(.+?)\\^\\^(.+?)>");
	public static Pattern websitePattern = Pattern.compile("<(.+?)>");
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException{
		BufferedReader in = BufferedIOUtils.getBufferedReader(new File(freebaseDumpPath));
		
		BufferedWriter kbOutput = BufferedIOUtils.getBufferedWriter(new File(outputFile));
		BufferedWriter entityOutput = BufferedIOUtils.getBufferedWriter(new File(entitiesOutputFile));

		String line;
		int lineCount =0 ;

		Set<String> relevantRelations = getRelevantRelations(relevantRelationsFile);
		for(String revRel : relevantRelations){
			System.out.println(revRel);
		}
		while((line = in.readLine())!=null){
			String[] values = line.split("\t");
			
			
			Matcher predicateMatcher = typePattern.matcher(values[1]);
			if(predicateMatcher.matches()){
				String rel =predicateMatcher.group(1);
				if(relevantRelations.contains(rel)){
					Matcher midMatcherArg1 = MidPattern.matcher(values[0]);
					if(midMatcherArg1.matches()){
						String midArg1 = transformId(midMatcherArg1.group(1));
						Matcher midMatcherArg2 = MidPattern.matcher(values[2]);
						Matcher nameMatcher = namePattern.matcher(values[2]);
						Matcher dateMatcher = datePattern.matcher(values[2]);
						Matcher websiteMatcher = websitePattern.matcher(values[2]);
						StringBuilder sb = new StringBuilder();
						sb.append(midArg1);
						sb.append("\t");
						if(midMatcherArg2.matches()){
							String midArg2 = transformId(midMatcherArg2.group(1));
							sb.append(midArg2);
							sb.append("\t");
							sb.append(convertToKBStyle(rel));
							kbOutput.write(sb.toString().trim()+"\n");
							
						}
						else if(nameMatcher.matches()){
							String name = nameMatcher.group(1);
							sb.append(name.replaceAll("\"", ""));
							entityOutput.write(sb.toString().trim()+"\n");
						}
						else if(dateMatcher.matches()){
							String dateString = dateMatcher.group(1);
							sb.append(dateString.replaceAll("\"", ""));
							sb.append("\t");
							sb.append(convertToKBStyle(rel));
							kbOutput.write(sb.toString().trim()+"\n");

						}
						else if(websiteMatcher.matches()){
							String website = websiteMatcher.group(1);
							sb.append(website.replaceAll("\"", ""));
							sb.append("\t");
							sb.append(convertToKBStyle(rel));
							kbOutput.write(sb.toString().trim()+"\n");
						}
						else{
							
						}
					}
				}
			}
			lineCount++;
			if(lineCount % 20000000 == 0){
				System.out.println(lineCount);
				kbOutput.flush();
				entityOutput.flush();
			}
		}
		in.close();
		kbOutput.close();
		entityOutput.close();
	}

	private static Set<String> getRelevantRelations(
			String relevantRelationsFile2) throws IOException {
		Set<String> relevantRelations = new HashSet<>();
		
		BufferedReader br = new BufferedReader(new FileReader(new File(relevantRelationsFile2)));
		String nextLine;
		while((nextLine = br.readLine())!=null){
			relevantRelations.add(convertToFBDumpStyle(nextLine.trim()));
		}
		
		br.close();
		
		return relevantRelations;
	}

	private static String convertToFBDumpStyle(String relString) {
		return relString.replaceAll("\\/", "\\.").substring(1);
	}
	
	private static String convertToKBStyle(String relString){
		return "/"+relString.replaceAll("\\.", "/");
	}
	private static String transformId(String rdfGid) {
		String newid = rdfGid.replaceAll("\\.", "/");
		return "/" + newid;
	}

}
