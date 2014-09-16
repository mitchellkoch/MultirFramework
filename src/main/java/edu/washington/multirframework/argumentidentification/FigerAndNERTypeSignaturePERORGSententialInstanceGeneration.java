package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignaturePERORGSententialInstanceGeneration
		extends FigerAndNERTypeSignatureSententialInstanceGeneration {

	private static FigerAndNERTypeSignaturePERORGSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignaturePERORGSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignaturePERORGSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignaturePERORGSententialInstanceGeneration(TypeConstraintUtils.GeneralType.PERSON,
					TypeConstraintUtils.GeneralType.ORGANIZATION);
		}
		return instance;
		}
}
