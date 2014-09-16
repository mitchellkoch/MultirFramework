package edu.washington.multir.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import edu.washington.multirframework.util.BufferedIOUtils;

public class RelatedLocationMap {
	
	public static RelatedLocationMap instance = null;
	
	private Map<String,Set<String>> containsMap = new HashMap<>();
	private Map<String,Set<String>> containedInMap = new HashMap<>();
	private static String defaultPath = "/projects/WebWare6/Multir/MultirSystem/files/kbfiles/fb-rels-all.tsv.gz";
	
	
	private RelatedLocationMap(String relFile) throws IOException{
		
		BufferedReader relationReader = BufferedIOUtils.getBufferedReader(new File(relFile));
		int index =0;
		String relationLine;
		while((relationLine = relationReader.readLine())!=null){
			String[] lineValues = relationLine.split("\t");
			String e1 = lineValues[0];
			String e2 = lineValues[1];
			String rel = lineValues[2];
			
			if(rel.equals("/location/location/containedby")){
				if(containedInMap.containsKey(e1)){
					containedInMap.get(e1).add(e2);
				}
				else{
				   Set<String> containedInSet = new HashSet<>();
				   containedInSet.add(e2);
				   containedInMap.put(e1,containedInSet);
				}
			
			}
			else if(rel.equals("/location/location/contains")){
				if(containsMap.containsKey(e1)){
					containsMap.get(e1).add(e2);
				}
				else{
				   Set<String> containsSet = new HashSet<>();
				   containsSet.add(e2);
				   containsMap.put(e1,containsSet);
				}
				
			}
			if(index % 1000000 == 0){
				System.out.println(index + " lines processed");
			}
			index ++;
		}
		relationReader.close();
	}
	
	public static RelatedLocationMap getInstance() throws IOException{
		
		if(instance == null){
			instance = new RelatedLocationMap(defaultPath);
		}
		return instance;
	}
	
	public static RelatedLocationMap getInstance(String relFile) throws IOException{
		
		if(instance == null){
			instance = new RelatedLocationMap(relFile);
		}
		return instance;
	}
	
	
	
	
	public Set<String> getRelatedLocations(String targetId){
		Set<String> relatedLocations = new HashSet<>();
		if(containsMap.containsKey(targetId)){
			Set<String> containsLocations = new HashSet<>();
			getContainsLocations(targetId,containsLocations);
			relatedLocations.addAll(containsLocations);
		}
		if(containedInMap.containsKey(targetId)){
			Set<String> containedInLocations = new HashSet<>();
			getContainedInLocations(targetId,containedInLocations);
			relatedLocations.addAll(containedInLocations);
		}
		return relatedLocations;
	}

	private void getContainedInLocations(
			String targetId, Set<String> containedInLocations) {
		
		if(targetId == null){
			return;
		}
		else{
			Set<String> containedInSet = containedInMap.get(targetId);
			if(containedInSet == null){
				getContainedInLocations(null,containedInLocations);
			}
			else{
				for(String tId: containedInSet){
					if(!containedInLocations.contains(tId)){
						containedInLocations.add(tId);
						getContainedInLocations(tId,containedInLocations);
					}
				}
			}
		}
	}

	private void getContainsLocations(String targetId,
			Set<String> containsLocations) {
		if(targetId == null){
			return;
		}
		else{
			Set<String> containsSet = containsMap.get(targetId);
			if(containsSet == null){
				getContainsLocations(null,containsLocations);
			}
			else{
				for(String tId: containsSet){
					if(!containsLocations.contains(tId)){
						containsLocations.add(tId);
						getContainsLocations(tId,containsLocations);
					}
				}
			}
		}
	}
}
