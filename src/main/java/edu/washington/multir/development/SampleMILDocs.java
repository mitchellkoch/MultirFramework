package edu.washington.multir.development;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multirframework.multiralgorithm.MILDocument;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multir.util.ModelUtils;

public class SampleMILDocs {
	
	
	public static void main(String[] args) throws IOException, SQLException{
		MILDocument d = new MILDocument();
		DataInputStream dis = new DataInputStream(new FileInputStream(new File(args[0])));
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(args[1])));
		BufferedReader br = new BufferedReader(new FileReader(new File(args[2])));
		CorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
		Corpus c = new Corpus(args[3],cis,true);
		Mappings m = new Mappings();
		m.read(args[4]);
		Map<Integer,String> relId2RelMap = ModelUtils.getRelationIDToRelationMap(m);
		
		Map<Integer,List<MILDocument>> numMentionEntityPairMap = new HashMap<>();
		Map<Pair<String,String>,List<String>> entityPairSentencesMap = new HashMap<>();
		Set<Pair<String,String>> targetEntityPairs = new HashSet<>();
		
		
		System.out.println("Configured");

		
		while(d.read(dis)){
			Integer key = 0;
			if(d.numMentions == 1){
				key = 1;
			}
			else if(d.numMentions == 2){
				key = 2;
			}
			else if(d.numMentions >= 3 && d.numMentions <=6){
				key = 3;
			}
			else if(d.numMentions >=7 && d.numMentions <=10){
				key = 4;
			}
			else{
				key = 5;
			}
			
			if(d.Y.length > 0){
				MILDocument newD = new MILDocument();
				newD.setCapacity(d.numMentions);
				newD.arg1 = d.arg1;
				newD.arg2 = d.arg2;
				newD.features = d.features;
				newD.mentionIDs = d.mentionIDs;
				newD.numMentions = d.numMentions;
				newD.Y = d.Y;
				newD.Z = d.Z;
				if(numMentionEntityPairMap.containsKey(key)){
					numMentionEntityPairMap.get(key).add(newD);
				}
				else{
					List<MILDocument> entityPairs = new ArrayList<>();
					entityPairs.add(newD);
					numMentionEntityPairMap.put(key, entityPairs);
				}
			}
			
			//bw.write(d.toString()+"\n");
		}
		
		System.out.println("REad training file");

		
		
		for(Integer k : numMentionEntityPairMap.keySet()){
			List<MILDocument> entityPairs = numMentionEntityPairMap.get(k);
			Collections.shuffle(entityPairs, new Random((int)System.currentTimeMillis()));
			List<MILDocument> top100 = entityPairs.subList(0, Math.min(100, entityPairs.size()));
			for(MILDocument doc: top100){
				Pair<String,String> ep = new Pair<>(doc.arg1,doc.arg2);
				targetEntityPairs.add(ep);
			}
			numMentionEntityPairMap.put(k,top100);
		}
		
		System.out.println("Shuffled EntityPairs");

		
		String nextLine;
		while((nextLine = br.readLine())!=null){
			String[] values = nextLine.split("\t");
			String arg1Id = values[0];
			String arg2Id = values[4];
			String arg1Name = values[3];
			String arg2Name = values[7];
			Integer arg1StartOffset = Integer.parseInt(values[1]);
			Integer arg2StartOffset = Integer.parseInt(values[5]);
			String sentNumString = values[8];
			Integer sentNum = Integer.parseInt(sentNumString);
			Pair<String,String> ep = new Pair<String,String>(arg1Id,arg2Id);
			
			if(targetEntityPairs.contains(ep)){
				System.out.println("reading sent text for " + sentNum);
				String sentText = c.getSentence(sentNum).get(CoreAnnotations.TextAnnotation.class);
				sentText = arg1Name + "\t" + arg1StartOffset + "\t" + arg2Name + "\t" + arg2StartOffset + "\t" + sentText;
				if(entityPairSentencesMap.containsKey(ep)){
					if(!entityPairSentencesMap.get(ep).contains(sentText)) entityPairSentencesMap.get(ep).add(sentText);
				}
				else{
					List<String> sentences = new ArrayList<>();
					sentences.add(sentText);
					entityPairSentencesMap.put(ep,sentences);
				}
			}
			
		}
		
		System.out.println("Read DS file");

		
		
		for(Integer k : numMentionEntityPairMap.keySet()){
			System.out.println("key = " + k);
			bw.write("Key = " + k + "\n");
			List<MILDocument> entityPairs = numMentionEntityPairMap.get(k);
			for(MILDocument doc: entityPairs){
				Pair<String,String> entityPair = new Pair<String,String>(doc.arg1,doc.arg2);
				List<String> sentences = entityPairSentencesMap.get(entityPair);
				bw.write(doc.arg1+"\t"+doc.arg2+"\t"+doc.numMentions+"\t");
				for(Integer i :doc.Y){
					bw.write(relId2RelMap.get(i)+ " ");
				}
				bw.write("\n");
				for(String sent: sentences){
					bw.write(sent+"\n");
				}
			}
		}
		
		
		
		bw.close();
		br.close();
		dis.close();	
	}

}
