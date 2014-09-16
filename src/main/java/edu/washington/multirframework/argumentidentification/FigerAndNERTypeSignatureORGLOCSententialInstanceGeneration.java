package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

/**
 * Populates <code>FigerAndNERTypeSignatureSententialInsantceGeneration</code> instance
 * with types of <code>GeneralType.ORGANIZATION</code> and <code>GeneralType.LOCATION</code>
 * @author jgilme1
 *
 */
public class FigerAndNERTypeSignatureORGLOCSententialInstanceGeneration extends
FigerAndNERTypeSignatureSententialInstanceGeneration {
	
	private static FigerAndNERTypeSignatureORGLOCSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureORGLOCSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureORGLOCSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureORGLOCSententialInstanceGeneration(TypeConstraintUtils.GeneralType.ORGANIZATION,
					TypeConstraintUtils.GeneralType.LOCATION);
		}
		return instance;
		}
	
}
