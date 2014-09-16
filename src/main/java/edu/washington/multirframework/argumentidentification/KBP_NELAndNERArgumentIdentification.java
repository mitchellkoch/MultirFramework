package edu.washington.multirframework.argumentidentification;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Interval;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.data.Argument;

public class KBP_NELAndNERArgumentIdentification implements ArgumentIdentification{

	
	private static KBP_NELAndNERArgumentIdentification instance = null;
	
	private KBP_NELAndNERArgumentIdentification(){}
	public static KBP_NELAndNERArgumentIdentification getInstance(){
		if(instance == null) instance = new KBP_NELAndNERArgumentIdentification();
		return instance;
		}
	
	@Override
	public List<Argument> identifyArguments(Annotation d, CoreMap s) {
		List<Argument> nerArguments = ExtendedNERArgumentIdentification.getInstance().identifyArguments(d, s);
		List<Argument> nelArguments = NELArgumentIdentification.getInstance().identifyArguments(d, s);
		List<Argument> args = new ArrayList<Argument>();
		args.addAll(nelArguments);
		for(Argument nerArg : nerArguments){
			boolean intersects = false;
			for(Argument nelArg: nelArguments){
				Interval<Integer> nerArgInterval = Interval.toInterval(nerArg.getStartOffset(), nerArg.getEndOffset());
				Interval<Integer> nelArgInterval = Interval.toInterval(nelArg.getStartOffset(), nelArg.getEndOffset());
				if(nerArgInterval.intersect(nelArgInterval) !=null){
					intersects = true;
				}
			}
			if(!intersects){
				args.add(nerArg);
			}
		}
		
		List<CoreLabel> tokens = s.get(CoreAnnotations.TokensAnnotation.class);
		List<Argument> websiteArguments = new ArrayList<>();
		for(CoreLabel tok: tokens){
			String text = tok.get(CoreAnnotations.TextAnnotation.class);
			if(text.contains("http") || text.contains("www")){
				Argument websiteArgument = new Argument(text,tok.get(SentenceRelativeCharacterOffsetBeginAnnotation.class),
						tok.get(SentenceRelativeCharacterOffsetEndAnnotation.class));
				Interval<Integer> webArgInterval = Interval.toInterval(websiteArgument.getStartOffset(), websiteArgument.getEndOffset());
				boolean intersects = false;
				for(Argument arg: args){
					Interval<Integer> argInterval = Interval.toInterval(arg.getStartOffset(), arg.getEndOffset());
					if(argInterval.intersect(webArgInterval) != null){
						intersects = true;
					}
				}
				if(!intersects){
					websiteArguments.add(websiteArgument);
				}
			}
		}
		
		args.addAll(websiteArguments);
		
		return args;
	}
	
}
