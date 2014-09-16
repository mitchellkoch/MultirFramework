package edu.washington.multir.data;

import edu.washington.multirframework.data.Extraction;

/**
 * Representation of ExtractionAnootation is 
 * an instance of an Extractoin with a boolean label
 * @author jgilme1
 *
 */
public class ExtractionAnnotation {
	Extraction e;
	boolean label;
	
	public ExtractionAnnotation(Extraction e, boolean label){
		this.e = e;
		this.label = label;
	}
	
	public static ExtractionAnnotation deserialize(String s){
		String[] values = s.split("\t");
		if(values.length != 11){
			throw new IllegalArgumentException("There should be 11 columns of data" + s);
		}
		StringBuilder extractionStringBuilder = new StringBuilder();
		
		for(int i = 0; i < 10; i++){
			extractionStringBuilder.append(values[i]);
			extractionStringBuilder.append("\t");
		}
		Extraction extr = Extraction.deserialize(extractionStringBuilder.toString().trim());
		String annoLabel = values[10];
		boolean anno = false;
		if(annoLabel.equals("y")){
			anno = true;
		}
		else if(annoLabel.equals("n")){
			anno = false;
		}
		else{
			throw new IllegalArgumentException("Label must be either 'y' or 'n'");
		}
		return new ExtractionAnnotation(extr,anno);
	}
	
	public Extraction getExtraction(){
		return e;
	}
	
	public boolean getLabel(){
		return label;
	}
}
