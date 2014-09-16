package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignatureORGNUMSententialInstanceGeneration extends
FigerAndNERTypeSignatureSententialInstanceGeneration {
	
	private static FigerAndNERTypeSignatureORGNUMSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureORGNUMSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureORGNUMSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureORGNUMSententialInstanceGeneration(TypeConstraintUtils.GeneralType.ORGANIZATION,
					TypeConstraintUtils.GeneralType.NUMBER);
		}
		return instance;
		}
	
}