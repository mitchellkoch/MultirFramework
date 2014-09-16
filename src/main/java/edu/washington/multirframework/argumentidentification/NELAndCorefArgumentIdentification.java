package edu.washington.multirframework.argumentidentification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.AnnotationUtils;
import edu.stanford.nlp.ie.machinereading.structure.Span;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.corpus.SentNamedEntityLinkingInformation.NamedEntityLinkingAnnotation;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;

public class NELAndCorefArgumentIdentification implements
ArgumentIdentification {

	private static NELAndCorefArgumentIdentification instance = null;

	public NELAndCorefArgumentIdentification(){}
	public static NELAndCorefArgumentIdentification getInstance(){
		if(instance == null) instance = new NELAndCorefArgumentIdentification();
		return instance;
	}

	// Information about the current document; not thread safe, so should have an instance of NELAndCorefArgumentIdentification for every thread
	private Annotation currentDocument = null;
	private List<List<CorefMention>> mentionsInSentences;
	private Map<Integer,IntCounter<String>> corefClusterIDToKbIds = null;
	private List<List<CorefArgument>> docNelNerArgs;

	public interface CorefArgument {
		public CorefMention getCorefMention();
		public int getCorefClusterID();
		public int getStartOffset();
		public int getEndOffset();
		public void setCorefClusterID(int corefClusterID);
		public String getKbId();
	}

	public class KBCorefArgument extends KBArgument implements CorefArgument {

		private CorefMention cm;
		private int corefClusterId = -1;

		public KBCorefArgument(Argument arg, String kbid, CorefMention cm) {
			super(arg, kbid);
			this.cm = cm;
			this.corefClusterId = cm.corefClusterID;
		}

		public KBCorefArgument(KBArgument arg) {
			super(arg, arg.getKbId());
		}

		public KBCorefArgument(KBArgument arg, int corefClusterID) {
			super(arg, arg.getKbId());
			this.cm = null;
			this.corefClusterId = corefClusterID;
		}

		@Override
		public CorefMention getCorefMention() {
			return cm;
		}

		@Override
		public int getCorefClusterID() {
			return corefClusterId;
		}

		@Override
		public void setCorefClusterID(int corefClusterID) {
			this.corefClusterId = corefClusterID;	
		}

	}

	public class NonKBCorefArgument extends Argument implements CorefArgument {

		private final CorefMention cm;
		private int corefClusterID = -1;;

		public NonKBCorefArgument(Argument arg, CorefMention cm) {
			super(arg);
			this.cm = cm;
			this.corefClusterID = cm.corefClusterID;
		}

		public NonKBCorefArgument(Argument arg) {
			super(arg);
			this.cm = null;
		}

		public NonKBCorefArgument(Argument arg, int corefClusterID) {
			super(arg);
			this.cm = null;
			this.corefClusterID = corefClusterID;
		}

		@Override
		public CorefMention getCorefMention() {
			return cm;
		}

		@Override
		public int getCorefClusterID() {
			return corefClusterID;
		}

		@Override
		public void setCorefClusterID(int corefClusterID) {
			this.corefClusterID = corefClusterID;	
		}

		@Override
		public String getKbId() {
			return null;
		}

	}

	private boolean argsHaveSameOffsets(Argument one, Argument other) {
		if((one.getStartOffset() == other.getStartOffset()) &&
				(one.getEndOffset()  == other.getEndOffset()) && 
				(one.getArgName().equals(other.getArgName()))){
			return true;
		}
		else{
			return false;
		}
	}
	
	private void rebuildDocumentInfo() {
		corefClusterIDToKbIds = new HashMap<Integer, IntCounter<String>>();
		docNelNerArgs = new ArrayList<List<CorefArgument>>();
		mentionsInSentences = new ArrayList<List<CorefMention>>();
		List<CoreMap> sentences = currentDocument.get(CoreAnnotations.SentencesAnnotation.class);
		int sentIndex = 0;
		for(CoreMap sentence : sentences){
			List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
			//	int sentIndex = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class);
			if (sentence.get(CoreAnnotations.SentenceIndexAnnotation.class) == null) {
				sentence.set(CoreAnnotations.SentenceIndexAnnotation.class, sentIndex);
			}

			int sentNum = sentIndex + 1;

			Map<Integer,CorefChain> corefChainMap = currentDocument.get(CorefCoreAnnotations.CorefChainAnnotation.class);
			List<CorefMention> mentionsInSentence = new ArrayList<>();
			//find all coref mentions in the current sentence
			if(corefChainMap != null){
				for(Integer k : corefChainMap.keySet()){
					CorefChain cc = corefChainMap.get(k);
					List<CorefMention> corefMentions = cc.getMentionsInTextualOrder();
					for(CorefMention cm : corefMentions){
						if(cm.sentNum == sentNum){
							mentionsInSentence.add(cm);
						}
					}	
				}			
			}
			mentionsInSentences.add(mentionsInSentence);

			List<Argument> originalArgs =  NELAndNERArgumentIdentification.getInstance().identifyArguments(currentDocument, sentence);
			List<CorefArgument> args = new ArrayList<CorefArgument>();

			// Get the coref cluster of the arg using exact span match
			for (Argument originalArg : originalArgs) {
				int corefClusterID = -1;
				for(CorefMention cm : mentionsInSentence) {
					Integer startOffset = tokens.get(cm.startIndex-1).get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
					Integer endOffset = tokens.get(cm.endIndex-2).get(SentenceRelativeCharacterOffsetEndAnnotation.class);
					Argument editedCm = corefMentionToArgument(sentence, cm);

					//			if (editedCm.getStartOffset().equals(originalArg.getStartOffset()) && endOffset.equals(originalArg.getEndOffset())) {
					if (startOffset.equals(originalArg.getStartOffset()) && endOffset.equals(originalArg.getEndOffset())
							|| argsHaveSameOffsets(editedCm, originalArg))	 {
						corefClusterID = cm.corefClusterID;
					}
				}

				CorefArgument arg = new NonKBCorefArgument(originalArg, corefClusterID);
				if (originalArg instanceof KBArgument) {
					arg = new KBCorefArgument((KBArgument) originalArg, corefClusterID);

					// Since the arg has a KbId, put it as a vote for its coref cluster
					String kbId = ((KBArgument) originalArg).getKbId();
					if (!corefClusterIDToKbIds.containsKey(corefClusterID))
						corefClusterIDToKbIds.put(corefClusterID, new IntCounter<String>());
					corefClusterIDToKbIds.get(corefClusterID).incrementCount(kbId);
				}
				args.add(arg);
			}
			// Try to fill in more coref cluster IDs by looking at shortest (least tokens) coref mention that contains it but ignoring ones with inconsistent kbids
			for (CorefArgument arg : args) {
				if (arg.getCorefClusterID() == -1) {
					// Take shortest (least tokens) coref mention that contains it
					IntCounter<CorefMention> containingMentions = new IntCounter<CorefMention>();
					for(CorefMention cm : mentionsInSentence) {
						Integer startOffset = tokens.get(cm.startIndex-1).get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
						Integer endOffset = tokens.get(cm.endIndex-2).get(SentenceRelativeCharacterOffsetEndAnnotation.class);
						//				Argument editedCm = corefMentionToArgument(sentence, cm);

						if (((arg.getStartOffset() >= startOffset) && (arg.getEndOffset() <= endOffset))) {
							containingMentions.setCount(cm, cm.mentionSpan.split("\\s+").length);
						}
					}
					CorefMention shortestContainingMention = containingMentions.argmin();
					if (shortestContainingMention != null) {
						// Ignore if there was already a different kbid from exact span matches
						if (corefClusterIDToKbIds.containsKey(shortestContainingMention.corefClusterID) &&
								!(corefClusterIDToKbIds.get(shortestContainingMention.corefClusterID).argmax().equals(arg.getKbId())))
							continue;

						arg.setCorefClusterID(shortestContainingMention.corefClusterID);

						if (arg instanceof KBArgument) {
							// Since the arg has a KbId, put it as a vote for its coref cluster
							String kbId = ((KBArgument) arg).getKbId();
							if (!corefClusterIDToKbIds.containsKey(shortestContainingMention.corefClusterID))
								corefClusterIDToKbIds.put(shortestContainingMention.corefClusterID, new IntCounter<String>());
							corefClusterIDToKbIds.get(shortestContainingMention.corefClusterID).incrementCount(kbId);
						}
					}
				}
			}
			sentIndex++;

			docNelNerArgs.add(args);
		}
	}

	@Override
	public List<Argument> identifyArguments(Annotation d, CoreMap sentence) {
		if (currentDocument != d) {
			currentDocument = d;
			rebuildDocumentInfo();
		}

		int sentIndex = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class);
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		List<CorefMention> mentionsInSentence = mentionsInSentences.get(sentIndex); 

		List<Argument> args = new ArrayList<>();
		List<Argument> corefArgs = new ArrayList<>();
		for(CorefMention cm : mentionsInSentence){
			//String kbLink = getLink(corefChainMap.get(cm.corefClusterID),d);
			String kbLink = null;
			if (corefClusterIDToKbIds.containsKey(cm.corefClusterID)) {
				kbLink = corefClusterIDToKbIds.get(cm.corefClusterID).argmax();
			}
			// if we found a most popular link, then instantiate a KBArgument
			if((tokens.size() > 0) && tokens.size() > (cm.endIndex-2)){
				Argument editedCm = corefMentionToArgument(sentence, cm);
				if (editedCm.getStartOffset() >= editedCm.getEndOffset())
					continue;

				//		Integer startOffset = tokens.get(cm.startIndex-1).get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
				//		Integer endOffset = tokens.get(cm.endIndex-2).get(SentenceRelativeCharacterOffsetEndAnnotation.class);
				if(editedCm.getArgName().split("\\s+").length < 6){
					Argument arg = new NonKBCorefArgument(editedCm, cm);
					if(kbLink != null){
						KBArgument kbArg = new KBCorefArgument(arg,kbLink,cm);	
						corefArgs.add(kbArg);
					}
					// else instantiate an ordinary Argument
					else{
						corefArgs.add(arg);
					}
				}
			}
		}

		List<CorefArgument> nelNerArgs = docNelNerArgs.get(sentIndex);

		for(CorefArgument arg: nelNerArgs){
			if(arg instanceof KBArgument){
				args.add((Argument) arg);
			}
		}

		for(Argument corefArg: corefArgs){
			if(corefArg instanceof KBArgument){
				if(!corefArg.intersectsWithList(args)){
					args.add(corefArg);
				}
			}
		}

		for(CorefArgument arg: nelNerArgs){
			if(!(arg instanceof KBArgument)){
				if(!((Argument) arg).intersectsWithList(args)){
					args.add((Argument) arg);
				}
			}
		}

		for(Argument corefArg: corefArgs){
			if(!(corefArg instanceof KBArgument)){
				if(!corefArg.intersectsWithList(args)){
					args.add(corefArg);
				}
			}
		}

		return args;
	}

	private Argument corefMentionToArgument(CoreMap sentence, CorefMention cm) {
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		int tokenStartOffset = cm.startIndex - 1;
		int tokenEndOffset = cm.endIndex - 1;
		String mentionString = cm.mentionSpan;

		if (mentionString.matches("^([Tt]he|[Aa]|[Aa]n) .*")) {
			tokenStartOffset++;
			mentionString = AnnotationUtils.getTextContent(sentence, new Span(tokenStartOffset, tokenEndOffset));
		}
		if (mentionString.endsWith("'s")) {
			tokenEndOffset--;
			mentionString = AnnotationUtils.getTextContent(sentence, new Span(tokenStartOffset, tokenEndOffset));
		}

		if (tokenEndOffset <= 0) {
			tokenEndOffset = 1;
		}

		int charStartOffset = tokens.get(tokenStartOffset).get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
		int charEndOffset = tokens.get(tokenEndOffset - 1).get(SentenceRelativeCharacterOffsetEndAnnotation.class);

		return new Argument(mentionString,charStartOffset,charEndOffset);
	}

}
