package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignaturePERNUMSententialInstanceGeneration extends
FigerAndNERTypeSignatureSententialInstanceGeneration {
	
	private static FigerAndNERTypeSignaturePERNUMSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignaturePERNUMSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignaturePERNUMSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignaturePERNUMSententialInstanceGeneration(TypeConstraintUtils.GeneralType.PERSON,
					TypeConstraintUtils.GeneralType.NUMBER);
		}
		return instance;
		}
	
}
