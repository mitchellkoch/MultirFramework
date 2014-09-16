package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignatureOTHERLOCSententialInstanceGeneration 
extends FigerAndNERTypeSignatureSententialInstanceGeneration {

	private static FigerAndNERTypeSignatureOTHERLOCSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureOTHERLOCSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureOTHERLOCSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureOTHERLOCSententialInstanceGeneration(TypeConstraintUtils.GeneralType.OTHER,
					TypeConstraintUtils.GeneralType.LOCATION);
		}
		return instance;
		}
}