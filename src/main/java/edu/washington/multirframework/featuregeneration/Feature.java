package edu.washington.multirframework.featuregeneration;

public class Feature {
	
	String featureString;
	
	public Feature(String featureString){
		this.featureString = featureString;
	}
	
	@Override
	public String toString(){
		return featureString;
	}
	
	public void setFeature(String featureString){
		this.featureString = featureString;
	}

}
