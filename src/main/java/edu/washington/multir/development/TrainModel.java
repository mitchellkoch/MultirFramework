package edu.washington.multir.development;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import edu.washington.multirframework.multiralgorithm.Train;
import edu.washington.multir.util.CLIUtils;

public class TrainModel {
	
	
	
	public static void main(String[] args) throws ParseException, IOException{
		
		List<String> arguments  = new ArrayList<String>();
		for(String arg: args){
			arguments.add(arg);
		}
		
		List<String> featureFilePaths = CLIUtils.loadFeatureFilePaths(arguments);
		List<String> modelFiles = CLIUtils.loadOutputFilePaths(arguments);
		Integer numberOfAverages = CLIUtils.loadNumberOfAverages(arguments);
		run(featureFilePaths,modelFiles,numberOfAverages);
	}
	
	public static void run(List<String> featureFilePaths, List<String> modelFiles, Integer numberOfAverages) throws IOException{
		
		if(featureFilePaths.size() != modelFiles.size()){
			throw new IllegalArgumentException("Number of feature files must be equal to number of output model files");
		}

		//for each input feature training file
		for(int i =0; i < featureFilePaths.size(); i++){
			String featureFile = featureFilePaths.get(i);
			File modelFile = new File(modelFiles.get(i));
			if(!modelFile.exists()) modelFile.mkdir();
			
			List<File> randomModelFiles = new ArrayList<File>();
			//if avg iteration !=1 train a model with averages of feature weights
			if(numberOfAverages != 1){
				
				//run preprocess
				Preprocess.run(featureFile, modelFile.getAbsolutePath().toString());

				//for each avg iteration run PP and Train
				int randomSeed = 1;
				for(int avgIter = 1; avgIter <= numberOfAverages; avgIter++){
					Train.train(modelFile.getAbsoluteFile().toString());					
					File newModelFile = new File(modelFile.getAbsolutePath()+"/"+modelFile.getName()+"avgIter"+avgIter);
					if(!newModelFile.exists()) newModelFile.mkdir();
					randomModelFiles.add(newModelFile);
					File oldParams = new File(modelFile.getAbsolutePath()+"/params");
					File newParams = new File(newModelFile.getAbsolutePath()+"/params");
					FileUtils.copyFile(oldParams, newParams);
					randomSeed++;
				}
				MakeAverageModel.run(randomModelFiles,modelFile);
			}
			
			else{
				Preprocess.run(featureFile, modelFile.getAbsolutePath().toString());
				Train.train(modelFile.getAbsoluteFile().toString());
			}
		}
	}

}
