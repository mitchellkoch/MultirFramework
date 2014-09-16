package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignatureLOCOTHERSententialInstanceGeneration 
extends FigerAndNERTypeSignatureSententialInstanceGeneration {

	private static FigerAndNERTypeSignatureLOCOTHERSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureLOCOTHERSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureLOCOTHERSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureLOCOTHERSententialInstanceGeneration(TypeConstraintUtils.GeneralType.LOCATION,
					TypeConstraintUtils.GeneralType.OTHER);
		}
		return instance;
		}
}