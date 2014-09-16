package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignatureOTHEROTHERSententialInstanceGeneration 
extends FigerAndNERTypeSignatureSententialInstanceGeneration {

	private static FigerAndNERTypeSignatureOTHEROTHERSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureOTHEROTHERSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureOTHEROTHERSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureOTHEROTHERSententialInstanceGeneration(TypeConstraintUtils.GeneralType.OTHER,
					TypeConstraintUtils.GeneralType.OTHER);
		}
		return instance;
		}
}