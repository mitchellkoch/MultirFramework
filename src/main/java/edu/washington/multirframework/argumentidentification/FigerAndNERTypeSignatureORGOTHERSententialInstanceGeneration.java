package edu.washington.multirframework.argumentidentification;

import edu.washington.multir.util.TypeConstraintUtils;

/**
 * Populates <code>FigerAndNERTypeSignatureSententialInsantceGeneration</code> instance
 * with types of <code>GeneralType.ORGANIZATION</code> and <code>GeneralType.OTHER</code>
 * @author jgilme1
 *
 */
public class FigerAndNERTypeSignatureORGOTHERSententialInstanceGeneration extends
FigerAndNERTypeSignatureSententialInstanceGeneration {
	
	private static FigerAndNERTypeSignatureORGOTHERSententialInstanceGeneration instance = null;
	private FigerAndNERTypeSignatureORGOTHERSententialInstanceGeneration(String arg1Type, String arg2Type){
		super(arg1Type,arg2Type);
	}
	public static FigerAndNERTypeSignatureORGOTHERSententialInstanceGeneration getInstance(){
		if(instance == null) {
			instance = new FigerAndNERTypeSignatureORGOTHERSententialInstanceGeneration(TypeConstraintUtils.GeneralType.ORGANIZATION,
					TypeConstraintUtils.GeneralType.OTHER);
		}
		return instance;
		}
	
}
