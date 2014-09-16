package edu.washington.multir.development;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.washington.multirframework.multiralgorithm.MILDocument;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;

public class BagManipulation {
	
	private static Random r = new Random(1);
	private static String newArgSuffix = "%";
	private static int counter = 0;
	private static String groupedPrefix = "grouped-";
	private static int groupBagSize = 3;


	public static void main (String[] args) throws IOException{
		run(args[0],args[1],3);
	}
	
	public static void run(String pathToTrain, String pathToNewTrain, int minBagSize) throws IOException{
		groupBagSize = minBagSize;
		MILDocument d = new MILDocument();
		List<MILDocument> l = new ArrayList<MILDocument>();
		DataInputStream dis = new DataInputStream(new BufferedInputStream
				(new FileInputStream(pathToTrain)));
		
		
		Map<List<Integer>,List<SparseBinaryVector>> relationMentionMap = new HashMap<>();
		List<MILDocument> splitBags = new ArrayList<>();
		
		while (d.read(dis)) {
			//if d num mentions > 15
			if(d.Y.length > 0 && d.numMentions > 15){
				splitBags.addAll(splitBag(d));
			}
			
			//if d num mentions < 5
			else if(d.Y.length > 0 && d.numMentions < 5){
				List<SparseBinaryVector> mentionFeaturesList = new ArrayList<>();
				List<Integer> relKey = new ArrayList<>();
				for(int y : d.Y){
					relKey.add(y);
				}
				
				for(SparseBinaryVector sbv: d.features){
					if(sbv != null) mentionFeaturesList.add(sbv.copy());
				}
				if(relationMentionMap.containsKey(relKey)){
					for(SparseBinaryVector mentionFeatures : mentionFeaturesList){
						relationMentionMap.get(relKey).add(mentionFeatures);
					}
				}
				else{
					relationMentionMap.put(relKey, mentionFeaturesList);
				}
			}
			
			else{
				l.add(copyMILDocument(d));
			}
			d = new MILDocument();
		}
		dis.close();
		
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream( new FileOutputStream(pathToNewTrain)));
		
		//write docs whose mention count is between 5 and 15
		for(MILDocument md : l){
			md.write(dos);
		}
		
		//write split mildocs
		for(MILDocument md : splitBags){
			md.write(dos);
		}
		
		//for each relation type with entity pairs with less than 5 mentions, shuffle all entity pairs, and group 
		//into sets of 5 as new MILDOcuments
		List<MILDocument> newBags = new ArrayList<>();
		System.out.println(relationMentionMap.keySet().size());
		for(List<Integer> relKey : relationMentionMap.keySet()){
			List<SparseBinaryVector> mentionFeatures = relationMentionMap.get(relKey);
			Collections.shuffle(mentionFeatures, r);
			MILDocument md;
			int start = 0;
			String relKeyString = String.valueOf(relKey);
			
			for(int i =groupBagSize; i <= mentionFeatures.size(); i+=groupBagSize){
				md = new MILDocument();
				md.numMentions = groupBagSize;
				md.arg1 = groupedPrefix+relKeyString+newArgSuffix+counter;
				counter++;
				md.arg2 = groupedPrefix+relKeyString+newArgSuffix+counter;
				counter++;
				md.features = mentionFeatures.subList(start, i).toArray(new SparseBinaryVector[md.numMentions]);
				md.Z = new int[md.numMentions];
				md.mentionIDs = new int[md.numMentions];
				for(int j =0; j < md.numMentions; j++){
					md.Z[j] = -1;
					md.mentionIDs[j] = j;
				}
				md.Y= new int[relKey.size()];
				for(int j = 0; j < relKey.size(); j++){
					md.Y[j] = relKey.get(j);
				}
				newBags.add(md);
				start = i;
				
				for(int j =0; j < md.numMentions; j++){
					if(md.features[j] == null){
						System.out.println("start = " + start);
						System.out.println("mention Features = " + mentionFeatures.size());
						System.out.println("num mentions " + md.numMentions);
						System.out.println("Regular group feature at " + j + "=null");
					}
				}
			}
			
			if(start < mentionFeatures.size()){
				md = new MILDocument();
				md.numMentions = mentionFeatures.size() - start;
				md.arg1 = groupedPrefix+relKeyString+newArgSuffix+counter;
				counter++;
				md.arg2 = groupedPrefix+relKeyString+newArgSuffix+counter;
				counter++;
				md.features = mentionFeatures.subList(start, mentionFeatures.size()).toArray(new SparseBinaryVector[md.numMentions]);
				md.Z = new int[md.numMentions];
				md.mentionIDs = new int[md.numMentions];
				for(int j =0; j < md.numMentions; j++){
					md.Z[j] = -1;
					md.mentionIDs[j] = j;
				}
				md.Y=new int[relKey.size()];
				for(int j = 0; j < relKey.size(); j++){
					md.Y[j] = relKey.get(j);
				}
				if(md.numMentions != md.features.length){
					System.out.println("numMentions = " + md.numMentions);
					System.out.println("size of features = " + md.features.length);
				}
				for(int i =0; i < md.numMentions; i++){
					if(md.features[i] == null){
						System.out.println("start = " + start);
						System.out.println("mention Features = " + mentionFeatures.size());
						System.out.println("num mentions " + md.numMentions);
						System.out.println("Last group feature at " + i + "=null");
					}
				}
				newBags.add(md);
			}
		}
		
		for(MILDocument md: newBags){
			md.write(dos);
		}
		
		dos.close();
	}

	private static MILDocument copyMILDocument(MILDocument d) {
		MILDocument md = new MILDocument();
		md.arg1 = d.arg1;
		md.arg2 = d.arg2;
		md.features = d.features;
		md.mentionIDs = d.mentionIDs;
		md.Y = d.Y;
		md.Z = d.Z;
		md.numMentions = d.numMentions;
		return md;
		
	}

	private static List<MILDocument> splitBag(
			MILDocument d) {
		List<MILDocument> newBags = new ArrayList<>();
		MILDocument bigBag = new MILDocument();
		//set big bag to be first 15, and last non-15 bag will be added to it
		bigBag.Y = d.Y;
		bigBag.arg1 = d.arg1;
		bigBag.arg2 = d.arg2;
		bigBag.features = Arrays.copyOf(d.features, 15, SparseBinaryVector[].class);
		bigBag.mentionIDs = Arrays.copyOf(d.mentionIDs,15);
		bigBag.numMentions = 15;
		bigBag.Z = Arrays.copyOf(d.Z, 15);
		
		MILDocument lastBag = null;
		int bagCount = 0;
		for(int j = 15; j < d.numMentions; j+=15){		
			if(lastBag != null){
				newBags.add(lastBag);
				bagCount++;
			}
			int k =0;
			for(k = 1; k < 15; k++){
				if( (k+j) == d.numMentions){
					break;
				}
			}
			int index = j+k;
			lastBag = new MILDocument();
			lastBag.Y = d.Y;
			lastBag.arg1 = d.arg1+newArgSuffix+bagCount;
			lastBag.arg2 = d.arg2+newArgSuffix+bagCount;
			lastBag.features = Arrays.copyOfRange(d.features, j, index, SparseBinaryVector[].class);
			lastBag.mentionIDs = Arrays.copyOfRange(d.mentionIDs,j,index);
			lastBag.numMentions = lastBag.features.length;
			lastBag.Z = Arrays.copyOfRange(d.Z, j, index);
		}
		
		if(lastBag.numMentions == 15){
			newBags.add(lastBag);
			newBags.add(bigBag);
		}
		//combine with bigBag
		else{			
			SparseBinaryVector[] newFeatures = new SparseBinaryVector[bigBag.numMentions+lastBag.numMentions];
			for(int i =0; i < bigBag.numMentions; i++){
				newFeatures[i] = bigBag.features[i];
			}
			for(int j =0; j < lastBag.numMentions; j++){
				newFeatures[bigBag.numMentions+j] = lastBag.features[j];
			}
			bigBag.features = newFeatures;

			int[] newMentionIds = new int[bigBag.numMentions+lastBag.numMentions];
			for(int i =0; i < bigBag.numMentions; i++){
				newMentionIds[i] = bigBag.mentionIDs[i];
			}
			for(int j =0; j <lastBag.numMentions; j++){
				newMentionIds[bigBag.numMentions+j] = lastBag.mentionIDs[j];
			}
			bigBag.mentionIDs = newMentionIds;
			
			int[] newZ = new int[bigBag.numMentions+lastBag.numMentions];
			for(int i =0; i < bigBag.numMentions; i++){
				newZ[i] = bigBag.Z[i];
			}
			for(int j =0; j <lastBag.numMentions; j++){
				newZ[bigBag.numMentions+j] = lastBag.Z[j];
			}
			bigBag.Z = newZ;
			
			bigBag.numMentions = bigBag.numMentions + lastBag.numMentions;
			newBags.add(bigBag);
		}
		return newBags;
	}
	
}
