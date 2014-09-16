package edu.washington.multirframework.featuregeneration;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class DefaultFeatureGeneratorMinusDirMinusDepMinusLong implements FeatureGenerator{

	private static FeatureGenerator fg = new DefaultFeatureGeneratorMinusDirMinusDep();
	@Override
	public List<String> generateFeatures(Integer arg1StartOffset,
			Integer arg1EndOffset, Integer arg2StartOffset,
			Integer arg2EndOffset, String arg1Id, String arg2Id,
			CoreMap sentence, Annotation document) {
		List<String> returnFeatures = new ArrayList<>();
		List<String> features = fg.generateFeatures(arg1StartOffset, arg1EndOffset, arg2StartOffset, arg2EndOffset, arg1Id, arg2Id, sentence, document);
		for(String f: features){
			if(!f.contains("*LONG")){
				returnFeatures.add(f);
			}
		}
		return returnFeatures;
	}
	

}
