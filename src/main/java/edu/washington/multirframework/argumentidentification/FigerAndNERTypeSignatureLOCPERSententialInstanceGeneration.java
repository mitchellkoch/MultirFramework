package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignatureLOCPERSententialInstanceGeneration 
extends FigerAndNERTypeSignatureSententialInstanceGeneration {

	private static FigerAndNERTypeSignatureLOCPERSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureLOCPERSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureLOCPERSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureLOCPERSententialInstanceGeneration(TypeConstraintUtils.GeneralType.LOCATION,
					TypeConstraintUtils.GeneralType.PERSON);
		}
		return instance;
		}
}
