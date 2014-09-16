package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignatureOTHERPERSententialInstanceGeneration 
extends FigerAndNERTypeSignatureSententialInstanceGeneration {

	private static FigerAndNERTypeSignatureOTHERPERSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureOTHERPERSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureOTHERPERSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureOTHERPERSententialInstanceGeneration(TypeConstraintUtils.GeneralType.OTHER,
					TypeConstraintUtils.GeneralType.PERSON);
		}
		return instance;
		}
}