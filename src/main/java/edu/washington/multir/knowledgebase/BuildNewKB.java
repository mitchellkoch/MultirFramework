package edu.washington.multir.knowledgebase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.util.BufferedIOUtils;

public class BuildNewKB {
	
	private static boolean print = true;
	
	public static void main(String[] args) throws IOException{
		String pathToOldKB = args[0];
		String pathToRelationDefinition = args[1];
		String pathToNewKB = args[2];
		buildNewKB(pathToOldKB,pathToRelationDefinition,pathToNewKB);
	}
	
	public static void buildNewKB(String pathToOldKB, String pathToRelationDefinition, String pathToNewKB) throws IOException{
		Map<String,String> translationMap = getTranslationMap(pathToRelationDefinition);
		Set<JoinRelation> joinRelations = getJoinRelations(pathToRelationDefinition);
		Map<String,List<Pair<String,String>>> rel1Map = new HashMap<>();
		Map<String,List<Pair<String,String>>> rel2Map = new HashMap<>();

		BufferedReader br = BufferedIOUtils.getBufferedReader(new File(pathToOldKB));
		BufferedWriter bw = BufferedIOUtils.getBufferedWriter(new File(pathToNewKB));
		String nextLine;
		while((nextLine=br.readLine())!=null){
			bw.write(nextLine+"\n");
			String[] values = nextLine.split("\t");
			String e1 = values[0];
			String e2 = values[1];
			String rel = values[2];
			if(translationMap.containsKey(rel)){
				String newLine = e1+"\t"+e2+"\t"+translationMap.get(rel);
				bw.write(newLine+"\n");
				if(print){
					System.out.println("Writing new Line:");
					System.out.println(newLine);
				}
			}
			if(JoinRelation.isRel1(rel, joinRelations)){
				if(rel1Map.containsKey(e1)){
					rel1Map.get(e1).add(new Pair<>(rel,e2));
				}
				else{
					List<Pair<String,String>> e1List = new ArrayList<>();
					e1List.add(new Pair<>(rel,e2));
					rel1Map.put(e1, e1List);
				}
			}
			if(JoinRelation.isRel2(rel,joinRelations)){
				if(rel2Map.containsKey(e1)){
					rel2Map.get(e1).add(new Pair<>(rel,e2));
				}
				else{
					List<Pair<String,String>> e1List = new ArrayList<>();
					e1List.add(new Pair<>(rel,e2));
					rel2Map.put(e1, e1List);
				}
			}
		}
		br.close();
		
		
		//process joinRelations
		for(String e1: rel1Map.keySet()){
			for(Pair<String,String> relPair : rel1Map.get(e1)){
				if(rel2Map.containsKey(relPair.second)){
					for(Pair<String,String> rel2Pair : rel2Map.get(relPair.second)){
						if(!e1.equals(rel2Pair.second)){
								JoinRelation jr = JoinRelation.getJoinRelation(relPair.first, rel2Pair.first, joinRelations);
								if(jr != null){
									String newLine = e1 + "\t" + rel2Pair.second +"\t" +jr.getRel3();
									bw.write(newLine+"\n");
									if(print){
										System.out.println("Writing new Line:");
										System.out.println(newLine);
									}
							}
						}
					}
				}
			}
		}
		
		
		bw.close();
		
	}


	private static Set<JoinRelation> getJoinRelations(
			String pathToRelationDefinition) throws IOException {
		BufferedReader br = BufferedIOUtils.getBufferedReader(new File(pathToRelationDefinition));
		String nextLine;
		Set<JoinRelation> joinRelations = new HashSet<>();
		while((nextLine=br.readLine())!=null){
			String[] values = nextLine.split("->");
			String rel1 = values[0].trim();
			String rel2 = values[1].trim();
			if(rel2.contains("=")){
				String [] vals = rel2.split("=");
				rel2 = vals[0].trim();
				String rel3 = vals[1].trim();
				JoinRelation jr = new JoinRelation(rel1,rel2,rel3);
				if(!joinRelations.contains(jr)){
					joinRelations.add(jr);
				}
			}
		}
		br.close();
		if(print){
			System.out.println("Join Relations");
			for(JoinRelation jr: joinRelations){
				System.out.println(jr);
			}
		}
		return joinRelations;
	}


	private static Map<String, String> getTranslationMap(
			String pathToRelationDefinition) throws IOException {
		BufferedReader br = BufferedIOUtils.getBufferedReader(new File(pathToRelationDefinition));
		String nextLine;
		Map<String,String> translationMap = new HashMap<>();
		while((nextLine=br.readLine())!=null){
			String[] values = nextLine.split("->");
			String rel1 = values[0].trim();
			String rel2 = values[1].trim();
			if(!rel2.contains("=")){
				translationMap.put(rel1,rel2);
			}
		}
		br.close();
		if(print){
			System.out.println("Translation Map");
			for(String k : translationMap.keySet()){
				System.out.println(k + " -> " + translationMap.get(k));
			}
		}
		return translationMap;
	}
	
	private static class JoinRelation{
		private String rel1;
		private String rel2;
		private String rel3;
		
		public JoinRelation(String rel1, String rel2, String rel3){
			this.rel1= rel1;
			this.rel2 = rel2;
			this.rel3 = rel3;
		}
		
		@Override
		public boolean equals(Object o){
			if(o == null) return false;
			if(o == this) return true;
			if(!(o instanceof JoinRelation)) return false;
			JoinRelation that = (JoinRelation)o;

			if(this.rel1.equals(that.rel1) && this.rel2.equals(that.rel2) && this.rel3.equals(that.rel3)){
				return true;
			}
			else{
				return false;
			}
			
		}
		
		public static boolean isRel1(String rel, Set<JoinRelation> joinRelations){
			for(JoinRelation jr : joinRelations){
				if(jr.rel1.equals(rel)){
					return true;
				}
			}
			return false;
		}
		
		public static boolean isRel2(String rel, Set<JoinRelation> joinRelations){
			for(JoinRelation jr : joinRelations){
				if(jr.rel2.equals(rel)){
					return true;
				}
			}
			return false;
		}
		
		public static JoinRelation getJoinRelation(String rel1, String rel2, Set<JoinRelation> joinRelations){
			for(JoinRelation jr: joinRelations){
				if(jr.rel1.equals(rel1) && jr.rel2.equals(rel2)){
					return jr;
				}
			}
			return null;
		}
		
		public String getRel3(){return rel3;}
		
		@Override
		public String toString(){
			return "JoinRelation: "+rel1 + "\t" + rel2 +"\t" +rel3;
		}
	}

}
