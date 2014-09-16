package edu.washington.multir.development;

import java.io.File;
import java.io.IOException;

import edu.washington.multirframework.multiralgorithm.Dataset;
import edu.washington.multirframework.multiralgorithm.MILDocument;
import edu.washington.multirframework.multiralgorithm.MemoryDataset;

public class ReadTrain {
	
	public static void main(String[] args) throws IOException{
		run(args[0]);
	}
	
	public static void run(String trainFile) throws IOException{
		
		Dataset train = new MemoryDataset(trainFile);
		MILDocument d = new MILDocument();
		while(train.next(d)){
			if(d.Y.length > 0){
				System.out.println("arg1="+d.arg1);
				System.out.println("arg2="+d.arg2);
				System.out.print("rels = ");
				for(int y: d.Y){
					System.out.print(y + " ");
				}
				System.out.print("\n");
				System.out.println("numMentions="+d.numMentions);
				System.out.println("--------------------");
			}
		}

	}

}
