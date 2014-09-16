package edu.washington.multir.knowledgebase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import edu.washington.multirframework.util.BufferedIOUtils;

public class CheckForRelationsInKB {
	
	public static void main(String[] args) throws IOException{
		String relationFilePath = args[0];
		String kbFile = args[1];
		List<String> relations = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(new File(relationFilePath)));
		String nextLine;
		while((nextLine = br.readLine())!=null){
			relations.add(nextLine.trim());
		}
		br.close();
		
		System.out.println("Target Relations:");
		for(String rel : relations){
			System.out.println(rel);
		}
		

		BufferedReader kbReader =
				BufferedIOUtils.getBufferedReader(new File(kbFile));
		
		Set<String> relationsInKB = new HashSet<>();
		Set<String> relationsNotInKB = new HashSet<>();
		while((nextLine = kbReader.readLine())!=null){
			String[] values = nextLine.split("\t");
			String rel = values[2];
			relationsInKB.add(rel);
		}
		kbReader.close();
		
		for(String rel : relations){
			if(!relationsInKB.contains(rel)){
				relationsNotInKB.add(rel);
			}
		}
		
		System.out.println("Relations not in KB:");
		for(String rel : relationsNotInKB){
			System.out.println(rel);
		}
		System.out.println("Relations in KB:");
		for(String rel : relationsInKB){
			System.out.println(rel);
		}
	}

}
