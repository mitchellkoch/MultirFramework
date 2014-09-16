package edu.washington.multir.distantsupervision;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.NELArgumentIdentification;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.corpus.SentNamedEntityLinkingInformation.NamedEntityLinkingAnnotation;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;
import edu.washington.multir.data.RelatedLocationMap;
import edu.washington.multir.sententialextraction.DocumentExtractor;
import edu.washington.multir.util.CLIUtils;
import edu.washington.multir.util.CorpusUtils;
import edu.washington.multir.util.FigerTypeUtils;
import edu.washington.multir.util.GuidMidConversion;
import edu.washington.multir.util.ModelUtils;
import edu.washington.multir.util.TypeConstraintUtils;

public class FeedbackNegativeDistantSupervision {
	
	private static long newMidCount =0;
	private static final String MID_BASE = "MID";
	private static Map<String,List<String>> idToAliasMap = null;
	private static boolean print = true;
	private static RelatedLocationMap rlm = null;
	
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ParseException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, SQLException, IOException{
		
		FigerTypeUtils.init();
		//load in k partitioned models and sigs
		List<String> arguments  = new ArrayList<String>();
		for(String arg: args){
			arguments.add(arg);
		}
		CorpusInformationSpecification cis = CLIUtils.loadCorpusInformationSpecification(arguments);
		FeatureGenerator fg = CLIUtils.loadFeatureGenerator(arguments);
		ArgumentIdentification ai = CLIUtils.loadArgumentIdentification(arguments);
		List<SententialInstanceGeneration> sigs = CLIUtils.loadSententialInstanceGenerationList(arguments);
		List<String> modelPaths = CLIUtils.loadFilePaths(arguments);
		List<String> outputDSFiles = CLIUtils.loadOutputFilePaths(arguments);
		
		if(!( (sigs.size() == modelPaths.size()) && (outputDSFiles.size() == modelPaths.size()))){
			throw new IllegalArgumentException("Size of inputDS, outputDS, modelPaths, and siglist must all be equal");
		}
		
		String corpusDatabase = arguments.get(0);
		String targetRelationPath = arguments.get(1);
		Corpus trainCorpus = new Corpus(corpusDatabase,cis,true);		
		KnowledgeBase kb = new KnowledgeBase(arguments.get(2),arguments.get(3),arguments.get(1));
		
		
		if(arguments.size() == 6){
			String corpusSetting = arguments.get(4);
			String pathToTestDocumentFile = arguments.get(5);
			
			if(!corpusSetting.equals("train") && !corpusSetting.equals("test")){
				throw new IllegalArgumentException("This argument must be train or test");
			}
			File f = new File(pathToTestDocumentFile);
			if(!f.exists() || !f.isFile()){
				throw new IllegalArgumentException("File at " + pathToTestDocumentFile + " does not exist or is not a file");
			}
			
			if(corpusSetting.equals("train")){
				trainCorpus.setCorpusToTrain(pathToTestDocumentFile);
			}
			else{
				trainCorpus.setCorpusToTest(pathToTestDocumentFile);
			}
		}

		run(kb,modelPaths,outputDSFiles,sigs,fg,ai,trainCorpus);

	}

	private static List<DSEntityPair> getEntityPairs(
			List<DS> relationSpecificDsList) {
		
		Map<String,List<DS>> idsToDSListMap = new HashMap<>();
		List<DSEntityPair> dsEntityPairs = new ArrayList<>();
		for(DS ds : relationSpecificDsList){
			
			String key = ds.arg1ID+":"+ds.arg2ID;
			if(idsToDSListMap.containsKey(key)){
				idsToDSListMap.get(key).add(ds);
			}
			else{
				List<DS> newDSList = new ArrayList<>();
				newDSList.add(ds);
				idsToDSListMap.put(key,newDSList);
			}
		}
		
		
		for(String key: idsToDSListMap.keySet()){
			List<DS> dsList = idsToDSListMap.get(key);
			String[] values = key.split(":");
			String arg1Id = values[0];
			String arg2Id = values[1];
			//prevent single instance entity pairs
			if(dsList.size()>1) dsEntityPairs.add(new DSEntityPair(arg1Id,arg2Id,dsList));
		}
		return dsEntityPairs;
	}
	
	
	public static void run(KnowledgeBase kb, List<String> modelPaths, List<String> outputDSFiles,
			List<SententialInstanceGeneration> sigs,
			FeatureGenerator fg, ArgumentIdentification ai, Corpus trainCorpus) throws SQLException, IOException{
		
		long start = System.currentTimeMillis();
		//create PartitionData Map from model name
		Map<String,PartitionData> modelDataMap = new HashMap<>();
		
		idToAliasMap = kb.getIDToAliasMap();
		rlm = RelatedLocationMap.getInstance();

		
		
		for(int i =0; i < modelPaths.size(); i++){
			String modelPath = modelPaths.get(i);
			SententialInstanceGeneration sig = sigs.get(i);
			DocumentExtractor de = new DocumentExtractor(modelPath, fg,ai,sig);
			Map<Integer,String> ftId2FtMap = ModelUtils.getFeatureIDToFeatureMap(de.getMapping());
			modelDataMap.put(modelPath,new PartitionData(sig,outputDSFiles.get(i),de,ftId2FtMap));
			
		}

		
		List<DS> candidateNegativeExamples = new ArrayList<>();
		//Set<Integer> relevantSentIds = new HashSet<>();
		//Run extractor over corpus, get new extractions
		Iterator<Annotation> di =trainCorpus.getDocumentIterator();
		int docCount =0;
		while(di.hasNext()){
			Annotation doc = di.next();
			List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
			for(CoreMap sentence : sentences){
				for(String modelPath: modelPaths){
					SententialInstanceGeneration sig = modelDataMap.get(modelPath).sig;
					DocumentExtractor de = modelDataMap.get(modelPath).de;
					List<DS> newNegativeAnnotations = modelDataMap.get(modelPath).newNegativeAnnotations;
					//argument identification
					List<Argument> sentenceArgs =  ai.identifyArguments(doc,sentence);
					//sentential instance generation
					List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(sentenceArgs, sentence);
					for(Pair<Argument,Argument> p : sententialInstances){
						Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> extrResult = 
						de.extractFromSententialInstanceWithAllFeatureScores(p.first, p.second, sentence, doc);
						if(extrResult != null){
							Integer sentNum = sentence.get(SentGlobalID.class);				
							Triple<String,Double,Double> extrScoreTriple = extrResult.first;
							String rel = extrScoreTriple.first;
							
							if(!rel.equals("NA")){
								String arg1Id = getArgId(doc,sentence,p.first);
								String arg2Id = getArgId(doc,sentence,p.second);
								if(arg1Id != null && arg2Id!= null){
									if(kb.participatesInRelationAsArg1(arg1Id, rel)){
									//if(isTrueNegative(kb,doc,sentence,p.first,p.second,rel,arg1Id,arg2Id)){
										DS ds =  new DS(p.first,p.second,arg1Id,arg2Id,sentNum,rel,modelPath);
										ds.score = extrScoreTriple.third;
										//relevantSentIds.add(sentNum);
										//newNegativeAnnotations.add(ds);
										candidateNegativeExamples.add(ds);
									//}
									}
								}
							}
						}
					}
				}
			}
			docCount++;
			if(docCount % 1000 == 0){
				System.out.println(docCount + " docs processed");
			}
		}
		System.out.println("Finished making Extractions");
		System.out.println("There are " + candidateNegativeExamples.size() + " negative candidate extractions");
		
		//get set of all argument tokens
		Set<String> argTokens = new HashSet<>();
		for(DS ds: candidateNegativeExamples){
			argTokens.addAll(getValidTokens(ds.arg1.getArgName()));
			argTokens.addAll(getValidTokens(ds.arg2.getArgName()));
			List<String> arg1Aliases = idToAliasMap.get(ds.arg1ID);
			if(arg1Aliases!=null){
				for(String alias: arg1Aliases){
					argTokens.addAll(getValidTokens(alias));
				}
			}
			List<String> arg2Aliases = idToAliasMap.get(ds.arg2ID);
			if(arg2Aliases!=null){
				for(String alias: arg2Aliases){
					argTokens.addAll(getValidTokens(alias));
				}
			}
		}
		System.out.println("Created set of all argument Tokens.");
		System.out.println("There are " + argTokens.size() + " unique argument tokens");
		
		
		System.out.println("Creating map from arg tokens to candidate entity ids....");
		//make map from argument tokens to list of candidate entities
		Map<String,Set<String>> tokenCandidateMap = new HashMap<>();
		int idCount =0;
		for(String idKey : idToAliasMap.keySet()){
			List<String> aliases = idToAliasMap.get(idKey);
			for(String alias: aliases){
				for(String token : argTokens){
					for(String aliasToken: alias.split("\\s+")){
						if(aliasToken.equals(token)){
							if(tokenCandidateMap.containsKey(token)){
								tokenCandidateMap.get(token).add(idKey);
							}
							else{
								Set<String> candidateIds = new HashSet<>();
								candidateIds.add(idKey);
								tokenCandidateMap.put(token, candidateIds);
							}
						}
					}
				}
			}
			idCount++;
			if(idCount % 10000 == 0){
				System.out.println(idCount + " entity ids processed out of " + idToAliasMap.keySet().size());
			}
			
		}
		System.out.println("Finished constructing map from arg keys to entity ids");
		
//		List<Integer> relevantSentIdList = new ArrayList<>(relevantSentIds);
//		List<Set<Integer>> subSentIdSets = new ArrayList<>();
//		for(int i =0; i < relevantSentIdList.size(); i+=100){
//			subSentIdSets.add(new HashSet<>(relevantSentIdList.subList(i, Math.min(relevantSentIdList.size(),i+100))));
//		}
//		Map<Integer,Pair<CoreMap,Annotation>> corpusData = new HashMap<>();
//		for(Set<Integer> subSet : subSentIdSets){
//			corpusData.putAll(trainCorpus.getAnnotationPairsForEachSentence(subSet));
//		}
//		System.out.println("Got map from sentence id to annotation objects");
		
		//iterate over candidate negative examples and text for being true negatives
		for(DS ds: candidateNegativeExamples){
			try{
				Set<Integer> sentIds = new HashSet<>();
				sentIds.add(ds.sentNum);
				Map<Integer,Pair<CoreMap,Annotation>> m = trainCorpus.getAnnotationPairsForEachSentence(sentIds);
				if(m.size() == 1){
					if(isTrueNegative(kb,m.get(ds.sentNum).second,m.get(ds.sentNum).first,ds.arg1,ds.arg2,ds.relation,ds.arg1ID,ds.arg2ID,tokenCandidateMap)){
						modelDataMap.get(ds.modelPath).newNegativeAnnotations.add(ds);
					}
				}
			}
			catch(Exception e){
				System.err.print(e.getMessage());
			}

		}
		

		FigerTypeUtils.close();

		
		//output new negatives into new DS files
		for(String modelPath: modelPaths){
			String outputFileName = modelDataMap.get(modelPath).dsOutputPath;
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFileName)));
			
			
			List<List<DS>> relationSpecificNegativeInstances = new ArrayList<>();
			List<DS> allNegativeExtractions = modelDataMap.get(modelPath).newNegativeAnnotations;
			for(DS ds : allNegativeExtractions){
				
				int foundIndex = -1;
				for(int i =0 ; i < relationSpecificNegativeInstances.size(); i++){
					List<DS> lds = relationSpecificNegativeInstances.get(i);
					DS ds1 = lds.get(0);
					if(ds.relation.equals(ds1.relation)){
						foundIndex = i;
					}
				}
				if(foundIndex == -1){
					List<DS> newDsList = new ArrayList<>();
					newDsList.add(ds);
					relationSpecificNegativeInstances.add(newDsList);
				}
				else{
					List<DS> relationSpecificDsList = relationSpecificNegativeInstances.get(foundIndex);
					relationSpecificDsList.add(ds);
				}
			}
			
			for(List<DS> relationSpecificDsList : relationSpecificNegativeInstances ){
				Collections.sort(relationSpecificDsList, new Comparator<DS>(){

					@Override
					public int compare(DS arg0, DS arg1) {
						if(arg0.score > arg1.score){
							return -1;
						}
						else if(arg0.score < arg1.score){
							return 1;
						}
						else{
							return 0;
						}
					}
					
				});				
				List<DS> topNegativeExtractions = relationSpecificDsList.subList(0, Math.max(0,(int)Math.ceil((double)relationSpecificDsList.size())));
				for(DS ds : topNegativeExtractions){
					ds.relation = "NA";
					bw.write(getDSString(ds)+"\n");
				}		
			}
			
			
			//make  a List<List<DS>> for each separate positive relation
			List<List<DS>> relationSpecificPositiveInstances = new ArrayList<>();
			List<DS> allPositiveExtractions = modelDataMap.get(modelPath).newPositiveAnnotations;
			

			for(DS ds : allPositiveExtractions){
				
				int foundIndex = -1;
				for(int i =0 ; i < relationSpecificPositiveInstances.size(); i++){
					List<DS> lds = relationSpecificPositiveInstances.get(i);
					DS ds1 = lds.get(0);
					if(ds.relation.equals(ds1.relation)){
						foundIndex = i;
					}
				}
				if(foundIndex == -1){
					List<DS> newDsList = new ArrayList<>();
					newDsList.add(ds);
					relationSpecificPositiveInstances.add(newDsList);
				}
				else{
					List<DS> relationSpecificDsList = relationSpecificPositiveInstances.get(foundIndex);
					relationSpecificDsList.add(ds);
				}
			}
			
			for(List<DS> relationSpecificDsList : relationSpecificPositiveInstances ){

				collapseArgumentPairs(relationSpecificDsList);
				List<DSEntityPair> dsEntityPairs = getEntityPairs(relationSpecificDsList);
				//group argument pairs by entity ids, and take top 10% of those mention pairs by average score
				
				Collections.sort(dsEntityPairs, new Comparator<DSEntityPair>(){

					@Override
					public int compare(DSEntityPair arg0, DSEntityPair arg1) {
						if(arg0.avgScore > arg1.avgScore){
							return -1;
						}
						else if(arg0.avgScore < arg1.avgScore){
							return 1;
						}
						else{
							return 0;
						}
					}
					
				});
				
				List<DSEntityPair> topPositiveExtractions = dsEntityPairs.subList(0, Math.max(0,(int)Math.ceil((double)dsEntityPairs.size()/10.0)));
				for(DSEntityPair dsEntityPair : topPositiveExtractions){
					for(DS ds : dsEntityPair.mentions){
					  bw.write(getDSString(ds)+"\n");
					}
				}		
			}
			bw.close();
		}
		
		
		long end = System.currentTimeMillis();
		System.out.println("time took  = " + (end -start));

	}

	private static void collapseArgumentPairs(List<DS> topPositiveExtractions) {

		//create map from argument pair names to pairs of ids
		Map<Pair<String,String>,Pair<String,String>> nameIdMap = new HashMap<>();
		
		
		//find valid mid pairs and put in map
		for(DS ds : topPositiveExtractions){
			String arg1Name = ds.arg1.getArgName();
			String arg2Name = ds.arg2.getArgName();
			Pair<String,String> idPair = new Pair<String,String>(ds.arg1ID,ds.arg2ID);
			if((!idPair.first.startsWith("MID")) && (!idPair.second.startsWith("MID"))){
				Pair<String,String> namePair = new Pair<>(arg1Name,arg2Name);
				if(!nameIdMap.containsKey(namePair)){
					nameIdMap.put(namePair,idPair);
				}
			}
		}
		
		//find arg pairs without valid ids in map and make them up
		for(DS ds : topPositiveExtractions){
			String arg1Name = ds.arg1.getArgName();
			String arg2Name = ds.arg2.getArgName();
			Pair<String,String> idPair = new Pair<String,String>(ds.arg1ID,ds.arg2ID);
			Pair<String,String> namePair = new Pair<>(arg1Name,arg2Name);
		    if(!nameIdMap.containsKey(namePair)){
					nameIdMap.put(namePair,idPair);
			}
		}
		
		//assign all idential name pairs the idential id pair
		for(DS ds : topPositiveExtractions){
			String arg1Name = ds.arg1.getArgName();
			String arg2Name = ds.arg2.getArgName();
			Pair<String,String> namePair = new Pair<>(arg1Name,arg2Name);
			Pair<String,String> idPair = nameIdMap.get(namePair);
			ds.arg1ID = idPair.first;
			ds.arg2ID = idPair.second;
		}
		
	}

	private static boolean isProbablyNegative(KnowledgeBase kb, Argument first,
			Argument second, String arg1Id, String arg2Id, String rel, CoreMap sentence,
			Map<String,Set<String>> tokenCandidateMap) {
		
		
		
//		if(typesDoNotMatch(kb,arg1Id,second,rel,sentence)) {
//			if(print) System.out.println("Returning false types for " + arg1Id + " do not match type of " + second.getArgName());
//			return false;
//		}
		
		
		List<String> arg1Ids = getCandidates(kb,first,arg1Id,rel,sentence,tokenCandidateMap); 
		if(arg1Ids.size() == 0) {
			if(print) System.out.println("Arg1 has no candidates " +first.getArgName() + " " + arg1Id);
			return false;
		}
		
		
		List<String> arg2Ids = getCandidates(kb,second,arg2Id,sentence,tokenCandidateMap);
		if(arg2Ids.size() == 0) {
			if(print) System.out.println("Arg2 has no candidates " +second.getArgName() + " " + arg2Id);
			return false;
		}
		
		if(print){
			System.out.println("Candidate arg1IDS:");
			for(String a1Id: arg1Ids){
				System.out.print(a1Id + " ");
			}
			System.out.println();
			System.out.println("Candidate arg2IDS:");
			for(String a2Id: arg2Ids){
				System.out.print(a2Id + " ");
			}
			System.out.println();
		}
		for(String a1Id : arg1Ids){
				for(String a2Id: arg2Ids){
					List<String> relations = kb.getRelationsBetweenArgumentIds(a1Id,a2Id);
					if(relations.size()>0){
						if(print) System.out.println("Comparing ids " + a1Id + " and " + a2Id);
						if(print) System.out.println("Returning false because candidate pair " + a1Id + " " + a2Id + " has relation");
						return false;
					}
					if(stringsSimilar(kb,a1Id,second.getArgName(),rel)) {
						if(print) System.out.println("Comparing ids " + a1Id + " and " + a2Id);
						if(print) System.out.println("Returning false becasue " + a1Id + " has a similar string to " + second.getArgName());
						return false;
					}
				}
		}
		return true;
		
	}



	private static List<String> getCandidates(KnowledgeBase kb,
			Argument arg, String argId, CoreMap sentence,
			Map<String, Set<String>> tokenCandidateMap) {
		
		Set<String> validTokens = new HashSet<>();
		Set<String> entityIds = new HashSet<>();
		validTokens.addAll(getValidTokens(arg.getArgName()));
		
		List<String> aliases = idToAliasMap.get(argId);
		if(aliases == null){
			return new ArrayList<>();
		}
		for(String alias: aliases){
			validTokens.addAll(getValidTokens(alias));
		}
		
		for(String tok: validTokens){
			Set<String> candidateIds = tokenCandidateMap.get(tok);
			if(candidateIds != null){
				entityIds.addAll(candidateIds);
			}
		}
		entityIds.add(argId);
		
		if(TypeConstraintUtils.getNERType(arg,sentence.get(CoreAnnotations.TokensAnnotation.class)).equals("LOCATION")){
			entityIds.addAll(getRelatedLocations(argId));
		}
		
		return new ArrayList<>(entityIds);
	}

	private static List<String> getCandidates(KnowledgeBase kb, Argument first,
			String arg1Id, String rel, CoreMap sentence,
			Map<String, Set<String>> tokenCandidateMap) {
		if(kb.participatesInRelationAsArg1(arg1Id, rel)){
			Set<String> entityIds = new HashSet<>();
			Set<String> validTokens = new HashSet<>();
			validTokens.addAll(getValidTokens(first.getArgName()));
			
			List<String> aliases = idToAliasMap.get(arg1Id);
			if(aliases == null){
				return new ArrayList<>();
			}
			for(String alias: aliases){
				validTokens.addAll(getValidTokens(alias));
			}
			
			for(String tok: validTokens){
				Set<String> candidateIds = tokenCandidateMap.get(tok);
				if(candidateIds != null){
					entityIds.addAll(candidateIds);
				}
			}
			entityIds.add(arg1Id);
			
			if(TypeConstraintUtils.getNERType(first,sentence.get(CoreAnnotations.TokensAnnotation.class)).equals("LOCATION")){
				entityIds.addAll(getRelatedLocations(arg1Id));
			}
			
			if(entityIds.contains(arg1Id)) {
				return new ArrayList<>(entityIds);
			}
		}
		return new ArrayList<>();
	}

	private static List<String> getCandidates(KnowledgeBase kb, Argument first,
			String arg1Id, String rel, CoreMap sentence) {
		Set<String> validTokens = new HashSet<>();
		Set<String> entityIds = new HashSet<>();
		validTokens.addAll(getValidTokens(first.getArgName()));
		
		List<String> aliases = idToAliasMap.get(arg1Id);
		if(aliases == null){
			return new ArrayList<>();
		}
		for(String alias: aliases){
			validTokens.addAll(getValidTokens(alias));
		}
		
		if(kb.participatesInRelationAsArg1(arg1Id, rel)){
			entityIds.add(arg1Id);
			for(String k : idToAliasMap.keySet()){
				aliases = idToAliasMap.get(k);
				for(String alias: aliases){
					for(String token : validTokens){
						for(String aliasToken: alias.split("\\s+")){
							if(aliasToken.equals(token)){
								entityIds.add(k);
							}
						}
					}
				}
			}
		}
		
		if(TypeConstraintUtils.getNERType(first,sentence.get(CoreAnnotations.TokensAnnotation.class)).equals("LOCATION")){
			entityIds.addAll(getRelatedLocations(arg1Id));
		}
		
		if(entityIds.contains(arg1Id)) {
			return new ArrayList<>(entityIds);
		}
		else{
			return new ArrayList<>();
		}
	}

	private static Set<String> getRelatedLocations(
			String argId) {
		return 	rlm.getRelatedLocations(argId);
	}

	private static List<String> getCandidates(KnowledgeBase kb, Argument first,
			String arg1Id, CoreMap sentence) {
		
		Set<String> validTokens = new HashSet<>();
		Set<String> entityIds = new HashSet<>();
		validTokens.addAll(getValidTokens(first.getArgName()));
		
		List<String> aliases = idToAliasMap.get(arg1Id);
		if(aliases == null){
			return new ArrayList<>();
		}
		for(String alias: aliases){
			validTokens.addAll(getValidTokens(alias));
		}
		
		entityIds.add(arg1Id);
		for(String k : idToAliasMap.keySet()){
			aliases = idToAliasMap.get(k);
			for(String alias: aliases){
				for(String token : validTokens){
					for(String aliasToken: alias.split("\\s+")){
						if(aliasToken.equals(token)){
							entityIds.add(k);
						}
					}
				}
			}
		}
		if(TypeConstraintUtils.getNERType(first,sentence.get(CoreAnnotations.TokensAnnotation.class)).equals("LOCATION")){
			entityIds.addAll(getRelatedLocations(arg1Id));
		}
		
		return new ArrayList<>(entityIds);
	}
	
	private static List<String> getValidTokens (String str){
		List<String> validTokens = new ArrayList<>();
		String [] argTokens = str.split("\\s+");
		for(String argToken : argTokens){
			if(isUpper(argToken.substring(0, 1))){
				validTokens.add(argToken);
			}
		}
		return validTokens;
	}

	private static boolean isUpper(String str) {
		if(!str.toLowerCase().equals(str)) return true;
		return false;
	}

	//if notable type matches for any a1Id rel a2 with second argument
	private static boolean typesDoNotMatch(KnowledgeBase kb, String a1Id, Argument second, String rel, CoreMap sentence) {
	
		List<Pair<String,String>> rels = kb.getEntityPairRelationMap().get(a1Id);
		List<Pair<String,String>> targetRels = new ArrayList<>();
		
		if(rels != null){
			Set<String> kbFreebaseTypes = new HashSet<>();
			for(Pair<String,String> p : rels){
				if(p.first.equals(rel)){
					targetRels.add(p);
				}
			}
			if(targetRels.size()>0){
				List<String> arg2Ids = new ArrayList<>();
				for(Pair<String,String> p : targetRels){
					arg2Ids.add(p.second);
				}
				
				for(String arg2Id: arg2Ids){
					kbFreebaseTypes.addAll(FigerTypeUtils.getFreebaseTypesFromID(GuidMidConversion.convertBackward(arg2Id)));
				}
				
				String fbType = CorpusUtils.getFreebaseNotableType(second.getStartOffset(), second.getEndOffset(), sentence);
				if(fbType != null){
					if(!kbFreebaseTypes.contains(fbType)){
						if(print) System.out.println("Type " + fbType + " from " + second.getArgName() + " not in " + a1Id + " relevant types");
						return true;
					}
				}
			}

		}
		
		
		
		return false;
	}

	private static boolean stringsSimilar(KnowledgeBase kb,String a1Id, String argName,
			String rel) {
				
		//if(print)System.out.println(a1Id + " " + argName);
		
		List<Pair<String,String>> rels = kb.getEntityPairRelationMap().get(a1Id);
		List<Pair<String,String>> targetRels = new ArrayList<>();
		
		if(rels != null){
			for(Pair<String,String> p : rels){
				if(p.first.equals(rel)){
					targetRels.add(p);
				}
			}
			
			List<String> arg2Ids = new ArrayList<>();
			for(Pair<String,String> p : targetRels){
				arg2Ids.add(p.second);
			}
			
			List<String> arg2Strings = new ArrayList<>();
			for(String id : arg2Ids){
				List<String> aliases = idToAliasMap.get(id);
				for(String alias: aliases){
					if(!arg2Strings.contains(alias)){
						arg2Strings.add(alias);
					}
				}
			}
			
			//if any arg2String in kb contains any token in relation string arg2 then strings are similar
			
			String[] argTokens = argName.split("\\s+");
			for(String arg2String : arg2Strings){
				for(String argToken: argTokens){
					if(arg2String.contains(argToken)){
						if(print)System.out.println(arg2String + " contains " + argToken);
						return true;
					}
				}
			}
			
			//if any token in arg2String in kb is similar enough to any token in arg2 then strings are similar
			for(String arg2String: arg2Strings){
				for(String arg2Token: arg2String.split("\\s+")){
					if(arg2Token.length() >2){
						for(String argToken: argTokens){
							if(argToken.length()>2){
								Integer maxLength = Math.max(arg2Token.length(), argToken.length());
								Integer editDistance = StringUtils.getLevenshteinDistance(argToken, arg2Token);
								Double ratio = (double)editDistance/(double)maxLength;
								//strings are too similar
								if(ratio <= .33){
									if(print)System.out.println(argToken + " is too similar to  " + arg2Token + " with edit score of " + ratio);
									return true;
								}
							}
						}
					}
				}
			}
		}
		
		//if(print) System.out.println("Arguments are not similar enough");
		return false;
	}

	private static String getWeakMid(CoreMap s, Argument arg) {
		List<Triple<Pair<Integer,Integer>,String,Float>> nelAnnotation = s.get(NamedEntityLinkingAnnotation.class);
		List<CoreLabel> tokens = s.get(CoreAnnotations.TokensAnnotation.class);
		if(nelAnnotation != null){
			for(Triple<Pair<Integer,Integer>,String,Float> trip : nelAnnotation){
				String id = trip.second;
				Float conf = trip.third;
				//if token span has a link create a new argument
				if(!id.equals("null")){
					//get character offsets
					Integer startTokenOffset = trip.first.first;
					Integer endTokenOffset = trip.first.second;
					if(startTokenOffset >= 0 && startTokenOffset < tokens.size() && endTokenOffset >= 0 && endTokenOffset < tokens.size()){
						Integer startCharacterOffset = tokens.get(startTokenOffset).get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
						Integer endCharacterOffset = tokens.get(endTokenOffset-1).get(SentenceRelativeCharacterOffsetEndAnnotation.class);
							
						//get argument string
						String sentText = s.get(CoreAnnotations.TextAnnotation.class);
						if(sentText != null && startCharacterOffset !=null && endCharacterOffset!=null){
							String argumentString = sentText.substring(startCharacterOffset, endCharacterOffset);
								
							//add argument to list
							KBArgument nelArgument = new KBArgument(new Argument(argumentString,startCharacterOffset,endCharacterOffset),id);
							if(arg.isContainedIn(nelArgument) || arg.equals(nelArgument)){
								return nelArgument.getKbId();
							}
						}
					}
				}
			}
		}
		return getNextMid();
	}


	private static String getArgId(Annotation doc, CoreMap sentence,
			Argument arg) {
		String argLink = null;
		List<Argument> nelArgs = NELArgumentIdentification.getInstance().identifyArguments(doc, sentence);
		for(Argument nelArg: nelArgs){
			KBArgument kbNelArg = (KBArgument)nelArg;
			if( arg.equals(nelArg) || (arg.isContainedIn(nelArg) && getLink(sentence,arg).equals(kbNelArg.getKbId()))){
				argLink = kbNelArg.getKbId();
			}
		}
		return argLink;
	}


	private static String getLink(CoreMap sentence, Argument arg) {
		List<Triple<Pair<Integer,Integer>,String,Float>> nelData = sentence.get(NamedEntityLinkingAnnotation.class);
		for(Triple<Pair<Integer,Integer>,String,Float> t : nelData){
			Pair<Integer,Integer> nelTokenOffset = t.first;
			Pair<Integer,Integer> argTokenOffset = CorpusUtils.getTokenOffsetsFromCharacterOffsets(arg.getStartOffset(), arg.getEndOffset(), sentence);
			if(nelTokenOffset.equals(argTokenOffset)){
				return t.second;
			}
		}
		return "null";
	}

	private static boolean isTrueNegative(KnowledgeBase KB, Annotation doc, CoreMap sentence, Argument arg1, Argument arg2, 
			String extractionRel, String arg1Id, String arg2Id,
			Map<String,Set<String>> tokenCandidateMap) {
		if(print){
			System.out.println("arg1  = " + arg1.getArgName());
			System.out.println("link1 = " + arg1Id);
			System.out.println("arg2  = " + arg2.getArgName());
			System.out.println("link2 = " + arg2Id);
			System.out.println("SentNum = " + sentence.get(SentGlobalID.class));
			System.out.println("Arg1NER = " + TypeConstraintUtils.getNERType(arg1,sentence.get(CoreAnnotations.TokensAnnotation.class)));
			System.out.println("Arg2NER = " + TypeConstraintUtils.getNERType(arg2,sentence.get(CoreAnnotations.TokensAnnotation.class)));
			System.out.println("REL = " + extractionRel);

			
		}


		if(isPromininent(arg1Id)){
			if(isProbablyNegative(KB,arg1,arg2,arg1Id,arg2Id,extractionRel,sentence,tokenCandidateMap)){
				if(print) System.out.println("Arg pair is probably negative");
				return true;
			}
		}
		
		return false;
	}


	private static boolean isPromininent(String arg1Id) {
		return true;
	}

	//creates a fake mid, used for new negative examples from feedback
	private static String getNextMid() {
		return MID_BASE + newMidCount++;
	}


	private static String getDSString(DS ds) {
		StringBuilder sb = new StringBuilder();
		sb.append(ds.arg1ID);
		sb.append("\t");
		sb.append(ds.arg1.getStartOffset());
		sb.append("\t");
		sb.append(ds.arg1.getEndOffset());
		sb.append("\t");
		sb.append(ds.arg1.getArgName());
		sb.append("\t");
		sb.append(ds.arg2ID);
		sb.append("\t");
		sb.append(ds.arg2.getStartOffset());
		sb.append("\t");
		sb.append(ds.arg2.getEndOffset());
		sb.append("\t");
		sb.append(ds.arg2.getArgName());
		sb.append("\t");
		sb.append(ds.sentNum);
		sb.append("\t");
		sb.append(ds.relation);
		return sb.toString().trim();
	}
	
	private static class DS{
		private Argument arg1;
		private Argument arg2;
		private String arg1ID;
		private String arg2ID;
		private Integer sentNum;
		private String relation;
		private Double score;
		private String modelPath;
		
		public DS(Argument arg1, Argument arg2, String arg1ID, String arg2ID, Integer sentNum,String relation,String modelPath){
			this.arg1=arg1;
			this.arg2=arg2;
			this.arg1ID=arg1ID;
			this.arg2ID=arg2ID;
			this.sentNum=sentNum;
			this.relation=relation;
			this.modelPath = modelPath;
		}
		
		@Override
		public boolean equals(Object other){
			DS ds = (DS)other;
			if((arg1.equals(ds.arg1)) &&
				(arg2.equals(ds.arg2)) &&
				(sentNum.equals(ds.sentNum))){
				return true;
			}
			else{
				return false;
			}
		}
		
		@Override
		public int hashCode(){
			return new HashCodeBuilder(37,41).append(arg1).
					append(arg2)
					.append(sentNum).toHashCode();
		}
		
		
	}
	
	private static class DSEntityPair{
		private String arg1Id;
		private String arg2Id;
		private List<DS> mentions;
		private double avgScore;
		
		
		public DSEntityPair(String arg1Id, String arg2Id, List<DS> mentions){
			this.arg1Id = arg1Id;
			this.arg2Id = arg2Id;
			this.mentions = mentions;
			
			double sum = 0.0;
			for(DS mention: mentions){
				sum += mention.score;
			}
			avgScore = (sum/mentions.size());
		}
	}


	private static class PartitionData{
		private SententialInstanceGeneration sig;
		private String dsOutputPath;
		private DocumentExtractor de;
		private List<DS> newNegativeAnnotations;
		private List<DS> newPositiveAnnotations;
		private Map<Integer,String> ftID2ftMap;
		
		
		public PartitionData(SententialInstanceGeneration sig, String dsOutputPath, DocumentExtractor de, Map<Integer,String> ftID2ftMap){
			this.sig = sig;
			this.dsOutputPath = dsOutputPath;
			this.de = de;
			this.ftID2ftMap = ftID2ftMap;
			newNegativeAnnotations = new ArrayList<>();
			newPositiveAnnotations = new ArrayList<>();

		}
	}
	

}
