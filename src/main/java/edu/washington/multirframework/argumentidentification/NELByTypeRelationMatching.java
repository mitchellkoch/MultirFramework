package edu.washington.multirframework.argumentidentification;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.data.TypeSignatureRelationMap;
import edu.washington.multir.util.TypeConstraintUtils;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;
import edu.stanford.nlp.ling.CoreLabel;


public class NELByTypeRelationMatching implements RelationMatching {

	
	private static NELByTypeRelationMatching instance = null;
	
	public static NELByTypeRelationMatching getInstance(){
		if(instance == null){
			instance = new NELByTypeRelationMatching();
		}
		return instance;
	}
	
	private NELByTypeRelationMatching(){}
	
	@Override
	public List<Triple<KBArgument, KBArgument, String>> matchRelations(
			List<Pair<Argument, Argument>> sententialInstances, KnowledgeBase KB,
			CoreMap sentence, Annotation doc) {

		List<Triple<KBArgument,KBArgument,String>> dsRelations = new ArrayList<>();

		String arg1Type = "OTHER";
		String arg2Type = "OTHER";
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		if(sententialInstances.size() > 0){
			arg1Type = TypeConstraintUtils.translateNERTypeToTypeString(TypeConstraintUtils.getNERType(sententialInstances.get(0).first, tokens));
			arg2Type = TypeConstraintUtils.translateNERTypeToTypeString(TypeConstraintUtils.getNERType(sententialInstances.get(0).second,tokens));		
		}
		
		List<String> typeAppropriateRelations = TypeSignatureRelationMap.getRelationsForTypeSignature(new Pair<String,String>(arg1Type,arg2Type));
				
		for(Pair<Argument,Argument> sententialInstance : sententialInstances){

			
			
			Argument arg1 = sententialInstance.first;
			Argument arg2 = sententialInstance.second;
			//if both arguments have ids in the KB
			if((arg1 instanceof KBArgument) && (arg2 instanceof KBArgument)){
				//check if they have a relation
				KBArgument kbArg1 = (KBArgument)arg1;
				KBArgument kbArg2 = (KBArgument)arg2;
				List<String> relations = KB.getRelationsBetweenArgumentIds(kbArg1.getKbId(), kbArg2.getKbId());
				for(String rel: relations){
					if(typeAppropriateRelations.contains(rel)){
						Triple<KBArgument,KBArgument,String> dsRelation = new Triple<>(kbArg1,kbArg2,rel);
						dsRelations.add(dsRelation);
					}
				}
			}
		}
		return dsRelations;
	}
}