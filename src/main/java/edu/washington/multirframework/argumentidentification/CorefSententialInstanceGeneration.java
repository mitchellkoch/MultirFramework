package edu.washington.multirframework.argumentidentification;

import java.util.ArrayList;
import java.util.List;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Interval;
import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.data.Argument;

/**
 * Implements <code>SententialInstanceGeneration</code> method 
 * <code>generateSententialInstances</code> that is used during
 * <code>DistantSupervision</code> and at extraction time.
 * @author jgilme1
 *
 */
public class CorefSententialInstanceGeneration implements
		SententialInstanceGeneration {

	private static CorefSententialInstanceGeneration instance = null;
	
	public static CorefSententialInstanceGeneration getInstance(){
		if(instance == null){
			instance = new CorefSententialInstanceGeneration();
		}
		return instance;
	}
	

	//method only returns pairs of Arguments that belong to different Coref clusters
	@Override
	public List<Pair<Argument, Argument>> generateSententialInstances(
			List<Argument> arguments, CoreMap sentence) {
		
		List<Pair<Argument,Argument>> sententialInstances = new ArrayList<>();
		
		
		for(int i =0; i < arguments.size(); i++){
			for(int j = 0; j < arguments.size(); j++){
				if(j != i){
					Argument arg1 = arguments.get(i);
					Argument arg2 = arguments.get(j);
					Interval<Integer> arg1Interval = Interval.toInterval(arg1.getStartOffset(), arg1.getEndOffset());
					Interval<Integer> arg2Interval = Interval.toInterval(arg2.getStartOffset(), arg2.getEndOffset());
					if(arg1Interval.intersect(arg2Interval) == null){
						Pair<Argument,Argument> p = new Pair<>(arg1,arg2);
						if(areInDifferentCorefClusters(p,sentence)) {
							sententialInstances.add(p);
						}
					}
				}
			}
		}
		return sententialInstances;
	}
	
	/**
	 * Uses <code>CorefCoreAnnotations.CorefChainAnnotation</code> data to check 
	 * if two arguments belong to the same coref cluster
	 * @param p  <code>Pair</code> of <code>Argument</code>
	 * @param sentence <code>CoreMap</code> representation of sentence, includes Coreference data
	 * @return  true if Arguments are in different coref clusters, false otherwise
	 */
	private boolean areInDifferentCorefClusters(Pair<Argument, Argument> p, CoreMap sentence) {
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		Argument arg1 = p.first;
		Argument arg2 = p.second;
		List<Integer> arg1CorefClusters = new ArrayList<>();
		List<Integer> arg2CorefClusters = new ArrayList<>();
		
		
		for(CoreLabel tok: tokens){
			int tokBegOffset  = tok.get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
			if(tokBegOffset >= arg1.getStartOffset() && tokBegOffset <= arg1.getEndOffset()){
				arg1CorefClusters.add(tok.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class));
			}
			if(tokBegOffset >= arg2.getStartOffset() && tokBegOffset <= arg2.getEndOffset()){
				arg2CorefClusters.add(tok.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class));
			}
		}
		
		for(Integer i : arg1CorefClusters){
			if( i != null){
				for(Integer j : arg2CorefClusters){
					if(j != null){
						if(i.equals(j)){
							return false;
						}
					}
				}
			}
		}
		return true;
	}

}
