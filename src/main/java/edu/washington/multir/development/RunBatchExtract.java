package edu.washington.multir.development;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;

import edu.washington.multir.util.CLIUtils;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.extractor.MultirRelationExtractor;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;

public class RunBatchExtract {
	
	
	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ParseException, InstantiationException, SQLException, IOException{
		List<String> arguments = new ArrayList<String>();
		for(String arg: args){
			arguments.add(arg);
		}
		
		FeatureGenerator fg = CLIUtils.loadFeatureGenerator(arguments);
		ArgumentIdentification ai = CLIUtils.loadArgumentIdentification(arguments);
		SententialInstanceGeneration sig = CLIUtils.loadSententialInformationGeneration(arguments);
		CorpusInformationSpecification cis = CLIUtils.loadCorpusInformationSpecification(arguments);
		String multirDir = arguments.get(0);
		String corpusDB = arguments.get(1);
		
		
		MultirRelationExtractor mre = new MultirRelationExtractor(multirDir,fg,ai,sig,cis);
		mre.batchExtract(corpusDB);
	}

}
