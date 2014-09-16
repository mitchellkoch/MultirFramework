package edu.washington.multirframework.argumentidentification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
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
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.SentOffsetInformation.SentStartOffset;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class KBPRelationMatching implements RelationMatching {

	
	private static KBPRelationMatching instance = null;
	
	public static KBPRelationMatching getInstance(){
		if(instance == null){
			instance = new KBPRelationMatching();
		}
		return instance;
	}
	
	private KBPRelationMatching(){}
	private static NELByTypeRelationMatching nelRM = NELByTypeRelationMatching.getInstance();
	private static Pattern documentDatePattern = Pattern.compile("_(\\d\\d\\d\\d\\d\\d\\d\\d)");
	private static AnnotationPipeline suTimePipeline = new AnnotationPipeline();
	private static Properties properties = new Properties();
	static{
		suTimePipeline.addAnnotator(new TimeAnnotator("sutime",properties));
	}

	@Override
	public List<Triple<KBArgument, KBArgument, String>> matchRelations(
			List<Pair<Argument, Argument>> sententialInstances, KnowledgeBase KB,
			CoreMap sentence, Annotation doc) {
		
		List<Triple<KBArgument,KBArgument,String>> dsRelations = new ArrayList<>();
		dsRelations.addAll(nelRM.matchRelations(sententialInstances, KB, sentence, doc));

		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		
		for(Pair<Argument,Argument> sententialInstance : sententialInstances){
			try{
				Argument arg1 = sententialInstance.first;
				Argument arg2 = sententialInstance.second;
	
				String arg1Type = TypeConstraintUtils.translateNERTypeToTypeString(TypeConstraintUtils.getNERType(sententialInstance.first, tokens));
				String arg2Type = TypeConstraintUtils.translateNERTypeToTypeString(TypeConstraintUtils.getNERType(sententialInstance.second,tokens));
				List<String> relevantRelations = TypeSignatureRelationMap.getRelationsForTypeSignature(new Pair<String,String>(arg1Type,arg2Type));
				
				if(arg1 instanceof KBArgument){
					KBArgument kbarg1 = (KBArgument)arg1;
					
					//if arg2 is a DATE
					if(arg2Type.equals(GeneralType.DATE)){
						List<CoreMap> timeExpressions = sentence.get(TimeAnnotations.TimexAnnotations.class);
						if(timeExpressions == null){
							setTimeExpressions(sentence,doc);
							timeExpressions = sentence.get(TimeAnnotations.TimexAnnotations.class);
						}
						String arg2TimexValue = getTimexValuesFromSentence(sentence,sententialInstance.second);
						if(arg2TimexValue.equals("") || (sententialInstance.second.getArgName().length() == 4)){
							arg2TimexValue = sententialInstance.second.getArgName();
						}
						
						KBArgument kbarg2 = new KBArgument(sententialInstance.second, arg2TimexValue);
	
						List<String> relationsBetweenArgs = KB.getRelationsBetweenArgumentIds(kbarg1.getKbId(), kbarg2.getKbId());
						for(String rel: relationsBetweenArgs){
							if(relevantRelations.contains(rel)){
								dsRelations.add(new Triple<>(kbarg1,kbarg2,rel));
							}
						}
						
					}
					//if arg2 is a NUM
					if(arg2Type.equals(GeneralType.NUMBER)){
						//handle both per:age and org:number_of_employees_or_members
						
						//handle org:number_of_employees_or_members
						if(arg1Type.equals(GeneralType.ORGANIZATION)){
							KBArgument kbarg2 = new KBArgument(sententialInstance.second, sententialInstance.second.getArgName());
							List<String> relationsBetweenArgs = KB.getRelationsBetweenArgumentIds(kbarg1.getKbId(), kbarg2.getKbId());
							for(String rel: relationsBetweenArgs){
								if(relevantRelations.contains(rel)){
								  dsRelations.add(new Triple<>(kbarg1,kbarg2,rel));
								}
							}
						}
						
						//handle per:age, have to add this relation to DS manually
						if(arg1Type.equals(GeneralType.PERSON)){
							Pair<Integer,Integer> ageRange = calculateAgeRangeOfPerson(kbarg1,KB,sentence,doc);
							if(ageRange != null){
								Integer arg2Num = Integer.parseInt(arg2.getArgName());
								if(arg2Num == ageRange.first || arg2Num == ageRange.second){
									KBArgument kbarg2 = new KBArgument(sententialInstance.second,arg2Num.toString());
									String ageRel = "per:age";
									dsRelations.add(new Triple<>(kbarg1,kbarg2,ageRel));
								}
							}
						}
					}
					//if arg2 is a website
					if(arg2Type.equals(GeneralType.OTHER)){
						String arg2String = sententialInstance.second.getArgName();
						if(arg2String.contains("http") || arg2String.contains("www")){
							KBArgument kbarg2 = new KBArgument(sententialInstance.second,arg2String);
							List<String> relationsBetweenArgs = KB.getRelationsBetweenArgumentIds(kbarg1.getKbId(), kbarg2.getKbId());
							for(String rel: relationsBetweenArgs){
								if(relevantRelations.contains(rel)){
									dsRelations.add(new Triple<>(kbarg1,kbarg2,rel));
								}
							}
						}
					}
					
					//check for alias
					Map<String,List<String>> aliasToIdMap = KB.getEntityMap();
					String arg2Name = sententialInstance.second.getArgName();
					if(aliasToIdMap.containsKey(arg2Name)){
						String aliasRel = "/common/topic/alias";
						List<String> ids = aliasToIdMap.get(arg2Name);
						if(!arg2Name.equals(sententialInstance.first.getArgName())){
							if(ids.contains(kbarg1.getKbId())){
								if(relevantRelations.contains(aliasRel)){
									dsRelations.add(new Triple<>(kbarg1,new KBArgument(sententialInstance.second,arg2Name),aliasRel));
								}
							}
						}
					}
				}
			}
			catch(Exception e){
			}
		}

		return dsRelations;
		
	}
	
	private Pair<Integer, Integer> calculateAgeRangeOfPerson(KBArgument kbarg1,
			KnowledgeBase kb, CoreMap sentence, Annotation doc) {
		
		List<Pair<String,String>> rels = kb.getEntityPairRelationMap().get(kbarg1.getKbId());
		Integer yearOfBirth = null;
		if(rels != null){
			for(Pair<String,String> rel : rels){
				if(rel.first.equals("per:date_of_birth")){
					String value = rel.second;
					if(value.length() == 4){
						yearOfBirth = Integer.parseInt(value);
					}
					else{
						try{
							SUTime.Time time = SUTime.parseDateTime(value);
							yearOfBirth  =  Integer.parseInt(time.getTimexValue().trim().substring(0,4));
						}
						catch(Exception e){
							
						}
					}
					break;
				}
			}
		}
		
		if(yearOfBirth != null){
			String docName = sentence.get(SentDocName.class);
			Matcher docDateMatcher = documentDatePattern.matcher(docName);
			Integer docYear = null;
			if(docDateMatcher.find()){
				String docDate = docDateMatcher.group(1);
				try{
					docYear = Integer.parseInt(docDate.substring(0,4));
				}
				catch(Exception e){
					
				}
			}
	
			if(docYear != null){
				Integer difference = docYear - yearOfBirth;
				return new Pair<>(difference-1,difference);
			}
			
		}
		
		return null;
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
					if(timexValue.timexType().equals("DATE")){
						sb.append(timexValue.value());
						return sb.toString().trim();
					}
				}
			}
		}
		return sb.toString().trim();
	}

}
