package edu.washington.multir.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

import edu.washington.multir.data.TypeSignatureRelationMap;
import edu.washington.multir.development.Preprocess;
import edu.washington.multir.development.TrainModel;
import edu.washington.multir.distantsupervision.FeedbackDistantSupervision;
import edu.washington.multir.distantsupervision.FeedbackNegativeDistantSupervision;
import edu.washington.multir.distantsupervision.MultiModelDistantSupervision;
import edu.washington.multir.util.FigerTypeUtils;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.DocumentInformationI;
import edu.washington.multirframework.corpus.SentInformationI;
import edu.washington.multirframework.corpus.TokenInformationI;
import edu.washington.multirframework.distantsupervision.DistantSupervision;
import edu.washington.multirframework.distantsupervision.NegativeExampleCollection;
import edu.washington.multirframework.featuregeneration.FeatureGeneration;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class Experiment {
	private String corpusPath;
	private String typeRelMapPath;
	private ArgumentIdentification ai;
	private FeatureGenerator fg;
	private List<SententialInstanceGeneration> sigs;
	private List<String> DSFiles;
	private List<String> oldFeatureFiles;
	private List<String> featureFiles;
	private List<String> multirDirs;
	private List<String> oldMultirDirs;
	private RelationMatching rm;
	private NegativeExampleCollection nec;
	private KnowledgeBase kb;
	private String testDocumentsFile;
	private CorpusInformationSpecification cis;
	private String evalOutputName;
	private boolean train = false;
	private boolean useFiger = false;
	private Integer featureThreshold = 2;
	private boolean strictNegativeGeneration = false;
	
	public Experiment(){}
	public Experiment(String propertiesFile) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException{

		String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
		Map<String,Object> properties = JsonReader.jsonToMaps(jsonProperties);
		
		corpusPath = getStringProperty(properties,"corpusPath");
		evalOutputName = getStringProperty(properties,"evalOutputName");
		testDocumentsFile = getStringProperty(properties,"testDocumentsFile");
		String train = getStringProperty(properties,"train");
		double necRatio = 4.0;
		if(train!=null){
			if(train.equals("false")){
				this.train = false;
			}
			else if(train.equals("true")){
				this.train = true;
			}
		}
		
		String strictNegativeGenerationString = getStringProperty(properties,"strictNegativeGeneration");
		if(strictNegativeGenerationString != null){
			if(strictNegativeGenerationString.equals("true")){
				strictNegativeGeneration = true;
			}
		}
		
		String featThresholdString = getStringProperty(properties,"featureThreshold");
		if(featThresholdString!=null){
			this.featureThreshold = Integer.parseInt(featThresholdString);
		}
		
		String useFiger = getStringProperty(properties,"useFiger");
		if(useFiger!=null){
			if(useFiger.equals("true")){
				this.useFiger = true;
			}
		}
		String featureGeneratorClass = getStringProperty(properties,"fg");
		if(featureGeneratorClass != null){
			fg = (FeatureGenerator) ClassLoader.getSystemClassLoader().loadClass(featureGeneratorClass).newInstance();
		}
		
		String aiClass = getStringProperty(properties,"ai");
		if(aiClass != null){
			ai = (ArgumentIdentification) ClassLoader.getSystemClassLoader().loadClass(aiClass).getMethod("getInstance").invoke(null);
		}
		
		String rmClass = getStringProperty(properties,"rm");
		if(rmClass != null){
			rm = (RelationMatching) ClassLoader.getSystemClassLoader().loadClass(rmClass).getMethod("getInstance").invoke(null);
		}
		
		String necRatioString = getStringProperty(properties,"necRatio");
		if(necRatioString!=null){
			necRatio = Double.parseDouble(necRatioString);
		}
		
		String necClass = getStringProperty(properties,"nec");
		if(necClass != null){
			nec = (NegativeExampleCollection) ClassLoader.getSystemClassLoader().loadClass(necClass).getMethod("getInstance", double.class).invoke(null,necRatio);
		}
		
		String kbRelFile = getStringProperty(properties,"kbRelFile");
		String kbEntityFile = getStringProperty(properties,"kbEntityFile");
		String targetRelFile = getStringProperty(properties,"targetRelFile");
		if(kbRelFile!=null && kbEntityFile!=null && targetRelFile != null){
			kb = new KnowledgeBase(kbRelFile,kbEntityFile,targetRelFile);
		}
		
		List<String> sigClasses = getListProperty(properties,"sigs");
		sigs = new ArrayList<>();
		for(String sigClass : sigClasses){
			sigs.add((SententialInstanceGeneration)ClassLoader.getSystemClassLoader().loadClass(sigClass).getMethod("getInstance").invoke(null));
		}
		
		List<String> dsFileNames = getListProperty(properties,"dsFiles");
		DSFiles = new ArrayList<>();
		for(String dsFileName : dsFileNames){
			DSFiles.add(dsFileName);
		}
		
		List<String> oldFeatureFileNames = getListProperty(properties,"oldFeatureFiles");
		oldFeatureFiles = new ArrayList<>();
		for(String oldFeatureFileName : oldFeatureFileNames){
			oldFeatureFiles.add(oldFeatureFileName);
		}
		
		List<String> featureFileNames = getListProperty(properties,"featureFiles");
		featureFiles = new ArrayList<>();
		for(String featureFileName : featureFileNames){
			featureFiles.add(featureFileName);
		}
		
		List<String> oldMultirDirNames = getListProperty(properties,"oldModels");
		oldMultirDirs = new ArrayList<>();
		for(String oldMultirDirName : oldMultirDirNames){
			oldMultirDirs.add(oldMultirDirName);
		}
		
		multirDirs = new ArrayList<>();
		List<String> multirDirNames = getListProperty(properties,"models");
		for(String multirDirName : multirDirNames){
			multirDirs.add(multirDirName);
		}
		
		cis = new CustomCorpusInformationSpecification();
		
		String altCisString = getStringProperty(properties,"cis");
		if(altCisString != null){
			cis = (CustomCorpusInformationSpecification)ClassLoader.getSystemClassLoader().loadClass(altCisString).newInstance();
		}
		
		//CorpusInformationSpecification
		List<String> tokenInformationClassNames = getListProperty(properties,"ti");
		List<TokenInformationI> tokenInfoList = new ArrayList<>();
		for(String tokenInformationClassName : tokenInformationClassNames){
			tokenInfoList.add((TokenInformationI)ClassLoader.getSystemClassLoader().loadClass(tokenInformationClassName).newInstance());
		}
		
		List<String> sentInformationClassNames = getListProperty(properties,"si");
		List<SentInformationI> sentInfoList = new ArrayList<>();
		for(String sentInformationClassName : sentInformationClassNames){
			sentInfoList.add((SentInformationI)ClassLoader.getSystemClassLoader().loadClass(sentInformationClassName).newInstance());
		}
		
		List<String> docInformationClassNames = getListProperty(properties,"di");
		List<DocumentInformationI> docInfoList = new ArrayList<>();
		for(String docInformationClassName : docInformationClassNames){
			docInfoList.add((DocumentInformationI)ClassLoader.getSystemClassLoader().loadClass(docInformationClassName).newInstance());
		}
		
		CustomCorpusInformationSpecification ccis = (CustomCorpusInformationSpecification)cis;
		ccis.addDocumentInformation(docInfoList);
		ccis.addTokenInformation(tokenInfoList);
		ccis.addSentenceInformation(sentInfoList);
		
		
		typeRelMapPath = getStringProperty(properties,"typeRelMap");
		
	}
	
	private List<String> getListProperty(Map<String, Object> properties,
			String string) {
		if(properties.containsKey(string)){
			JsonObject obj = (JsonObject) properties.get(string);
			List<String> returnValues = new ArrayList<>();
			for(Object o : obj.getArray()){
				returnValues.add(o.toString());
			}
			return returnValues;
		}
		return new ArrayList<>();
	}
	private String getStringProperty(Map<String,Object> properties, String str) {
		if(properties.containsKey(str)){
			if(properties.get(str)== null){
				return null;
			}
			else{
				return properties.get(str).toString();
			}
		}
		return null;
	}
	public void runExperiment() throws SQLException, IOException, InterruptedException, ExecutionException{
		if(useFiger){
			FigerTypeUtils.init();
		}
		if(typeRelMapPath!=null){
			TypeSignatureRelationMap.init(typeRelMapPath);
		}
		
		Corpus corpus = new Corpus(corpusPath, cis, true);
		if(train){
		 corpus.setCorpusToTrain(testDocumentsFile);
		}
		else{
		  corpus.setCorpusToDefault();
		}
		
		if(!filesExist(multirDirs)){
			for(String s : multirDirs){
				File f = new File(s);
				f.mkdirs();
			}
		}
		
		if(oldFeatureFiles != null){
			if(oldFeatureFiles.size() > 0){
			 runFeedbackExperiment(corpus);
			}
		}
	
		boolean runDS = !filesExist(DSFiles);
		boolean runFG = false;
		
		//if distant supervision hasnt been run yet
		if(runDS){
			System.err.println("Running DS");
			runFG = true;
			if(DSFiles.size() > 1){
				MultiModelDistantSupervision mmds = new MultiModelDistantSupervision(ai, DSFiles, sigs, rm, nec, strictNegativeGeneration);
				mmds.run(kb, corpus);
			}
			else{
				DistantSupervision ds = new DistantSupervision(ai, sigs.get(0), rm, nec);
				ds.run(DSFiles.get(0), kb, corpus);
			}
		}
		
		if(!runFG){
			runFG = !filesExist(featureFiles);
		}
		
		//if feature generation hasnt been run yet
		if(runFG){
			System.err.println("Running FG");

			FeatureGeneration fGeneration = new FeatureGeneration(fg);
			fGeneration.run(DSFiles, featureFiles, corpus, cis);
		}

		//do average training run
		Preprocess.FEATURE_THRESHOLD = this.featureThreshold;
		TrainModel.run(featureFiles,multirDirs,10);
		
		if(useFiger){
			FigerTypeUtils.close();
		}
	}



	private void runFeedbackExperiment(Corpus corpus) throws SQLException, IOException, InterruptedException, ExecutionException {
		
		List<String> feedbackDSFiles = new ArrayList<>();
		int count = 1;
		for(String s : featureFiles){
			File f = new File(s);
			File dir = f.getParentFile();
			feedbackDSFiles.add(dir.toString()+"/FBDS"+count);
			count++;
		}
		List<String> feedbackFeatureFiles = new ArrayList<>();
		for(String s: feedbackDSFiles){
			feedbackFeatureFiles.add(s+".features");
		}
		
		FeedbackNegativeDistantSupervision.run(kb, oldMultirDirs, feedbackDSFiles, sigs, fg, ai, corpus);

		FeatureGeneration fGeneration = new FeatureGeneration(fg);
		fGeneration.run(feedbackDSFiles, feedbackFeatureFiles, corpus, cis);
		
		for(int i =0; i < featureFiles.size(); i++){
			//read feedbackFeatureFiles, find NA annotations and write them to feature file
			File feedbackFeatureFile = new File(feedbackFeatureFiles.get(i));
			File oldFeatureFile = new File(oldFeatureFiles.get(i));
			File newFeatureFile = new File(featureFiles.get(i));
			BufferedWriter bw = new BufferedWriter(new FileWriter(newFeatureFile));
			BufferedReader oldFeatureReader = new BufferedReader(new FileReader(oldFeatureFile));
			BufferedReader feedbackFeatureReader = new BufferedReader(new FileReader(feedbackFeatureFile));
			
			String nextLine;
			while((nextLine = feedbackFeatureReader.readLine())!=null){
				String[] values = nextLine.split("\t");
				String rel = values[3];
				if(rel.equals("NA")){
					bw.write(nextLine+"\n");
				}
			}
			
			String prevLine = null;
			while((nextLine=oldFeatureReader.readLine())!=null){
				if(prevLine!=null) bw.write(prevLine+"\n");
				prevLine = nextLine;
			}
			if(prevLine != null) bw.write(prevLine);
			
			
			bw.close();
			oldFeatureReader.close();
			feedbackFeatureReader.close();
		}
		
		
	}



	private boolean filesExist(List<String> dsFiles) {
		for(String s : dsFiles){
			File f = new File(s);
			if(!f.exists()){
				System.err.println(s + " File does not exist!Need To Generate it");
				return false;
			}
		}
		return true;
	}



	public ArgumentIdentification getAi() {
		return ai;
	}



	public void setAi(ArgumentIdentification ai) {
		this.ai = ai;
	}



	public FeatureGenerator getFg() {
		return fg;
	}



	public void setFg(FeatureGenerator fg) {
		this.fg = fg;
	}



	public List<SententialInstanceGeneration> getSigs() {
		return sigs;
	}



	public void setSigs(List<SententialInstanceGeneration> sigs) {
		this.sigs = sigs;
	}



	public List<String> getDSFiles() {
		return DSFiles;
	}



	public void setDSFiles(List<String> dSFiles) {
		DSFiles = dSFiles;
	}



	public List<String> getFeatureFiles() {
		return featureFiles;
	}



	public void setFeatureFiles(List<String> featureFiles) {
		this.featureFiles = featureFiles;
	}



	public List<String> getMultirDir() {
		return multirDirs;
	}



	public void setMultirDir(List<String> multirDirs) {
		this.multirDirs = multirDirs;
	}



	public List<String> getOldMultirDirs() {
		return oldMultirDirs;
	}



	public void setOldMultirDir(List<String> oldMultirDirs) {
		this.oldMultirDirs = oldMultirDirs;
	}



	public List<String> getOldDSFiles() {
		return oldFeatureFiles;
	}



	public void setOldDSFiles(List<String> oldDSFiles) {
		this.oldFeatureFiles = oldDSFiles;
	}



	public String getTestDocumentsFile() {
		return testDocumentsFile;
	}



	public void setTestDocumentsFile(String testDocumentsFile) {
		this.testDocumentsFile = testDocumentsFile;
	}
	
	
	
	public CorpusInformationSpecification getCis() {
		return cis;
	}



	public void setCis(CorpusInformationSpecification cis) {
		this.cis = cis;
	}
	
	public RelationMatching getRm() {
		return rm;
	}



	public void setRm(RelationMatching rm) {
		this.rm = rm;
	}
	
	public String getCorpusPath() {
		return corpusPath;
	}



	public void setCorpusPath(String corpusPath) {
		this.corpusPath = corpusPath;
	}
	
	public List<String> getOldFeatureFiles() {
		return oldFeatureFiles;
	}



	public void setOldFeatureFiles(List<String> oldFeatureFiles) {
		this.oldFeatureFiles = oldFeatureFiles;
	}



	public List<String> getMultirDirs() {
		return multirDirs;
	}



	public void setMultirDirs(List<String> multirDirs) {
		this.multirDirs = multirDirs;
	}



	public NegativeExampleCollection getNec() {
		return nec;
	}



	public void setNec(NegativeExampleCollection nec) {
		this.nec = nec;
	}



	public KnowledgeBase getKb() {
		return kb;
	}



	public void setKb(KnowledgeBase kb) {
		this.kb = kb;
	}



	public String getEvalOutputName() {
		return evalOutputName;
	}



	public void setEvalOutputName(String evalOutputName) {
		this.evalOutputName = evalOutputName;
	}



	public void setOldMultirDirs(List<String> oldMultirDirs) {
		this.oldMultirDirs = oldMultirDirs;
	}
	




	public boolean isTrain() {
		return train;
	}



	public void setTrain(boolean train) {
		this.train = train;
	}
	
	public static void main(String [] args) throws IOException, InstantiationException, 
	IllegalAccessException, ClassNotFoundException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, SQLException, 
	InterruptedException, ExecutionException{
		Experiment e = new Experiment(args[0]);
		e.runExperiment();
	}


}
