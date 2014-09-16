package edu.washington.multir.development;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.washington.multirframework.multiralgorithm.DenseVector;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;

public class MakeAverageModel {
	
	
	//private static Map<Integer,Map<Integer,Double>> relFeatureScoreMap;
	private static Mappings m;
	private static Model model;
	private static Parameters p;
	
	public static void main (String[] args) throws IOException{
		
		List<File> randomizedModels = new ArrayList<>();
		
		for(int i =0; i < (args.length-1); i++){
			randomizedModels.add(new File(args[i]));
		}
		run(randomizedModels,new File(args[args.length-1]));
	}
	
	
	public static void run(List<File> randomizedModels, File modelFile) throws IOException{
			
		initializeMapping(modelFile.getAbsolutePath());
		System.out.println("Initialized maps");

		int size =0;
		for(int i =0; i < randomizedModels.size(); i++){
			String randomDir = randomizedModels.get(i).getAbsolutePath();
			collectValuesFromRandomDir(randomDir);
			System.out.println(i+1 + " random dirs added");
			size++;
		}
		
		//get average
		writeNewModel(modelFile.getAbsolutePath(),size);
		System.out.println("Wrote new model");
	}

	private static void writeNewModel(String newModelDir, int size) throws IOException {
		String newParamsFile = newModelDir + "/params";
		
	
		
		for(int rel = 0; rel < p.relParameters.length; rel++){	
			for(int feat = 0; feat < p.relParameters[rel].vals.length; feat++){
				p.relParameters[rel].vals[feat] = (p.relParameters[rel].vals[feat]/(double)size);
			}
		}
		
		p.serialize(newParamsFile);
	}

	private static void collectValuesFromRandomDir(String randomDir) throws IOException {
		

		//String mappingFile = randomDir + "/mapping";
		//Mappings thisMapping = new Mappings();
		//thisMapping.read(mappingFile);
		
		//Map<Integer,String> relIdToRelString = new HashMap<>();
		//Map<Integer,String> ftId2FtString = new HashMap<>();
		
		//for(String rel: thisMapping.getRel2RelID().keySet()){
	//		relIdToRelString.put(thisMapping.getRel2RelID().get(rel), rel);
		//}
		//for(String ft: thisMapping.getFt2ftId().keySet()){
		//	ftId2FtString.put(thisMapping.getFt2ftId().get(ft),ft);
		//}
		

		String parameterFile = randomDir + "/params";
		InputStream parameterInputStream = new BufferedInputStream(new FileInputStream(parameterFile));
		int numFeatures = p.relParameters[0].vals.length;
		
		for(int rel = 0; rel < p.relParameters.length; rel++){
			DenseVector newRelVector  = new DenseVector(numFeatures);
			newRelVector.deserialize(parameterInputStream);			
			for(int feat = 0; feat < p.relParameters[rel].vals.length; feat++){
				p.relParameters[rel].vals[feat] = p.relParameters[rel].vals[feat] + newRelVector.vals[feat];
			}
		}
		parameterInputStream.close();
		
	}

	private static void initializeMapping(String dir1) throws IOException {
				
		m = new Mappings();
		m.read(dir1+"/mapping");
		String mappingFile = dir1+"/mapping";
		Mappings dir1Mapping = new Mappings();
		dir1Mapping.read(mappingFile);
		String modelFile = dir1 + "/model";
		model = new Model();
		model.read(modelFile);
		p = new Parameters();
		p.model = model;
		p.init();
	}

}
