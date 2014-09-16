package edu.washington.multir.taggers;

import java.io.File;

import edu.knowitall.repr.sentence.Sentence;
import edu.knowitall.taggers.TaggerCollection;

public class Taggers {

	
	public static TaggerCollection<Sentence> educationTagger = 
			TaggerCollection.fromFile(new File(System.class.getResource("/edu/washington/multir/taggers/EducationalOrganizationTaggers").getPath()));
	
	public static TaggerCollection<Sentence> religionTagger = 
			TaggerCollection.fromFile(new File(System.class.getResource("/edu/washington/multir/taggers/ReligionTaggers").getPath()));
	
	public static TaggerCollection<Sentence> crimeTagger = 
			TaggerCollection.fromFile(new File(System.class.getResource("/edu/washington/multir/taggers/CrimeTaggers").getPath()));
	
	public static TaggerCollection<Sentence> jobTagger = 
			TaggerCollection.fromFile(new File(System.class.getResource("/edu/washington/multir/taggers/JobTitleTaggers").getPath()));
}
