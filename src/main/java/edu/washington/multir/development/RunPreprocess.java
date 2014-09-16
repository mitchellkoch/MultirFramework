package edu.washington.multir.development;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class RunPreprocess {
	
	public static void main(String[] args) throws IOException{
		String featureFile = args[0];
		String multirDir = args[1];
		
		File multirDirFile = new File(multirDir);
		
		if(!multirDirFile.exists()){
			multirDirFile.mkdir();
		}
		
		Preprocess.run(featureFile,multirDir);
	}

}
