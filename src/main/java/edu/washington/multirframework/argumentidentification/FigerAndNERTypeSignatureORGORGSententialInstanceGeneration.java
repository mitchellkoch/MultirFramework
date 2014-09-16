package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

/**
 * Populates <code>FigerAndNERTypeSignatureSententialInsantceGeneration</code> instance
 * with types of <code>GeneralType.ORGANIZATION</code> and <code>GeneralType.ORGANIZATION</code>
 * @author jgilme1
 *
 */
public class FigerAndNERTypeSignatureORGORGSententialInstanceGeneration extends
FigerAndNERTypeSignatureSententialInstanceGeneration {
	
	private static FigerAndNERTypeSignatureORGORGSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureORGORGSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureORGORGSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureORGORGSententialInstanceGeneration(TypeConstraintUtils.GeneralType.ORGANIZATION,
					TypeConstraintUtils.GeneralType.ORGANIZATION);
		}
		return instance;
		}
	
}
