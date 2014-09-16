package edu.washington.multir.distantsupervision;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.time.SUTime;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.data.TypeSignatureRelationMap;
import edu.washington.multir.util.CorpusUtils;
import edu.washington.multir.util.TypeConstraintUtils;
import edu.washington.multir.util.TypeConstraintUtils.GeneralType;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignatureORGDATESententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignatureORGLOCSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignatureORGNUMSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignatureORGORGSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignatureORGOTHERSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignatureORGPERSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePERDATESententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePERLOCSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePERNUMSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePERORGSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePEROTHERSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePERPERSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.KBPRelationMatching;
import edu.washington.multirframework.argumentidentification.KBP_NELAndNERArgumentIdentification;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multirframework.corpus.SentInformationI;
import edu.washington.multirframework.corpus.SentNamedEntityLinkingInformation;
import edu.washington.multirframework.corpus.SentOffsetInformation.SentStartOffset;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.data.NegativeAnnotation;
import edu.washington.multirframework.distantsupervision.DistantSupervision;
import edu.washington.multirframework.distantsupervision.NegativeExampleCollection;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;
import edu.washington.multirframework.util.BufferedIOUtils;

public class KBPDistantSupervision {
	private List<SententialInstanceGeneration> sigList;
	private List<String> outputPaths;
	private ArgumentIdentification ai;
	private RelationMatching rm;
	private NegativeExampleCollection nec;
	private List<PrintWriter> writers;
	private static Pattern documentDatePattern = Pattern.compile("_(\\d\\d\\d\\d\\d\\d\\d\\d)");
	private static AnnotationPipeline suTimePipeline = new AnnotationPipeline();
	private static Properties properties = new Properties();
	static{
		suTimePipeline.addAnnotator(new TimeAnnotator("sutime",properties));
	}

	
	public static void main(String[] args) throws SQLException, IOException{
		
		ArgumentIdentification ai = KBP_NELAndNERArgumentIdentification.getInstance();
		List<String> outputPaths = new ArrayList<>();
		List<SententialInstanceGeneration> sigList = new ArrayList<>();
		RelationMatching rm = KBPRelationMatching.getInstance();
		NegativeExampleCollection nec = NegativeExampleCollectionByRatio.getInstance(1.0);
		KnowledgeBase kb = new KnowledgeBase("/homes/gws/jgilme1/KBPMultir/NewKnowledgeBase/FromFBDump/kbpMultirKB-Full.tsv",
				"/homes/gws/jgilme1/KBPMultir/NewKnowledgeBase/FromFBDump/entities.tsv","/homes/gws/jgilme1/KBPMultir/NewKnowledgeBase/kbp-multir-relations");
		CustomCorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
		List<SentInformationI> sentInformationList = new ArrayList<>();
		sentInformationList.add(new SentNamedEntityLinkingInformation());
		cis.addSentenceInformation(sentInformationList);
		Corpus c = new Corpus("/scratch2/code/multir-reimplementation/MultirExtractor/FullCorpus-UIUCNotableTypes",cis,true);
		TypeSignatureRelationMap.init("/homes/gws/jgilme1/KBPMultir/DistantSupervision/partition-relation-map");

		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/PERPER-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/PERDATE-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/PERNUM-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/PERLOC-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/PEROTHER-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/PERORG-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/ORGORG-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/ORGOTHER-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/ORGPER-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/ORGNUM-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/ORGDATE-DS");
		outputPaths.add("/homes/gws/jgilme1/KBPMultir/DistantSupervision/FullCorpus/ORGLOC-DS");

		sigList.add(FigerAndNERTypeSignaturePERPERSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignaturePERDATESententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignaturePERNUMSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignaturePERLOCSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignaturePEROTHERSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignaturePERORGSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignatureORGORGSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignatureORGOTHERSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignatureORGPERSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignatureORGNUMSententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignatureORGDATESententialInstanceGeneration.getInstance());
		sigList.add(FigerAndNERTypeSignatureORGLOCSententialInstanceGeneration.getInstance());

		KBPDistantSupervision ds = new KBPDistantSupervision(ai,outputPaths,sigList,rm,nec);
		ds.run(kb,c);
	}
	
	public KBPDistantSupervision(ArgumentIdentification ai, List<String> outputPaths, List<SententialInstanceGeneration> sigList, 
			RelationMatching rm, NegativeExampleCollection nec){
		this.sigList = sigList;
		this.ai = ai;
		this.rm =rm;
		this.nec = nec;
		this.outputPaths=outputPaths;
		if(outputPaths.size()!=sigList.size()){
			throw new IllegalArgumentException("Number of SentenceInstanceGeneration specifications must equal number of output paths");
		}
	}

	
	public void run(KnowledgeBase kb,  Corpus c) throws SQLException, IOException{
    	long start = System.currentTimeMillis();
    	
    	writers = new ArrayList<PrintWriter>();
    	for(int j =0; j < outputPaths.size(); j++){
    	  writers.add(new PrintWriter(BufferedIOUtils.getBufferedWriter(new File(outputPaths.get(j)))));
    	}
		Iterator<Annotation> di = c.getDocumentIterator();
		int count =0;
		long startms = System.currentTimeMillis();
		while(di.hasNext()){
			Annotation d = di.next();
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			List<List<Argument>> argumentList = new ArrayList<>();
			for(CoreMap sentence : sentences){
			  argumentList.add(ai.identifyArguments(d, sentence));
			}
			for(int j =0; j < sigList.size(); j++){
				SententialInstanceGeneration sig = sigList.get(j);
		    	PrintWriter dsWriter = writers.get(j);
				List<NegativeAnnotation> documentNegativeExamples = new ArrayList<>();
				List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> documentPositiveExamples = new ArrayList<>();
				int sentIndex = 0;
				for(CoreMap sentence : sentences){
					int sentGlobalID = sentence.get(SentGlobalID.class);
					

									
					//argument identification
					List<Argument> arguments =  argumentList.get(sentIndex);
					//sentential instance generation
					List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sentence);
				
					List<Triple<KBArgument,KBArgument,String>> distantSupervisionAnnotations 
					  = rm.matchRelations(sententialInstances,kb,sentence,d);
				
					
					//adding sentence IDs
					List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> dsAnnotationWithSentIDs = new ArrayList<>();
					for(Triple<KBArgument,KBArgument,String> trip : distantSupervisionAnnotations){
						Integer i = new Integer(sentGlobalID);
						Pair<Triple<KBArgument,KBArgument,String>,Integer> p = new Pair<>(trip,i);
						dsAnnotationWithSentIDs.add(p);
					}
					
					//negative example annotations
					List<NegativeAnnotation> negativeExampleAnnotations = null;
					negativeExampleAnnotations =
							  findNegativeExampleAnnotations(sententialInstances,distantSupervisionAnnotations,
									  kb,sentGlobalID, sentence, d);
					
					documentNegativeExamples.addAll(negativeExampleAnnotations);
					documentPositiveExamples.addAll(dsAnnotationWithSentIDs);
					
					sentIndex++;
					
				}
				DistantSupervision.writeDistantSupervisionAnnotations(documentPositiveExamples,dsWriter);
				DistantSupervision.writeNegativeExampleAnnotations(nec.filter(documentNegativeExamples,documentPositiveExamples,kb,sentences),dsWriter);
			}
			count++;
			if( count % 1000 == 0){
				long endms = System.currentTimeMillis();
				System.out.println(count + " documents processed");
				System.out.println("Time took = " + (endms-startms));
			}
		}
		
		for(int j =0; j < writers.size(); j++){
			writers.get(j).close();
		}
    	long end = System.currentTimeMillis();
    	System.out.println("Distant Supervision took " + (end-start) + " millisseconds");
	}
	
	public void run1(KnowledgeBase kb, DateMap dm, Corpus c) throws SQLException, IOException{
    	long start = System.currentTimeMillis();
    	
    	writers = new ArrayList<PrintWriter>();
    	for(int j =0; j < outputPaths.size(); j++){
    	  writers.add(new PrintWriter(BufferedIOUtils.getBufferedWriter(new File(outputPaths.get(j)))));
    	}
		Iterator<Annotation> di = c.getDocumentIterator();
		int count =0;
		long startms = System.currentTimeMillis();
		while(di.hasNext()){
			Annotation d = di.next();
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			List<List<Argument>> argumentList = new ArrayList<>();
			for(CoreMap sentence : sentences){
			  argumentList.add(ai.identifyArguments(d, sentence));
			}
			for(int j =0; j < sigList.size(); j++){
				SententialInstanceGeneration sig = sigList.get(j);
		    	PrintWriter dsWriter = writers.get(j);
				List<NegativeAnnotation> documentNegativeExamples = new ArrayList<>();
				List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> documentPositiveExamples = new ArrayList<>();
				int sentIndex = 0;
				for(CoreMap sentence : sentences){
					int sentGlobalID = sentence.get(SentGlobalID.class);
					

									
					//argument identification
					List<Argument> arguments =  argumentList.get(sentIndex);
					//sentential instance generation
					List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sentence);
					if(sententialInstances.size() > 0){
						List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
						String arg1Type = TypeConstraintUtils.translateNERTypeToTypeString(TypeConstraintUtils.getNERType(sententialInstances.get(0).first, tokens));
						String arg2Type = TypeConstraintUtils.translateNERTypeToTypeString(TypeConstraintUtils.getNERType(sententialInstances.get(0).second,tokens));	
						
						List<Triple<KBArgument,KBArgument,String>> distantSupervisionAnnotations = new ArrayList<>();
						//handle DATE
						if(arg2Type.equals(GeneralType.DATE)){
							distantSupervisionAnnotations = getDateRelations(sententialInstances,kb,dm,sentence,d);
							if(distantSupervisionAnnotations.size() > 0 ){
								for(Triple<KBArgument,KBArgument,String> dsAnno : distantSupervisionAnnotations){
								}
							}
						}
						
						//handle NUM
						
						//handle WEBSITE
						
						//relation matching
						else{
						distantSupervisionAnnotations = rm.matchRelations(sententialInstances,kb,sentence,d);
						}
						
						//adding sentence IDs
						List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> dsAnnotationWithSentIDs = new ArrayList<>();
						for(Triple<KBArgument,KBArgument,String> trip : distantSupervisionAnnotations){
							Integer i = new Integer(sentGlobalID);
							Pair<Triple<KBArgument,KBArgument,String>,Integer> p = new Pair<>(trip,i);
							dsAnnotationWithSentIDs.add(p);
						}
						//negative example annotations
						List<NegativeAnnotation> negativeExampleAnnotations = null;
						negativeExampleAnnotations =
								  findNegativeExampleAnnotations(sententialInstances,distantSupervisionAnnotations,
										  kb,sentGlobalID, sentence, d);
						
						documentNegativeExamples.addAll(negativeExampleAnnotations);
						documentPositiveExamples.addAll(dsAnnotationWithSentIDs);
					}
					sentIndex++;
					
				}
				DistantSupervision.writeDistantSupervisionAnnotations(documentPositiveExamples,dsWriter);
				DistantSupervision.writeNegativeExampleAnnotations(nec.filter(documentNegativeExamples,documentPositiveExamples,kb,sentences),dsWriter);
			}
			count++;
			if( count % 1000 == 0){
				long endms = System.currentTimeMillis();
				System.out.println(count + " documents processed");
				System.out.println("Time took = " + (endms-startms));
			}
		}
		
		for(int j =0; j < writers.size(); j++){
			writers.get(j).close();
		}
    	long end = System.currentTimeMillis();
    	System.out.println("Distant Supervision took " + (end-start) + " millisseconds");
	}
	
	
	
	
	private List<String> getCandidateEntities(KnowledgeBase kb,String argumentName){
		Map<String,List<String>> entityMap = kb.getEntityMap();
		
		if(entityMap.containsKey(argumentName)){
			return entityMap.get(argumentName);
		}
		else{
			return new ArrayList<String>();
		}
	}
	
	private List<Triple<KBArgument, KBArgument, String>> getDateRelations(
			List<Pair<Argument, Argument>> sententialInstances, KnowledgeBase kb,
			DateMap dm, CoreMap sentence, Annotation d) {

		List<Triple<KBArgument, KBArgument, String>> dateRelations = new ArrayList<>();

		List<CoreMap> timeExpressions = d.get(TimeAnnotations.TimexAnnotations.class);
		if(timeExpressions == null){
			setTimeExpressions(sentence,d);
			timeExpressions = d.get(TimeAnnotations.TimexAnnotations.class);
		}
		
		for (Pair<Argument, Argument> p : sententialInstances) {
			
			
			String arg2TimexValue = getTimexValuesFromSentence(sentence,p.second);
			KBArgument kbarg2 = new KBArgument(p.second, arg2TimexValue);

			if (p.first instanceof KBArgument) {
				KBArgument kbarg1 = (KBArgument) p.first;
				String arg1Id = kbarg1.getKbId();
				if (dm.relMap.containsKey(arg1Id)) {
					List<Pair<String, String>> arg1Relations = dm.relMap.get(arg1Id);
					for (Pair<String, String> arg1Rel : arg1Relations) {
						String relString = arg1Rel.first;
						String timexValue = arg1Rel.second;
						//System.out.println("Comparing " + timexValue + " and " + arg2TimexValue + " for entity " + p.first.getArgName() + " " + kbarg1.getKbId() + " and relation " + relString);
						if (timexValue.equals(arg2TimexValue)) {
							Triple<KBArgument, KBArgument, String> dsTriple = new Triple<>(
									kbarg1, kbarg2, relString);
							dateRelations.add(dsTriple);
						}
					}
				}
			} else {
				List<String> candidateArg1Ids = getCandidateEntities(kb,
						p.first.getArgName());
				for (String candidateArg1Id : candidateArg1Ids) {
					if (dm.relMap.containsKey(candidateArg1Id)) {
						List<Pair<String, String>> arg1Relations = dm.relMap
								.get(candidateArg1Id);
						for (Pair<String, String> arg1Rel : arg1Relations) {
							String relString = arg1Rel.first;
							String timexValue = arg1Rel.second;
							//System.out.println("Comparing " + timexValue + " and " + arg2TimexValue + " for entity " + p.first.getArgName() + " " + candidateArg1Id + " and relation " + relString);
							if (timexValue.equals(arg2TimexValue)) {
								KBArgument kbarg1 = new KBArgument(p.first,
										candidateArg1Id);
								Triple<KBArgument, KBArgument, String> dsTriple = new Triple<>(
										kbarg1, kbarg2, relString);
								dateRelations.add(dsTriple);
							}
						}
					}
				}

			}
		}
		return dateRelations;
	}

	private String getTimexValuesFromSentence(CoreMap sentence, Argument arg) {
		
		StringBuilder sb = new StringBuilder();
		Pair<Integer,Integer> tokenOffsets = CorpusUtils.getTokenOffsetsFromCharacterOffsets(arg.getStartOffset(), arg.getEndOffset(), sentence);
		List<CoreMap> temporalExpressions = sentence.get(TimeAnnotations.TimexAnnotations.class);
		if(temporalExpressions != null){
			for(CoreMap temporalExpression: temporalExpressions){
				Timex timexValue = temporalExpression.get(TimeAnnotations.TimexAnnotation.class);
				Integer tokenStart = temporalExpression.get(CoreAnnotations.TokenBeginAnnotation.class);
				Integer tokenEnd = temporalExpression.get(CoreAnnotations.TokenEndAnnotation.class);
				if(tokenStart.equals(tokenOffsets.first) && tokenEnd.equals(tokenOffsets.second)){
					sb.append(timexValue.value());
				}
			}
		}
		return sb.toString().trim();
	}

	private void setTimeExpressions(CoreMap s, Annotation d) {
		
		try{
			String docName = s.get(SentDocName.class);
			Matcher docDateMatcher = documentDatePattern.matcher(docName);
			String docDate = null;
			if(docDateMatcher.find()){
				String dateString = docDateMatcher.group(1);
				String year = dateString.substring(0, 4);
				String month = dateString.substring(4,6);
				String day = dateString.substring(6,8);
				docDate = year+"-"+month+"-"+day;
				d.set(CoreAnnotations.DocDateAnnotation.class, docDate);
			}
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			for(CoreMap sentence: sentences){
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
				for(CoreLabel tok: tokens){
					tok.set(CoreAnnotations.OriginalTextAnnotation.class, tok.get(CoreAnnotations.TextAnnotation.class));
					tok.set(CharacterOffsetBeginAnnotation.class, tok.get(SentenceRelativeCharacterOffsetBeginAnnotation.class) + sentence.get(SentStartOffset.class));
					tok.set(CharacterOffsetEndAnnotation.class, tok.get(SentenceRelativeCharacterOffsetEndAnnotation.class) + sentence.get(SentStartOffset.class));
					
				}
			}
			try{
				suTimePipeline.annotate(d);
			}
			catch(Exception e){
				System.err.println("SUTime failed on document " + docName);
			}
		}
		catch(Exception e){
			
		}
	}

	private  List<NegativeAnnotation> findNegativeExampleAnnotations(
			List<Pair<Argument, Argument>> sententialInstances,
			List<Triple<KBArgument, KBArgument, String>> distantSupervisionAnnotations,
			KnowledgeBase KB, Integer sentGlobalID, CoreMap sentence, Annotation doc) {
		
		Map<String,List<String>> entityMap = KB.getEntityMap();
		List<NegativeAnnotation> negativeExampleAnnotations = new ArrayList<>();
		
		
		
		String arg1Type = "OTHER";
		String arg2Type = "OTHER";
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		if(sententialInstances.size() > 0){
			arg1Type = TypeConstraintUtils.translateNERTypeToTypeString(TypeConstraintUtils.getNERType(sententialInstances.get(0).first, tokens));
			arg2Type = TypeConstraintUtils.translateNERTypeToTypeString(TypeConstraintUtils.getNERType(sententialInstances.get(0).second,tokens));		
			
			List<String> typeAppropriateRelations = TypeSignatureRelationMap.getRelationsForTypeSignature(new Pair<String,String>(arg1Type,arg2Type));

			for(Pair<Argument,Argument> p : sententialInstances){
				//check that at least one argument is not in distantSupervisionAnnotations
				Argument arg1 = p.first;
				Argument arg2 = p.second;
				boolean canBeNegativeExample = true;
				for(Triple<KBArgument,KBArgument,String> t : distantSupervisionAnnotations){
					Argument annotatedArg1 = t.first;
					Argument annotatedArg2 = t.second;
					
					//if sententialInstance is a distance supervision annotation
					//then it is not a negative example candidate
					if( (arg1.getStartOffset() == annotatedArg1.getStartOffset()) &&
						(arg1.getEndOffset() == annotatedArg1.getEndOffset()) &&
						(arg2.getStartOffset() == annotatedArg2.getStartOffset()) &&
						(arg2.getEndOffset() == annotatedArg2.getEndOffset())){
						canBeNegativeExample = false;
						break;
					}
				}
				if(canBeNegativeExample){
					//look for KBIDs, select a random pair
					List<String> arg1Ids = new ArrayList<>();
					if(arg1 instanceof KBArgument){
						   arg1Ids.add(((KBArgument) arg1).getKbId());
					}
					else{
						if(entityMap.containsKey(arg1.getArgName())){
							List<String> candidateArg1Ids = entityMap.get(arg1.getArgName());
						    arg1Ids = candidateArg1Ids;
						}
					}
	
					List<String> arg2Ids = new ArrayList<>();
					if(arg2 instanceof KBArgument){
						arg2Ids.add(((KBArgument) arg2).getKbId());
					}
					else{
						if(entityMap.containsKey(arg2.getArgName())){
							List<String> candidateArg2Ids = entityMap.get(arg2.getArgName());
							arg2Ids = candidateArg2Ids;
						}
					}
					if( (!arg1Ids.isEmpty()) && (!arg2Ids.isEmpty())){
						//check that no pair of entities represented by these
						//argument share a relation:
						if(KB.noRelationsHold(arg1Ids,arg2Ids)){
							String arg1Id = arg1Ids.get(0);
							String arg2Id = arg2Ids.get(0);
							if((!arg1Id.equals("null")) && (!arg2Id.equals("null"))){
								KBArgument kbarg1 = new KBArgument(arg1,arg1Id);
								KBArgument kbarg2 = new KBArgument(arg2,arg2Id);
								List<String> annoRels = new ArrayList<String>();
								annoRels.add("NA");
								if(annoRels.size()>0){
									NegativeAnnotation negAnno = new NegativeAnnotation(kbarg1,kbarg2,sentGlobalID,annoRels);
									negativeExampleAnnotations.add(negAnno);
								}
							}
						}
					}
				}
			}
		}
		return negativeExampleAnnotations;
	}

	private boolean participatesInTargetRelations(List<String> arg1Ids,
			KnowledgeBase KB, List<String> typeAppropriateRelations) {

		for(String arg1Id: arg1Ids){
			for(String relation: typeAppropriateRelations){
				if(!KB.participatesInRelationAsArg1(arg1Id, relation)){
					return false;
				}
			}
		}
		return true;
	}

	public static class DistantSupervisionAnnotation{
		KBArgument arg1;
		KBArgument arg2;
		String rel;
		Integer sentID;
	}
	
	public static class DateMap {
		
		private Map<String,List<Pair<String,String>>> relMap;
		
		public DateMap(String file, String relationFile) throws IOException{
			Set<String> targetRelations = readTargetRelations(relationFile);
			
			BufferedReader br = BufferedIOUtils.getBufferedReader(new File(file));
			String nextLine;
			relMap = new HashMap<>();
			while((nextLine = br.readLine())!=null){
				try{
					String[] values = nextLine.split("\t");
					String entityId = values[0];
					String rel = values[2];
					String timeString = values[1];
					String timexString = null;
					if(timeString.length() == 4){
					  timexString = timeString;	
					}
					else{
						SUTime.Time time = SUTime.parseDateTime(timeString);
						timexString = time.getTimexValue();
					}
					
					if(targetRelations.contains(rel)){
						if(relMap.containsKey(entityId)){
							relMap.get(entityId).add(new Pair<>(rel,timexString));
						}
						else{
							List<Pair<String,String>> timeRels = new ArrayList<>();
							timeRels.add(new Pair<>(rel,timexString));
							relMap.put(entityId,timeRels);
						}
					}
				}
				catch (Exception e){
					
					
					
				}
			}
			
			br.close();
		}
		
		private Set<String> readTargetRelations(String relationFile) throws IOException {
			BufferedReader br = new BufferedReader(new FileReader(new File(relationFile)));
			String nextLine;
			Set<String> targetRelations = new HashSet<>();
			while((nextLine = br.readLine())!=null){
				targetRelations.add(nextLine.trim());
			}
			
			br.close();
			return targetRelations;
		}

		public boolean participatesInRelation(String arg1Id, String rel){
			List<Pair<String,String>> values = relMap.get(arg1Id);
			if(values != null){
				for(Pair<String,String> p : values){
					if(p.first.equals(rel)){
						return true;
					}
				}
			}
			return false;
		}
		
		public boolean matchesValue(String arg1Id, String rel, String timexValue){
			List<Pair<String,String>> values = relMap.get(arg1Id);
			if(values != null){
				for(Pair<String,String> p : values){
					if(p.first.equals(rel)){
						if(p.second.equals(timexValue)){
							return true;
						}
					}
				}
			}
			return false;
		}
		
		
	}
	
	
}