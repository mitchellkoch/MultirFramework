package edu.washington.multir.experiment;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignatureORGPERSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePERLOCSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.FigerAndNERTypeSignaturePERPERSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.NERArgumentIdentification;
import edu.washington.multirframework.argumentidentification.NERRelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multirframework.corpus.SentFreebaseNotableTypeInformation;
import edu.washington.multirframework.corpus.SentInformationI;
import edu.washington.multirframework.corpus.SentNamedEntityLinkingInformation;
import edu.washington.multirframework.corpus.TokenChunkInformation;
import edu.washington.multirframework.corpus.TokenInformationI;
import edu.washington.multir.data.TypeSignatureRelationMap;
import edu.washington.multir.util.FigerTypeUtils;
import edu.washington.multirframework.distantsupervision.NegativeExampleCollectionByRatio;
import edu.washington.multirframework.featuregeneration.DefaultFeatureGeneratorGeneralizedMinusDirPath;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class Partitioned_DefaultDS_GeneralizedOneMinusDir {

	public static void main(String [] args) throws SQLException, IOException, InterruptedException, ExecutionException{	
		
		 String expPath = "/scratch2/usr/jgilme1/MultirExperimentsExperiments/";
		 String expName = Partitioned_DefaultDS_GeneralizedOneMinusDir.class.getSimpleName();
		 String fullPath = expPath+expName+"/";
		
		Experiment e = new Experiment();
		e.setAi(NERArgumentIdentification.getInstance());
		e.setFg(new DefaultFeatureGeneratorGeneralizedMinusDirPath());
		e.setRm(NERRelationMatching.getInstance());
		e.setNec(NegativeExampleCollectionByRatio.getInstance(4.0));
		
		
		e.setKb(new KnowledgeBase("/projects/WebWare6/Multir/MultirSystem/files/kbfiles/fb-rels-all.tsv.gz",
				"/projects/WebWare6/Multir/MultirSystem/files/kbfiles/fb-entities.tsv",
				"/projects/WebWare6/Multir/MultirSystem/files/targetRelations/partitionRelations.txt"));
		
		
		/*
		e.setKb(new KnowledgeBase("/scratch2/code/multir-reimplementation/MultirSystem/fb-rels-10mill.tsv",
				"/projects/WebWare6/Multir/MultirSystem/files/kbfiles/fb-entities.tsv",
				"/projects/WebWare6/Multir/MultirSystem/files/targetRelations/partitionRelations.txt"));
				*/
		
		
		
		List<SententialInstanceGeneration> sigs = new ArrayList<>();
		sigs.add(FigerAndNERTypeSignatureORGPERSententialInstanceGeneration.getInstance());
		sigs.add(FigerAndNERTypeSignaturePERPERSententialInstanceGeneration.getInstance());
		sigs.add(FigerAndNERTypeSignaturePERLOCSententialInstanceGeneration.getInstance());
		e.setSigs(sigs);
		
		
		
		CustomCorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
		List<TokenInformationI> tokenInformationList = new ArrayList<>();
		tokenInformationList.add(new TokenChunkInformation());
		List<SentInformationI> sentInformationList = new ArrayList<>();
		sentInformationList.add(new SentNamedEntityLinkingInformation());
		sentInformationList.add(new SentFreebaseNotableTypeInformation());
		
		cis.addSentenceInformation(sentInformationList);
		cis.addTokenInformation(tokenInformationList);
		e.setCis(cis);
		e.setCorpusPath("/scratch2/usr/jgilme1/FullCorpus");
		e.setTestDocumentsFile("/projects/WebWare6/Multir/MultirSystem/files/testDocuments");
		
		List<String> dsFiles = new ArrayList<String>();
		
		dsFiles.add("/projects/WebWare6/Multir/MultirSystem/files/distantSupervision/TypePartitionDistantSupervision/Baseline-ORGPER-DS");
		dsFiles.add("/projects/WebWare6/Multir/MultirSystem/files/distantSupervision/TypePartitionDistantSupervision/Baseline-PERPER-DS");
		dsFiles.add("/projects/WebWare6/Multir/MultirSystem/files/distantSupervision/TypePartitionDistantSupervision/Baseline-PERLOC-DS");
		
		/*
		dsFiles.add("ORGPER-DS");
		dsFiles.add("PERPER-DS");
		dsFiles.add("PERLOC-DS");
		*/
		e.setDSFiles(dsFiles);
		
		TypeSignatureRelationMap.init("/projects/WebWare6/Multir/MultirSystem/files/distantSupervision/TypePartitionDistantSupervision/TargetRelationTypeSignatureMap");
		
		List<String> featureFiles = new ArrayList<>();
		featureFiles.add(fullPath+"featuresORGPER");
		featureFiles.add(fullPath+"featuresPERPER");
		featureFiles.add(fullPath+"featuresPERLOC");
		e.setFeatureFiles(featureFiles);
		
		List<String> modelFiles = new ArrayList<String>();
		modelFiles.add(fullPath+"modelORGPER");
		modelFiles.add(fullPath +"modelPERPER");
		modelFiles.add(fullPath +"modelPERLOC");
		e.setMultirDir(modelFiles);
		
		e.setTrain(true);
		
		FigerTypeUtils.init();
		e.runExperiment();
		FigerTypeUtils.close();
	}
}
