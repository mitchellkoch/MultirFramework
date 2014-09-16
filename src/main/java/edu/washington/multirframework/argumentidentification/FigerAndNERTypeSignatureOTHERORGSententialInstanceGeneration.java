package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignatureOTHERORGSententialInstanceGeneration 
extends FigerAndNERTypeSignatureSententialInstanceGeneration {

	private static FigerAndNERTypeSignatureOTHERORGSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureOTHERORGSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureOTHERORGSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureOTHERORGSententialInstanceGeneration(TypeConstraintUtils.GeneralType.OTHER,
					TypeConstraintUtils.GeneralType.ORGANIZATION);
		}
		return instance;
		}
}