package edu.washington.multir.development;

import java.io.IOException;

import edu.washington.multirframework.multiralgorithm.Train;

public class RunTrain {
	
	public static void main(String[] args) throws IOException{
		String multirDir = args[0];
		Train.train(multirDir);
	}

}
