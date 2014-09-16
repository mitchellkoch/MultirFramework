package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignaturePERDATESententialInstanceGeneration extends
		FigerAndNERTypeSignatureSententialInstanceGeneration {
	
	private static FigerAndNERTypeSignaturePERDATESententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignaturePERDATESententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignaturePERDATESententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignaturePERDATESententialInstanceGeneration(TypeConstraintUtils.GeneralType.PERSON,
					TypeConstraintUtils.GeneralType.DATE);
		}
		return instance;
		}
	
}
