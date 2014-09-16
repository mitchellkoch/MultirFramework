package edu.washington.multir.extractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.ParseException;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.sententialextraction.DocumentExtractor;
import edu.washington.multir.util.CLIUtils;
import edu.washington.multir.util.EvaluationUtils;
import edu.washington.multir.util.ModelUtils;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePERPERSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.SentOffsetInformation.SentStartOffset;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;


/**
 * The main method of this class will print to an output file all the extractions
 * made over an input corpus.
 * @author jgilme1
 *
 */
public class ExtractFromCorpus {
	
	
	
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ParseException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, SQLException, IOException{
		List<String> arguments  = new ArrayList<String>();
		for(String arg: args){
			arguments.add(arg);
		}
		
		CorpusInformationSpecification cis = CLIUtils.loadCorpusInformationSpecification(arguments);
		FeatureGenerator fg = CLIUtils.loadFeatureGenerator(arguments);
		ArgumentIdentification ai = CLIUtils.loadArgumentIdentification(arguments);
		List<SententialInstanceGeneration> sigs = CLIUtils.loadSententialInstanceGenerationList(arguments);
		List<String> modelPaths = CLIUtils.loadFilePaths(arguments);
		
		Corpus c = new Corpus(arguments.get(0),cis,true);
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(arguments.get(1))));
		getMultiModelExtractions(c,ai,fg,sigs,modelPaths,bw);
		bw.close();

	}
	
	public static String formatExtractionString(Corpus c,Extraction e) throws SQLException{
		StringBuilder sb = new StringBuilder();
		String[] eValues  = e.toString().split("\t");
		String arg1Name = eValues[0];
		String arg2Name = eValues[3];
		String docName = eValues[6].replaceAll("__", "_");
		String rel = eValues[7];
		String sentenceText = eValues[9];
		
		
		Integer sentNum = Integer.parseInt(eValues[8]);
		Integer arg1SentStartOffset = Integer.parseInt(eValues[1]);
		Integer arg1SentEndOffset = Integer.parseInt(eValues[2]);
		Integer arg2SentStartOffset = Integer.parseInt(eValues[4]);
		Integer arg2SentEndOffset = Integer.parseInt(eValues[5]);
		
		CoreMap s = c.getSentence(sentNum);
		Integer sentStartOffset = s.get(SentStartOffset.class);
		Integer arg1DocStartOffset = sentStartOffset + arg1SentStartOffset;
		Integer arg1DocEndOffset = sentStartOffset + arg1SentEndOffset;
		Integer arg2DocStartOffset = sentStartOffset + arg2SentStartOffset;
		Integer arg2DocEndOffset = sentStartOffset + arg2SentEndOffset;
		
		sb.append(arg1Name);
		sb.append("\t");
		sb.append(arg1DocStartOffset);
		sb.append("\t");
		sb.append(arg1DocEndOffset);
		sb.append("\t");
		sb.append(arg2Name);
		sb.append("\t");
		sb.append(arg2DocStartOffset);
		sb.append("\t");
		sb.append(arg2DocEndOffset);
		sb.append("\t");
		sb.append(docName);
		sb.append("\t");
		sb.append(rel);
		sb.append("\t");
		sb.append(e.getScore());
		sb.append("\t");
		sb.append(sentenceText);
		return sb.toString().trim();
		
	}
	
	public static List<Extraction> getMultiModelExtractions(Corpus c,
			ArgumentIdentification ai, FeatureGenerator fg, List<SententialInstanceGeneration> sigs, List<String> modelPaths,
			BufferedWriter bw) throws SQLException, IOException {
		
		List<Extraction> extrs = new ArrayList<Extraction>();
		for(int i =0; i < sigs.size(); i++){
			Iterator<Annotation> docs = c.getDocumentIterator();
			SententialInstanceGeneration sig = sigs.get(i);
			String modelPath = modelPaths.get(i);
			DocumentExtractor de = new DocumentExtractor(modelPath,fg,ai,sig);

			Map<String,Integer> rel2RelIdMap =de.getMapping().getRel2RelID();
			Map<Integer,String> ftID2ftMap = ModelUtils.getFeatureIDToFeatureMap(de.getMapping());
			
			int docCount = 0;
			while(docs.hasNext()){
				Annotation doc = docs.next();
				List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
				for(CoreMap sentence : sentences){				
					//argument identification
					List<Argument> arguments =  ai.identifyArguments(doc,sentence);
					//sentential instance generation
					List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sentence);
					for(Pair<Argument,Argument> p : sententialInstances){
						Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> extrResult = 
						de.extractFromSententialInstanceWithAllFeatureScores(p.first, p.second, sentence, doc);
						if(extrResult != null){
							Triple<String,Double,Double> extrScoreTriple = extrResult.first;
							if(!extrScoreTriple.first.equals("NA")){
								Map<Integer,Double> featureScores = extrResult.second.get(rel2RelIdMap.get(extrResult.first.first));
								String rel = extrScoreTriple.first;
								List<Pair<String,Double>> featureScoreList = EvaluationUtils.getFeatureScoreList(featureScores, ftID2ftMap);
								
								String docName = sentence.get(SentDocName.class);
								String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
								Integer sentNum = sentence.get(SentGlobalID.class);
								Extraction e = new Extraction(p.first,p.second,docName,rel,sentNum,extrScoreTriple.third,senText);
								e.setFeatureScoreList(featureScoreList);
								extrs.add(e);
								bw.write(formatExtractionString(c,e)+"\n");
							}
							
						}
						}
					}
				}
				docCount++;
				if(docCount % 100 == 0){
					System.out.println(docCount + " docs processed");
					bw.flush();
				}
			}
		return EvaluationUtils.getUniqueList(extrs);
	}

}
