package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

public class FigerAndNERTypeSignatureLOCLOCSententialInstanceGeneration 
extends FigerAndNERTypeSignatureSententialInstanceGeneration {

	private static FigerAndNERTypeSignatureLOCLOCSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureLOCLOCSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureLOCLOCSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureLOCLOCSententialInstanceGeneration(TypeConstraintUtils.GeneralType.LOCATION,
					TypeConstraintUtils.GeneralType.LOCATION);
		}
		return instance;
		}
}
