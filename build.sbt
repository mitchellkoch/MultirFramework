organization := "edu.washington.cs.knowitall"

name := "multir-framework"

version := "0.3-SNAPSHOT"

fork := true

javaOptions in run += "-Xmx12G"

javaOptions in run += "-Djava.util.Arrays.useLegacyMergeSort=true"

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false}

pomExtra := (
  <url>https://github.com/knowitall/MultirFramework</url>
  <licenses>
    <license>
      <name>GNU General Public License Version 2</name>
      <url>http://www.gnu.org/licenses/gpl-2.0.txt</url>
    </license>
  </licenses>
 <scm>
   <url>https://github.com/knowitall/MultirFramework</url>
   <connection>scm:git://github.com/knowitall/MultirFramework.git</connection>
   <developerConnection>scm:git:git@github.com:knowitall/MultirFramework.git</developerConnection>
   <tag>HEAD</tag>
 </scm>
    <developers>
       <developer>
          <name>John Gilmer</name>
       </developer>
    </developers>)

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if(v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2") }


libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-io" % "1.3.2",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-cli" % "commons-cli" % "1.2",
  "edu.stanford.nlp" % "stanford-corenlp" % "1.3.5",
  "org.apache.derby" % "derby" % "10.10.1.1",
  "org.apache.derby" % "derbyclient" % "10.9.1.0",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-ner-models" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-postag-models" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-dcoref-models" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-parse-models" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-sutime-models" % "1.3.5",
  "edu.washington.cs.knowitall" % "reverb-core" % "1.4.3",
  "edu.washington.cs.knowitall.nlptools" % "nlptools-core_2.10" % "2.4.4",
  "edu.washington.cs.knowitall.nlptools" % "nlptools-chunk-opennlp_2.10" % "2.4.4",
  "edu.mit" % "jwi" % "2.2.3",
  "postgresql" % "postgresql" % "9.0-801.jdbc4",
  "edu.washington.cs.knowitall.nlptools" % "nlptools-wordnet-uw_2.10" % "2.4.4",
  "org.apache.hadoop" % "hadoop-core" % "0.20.2",
  "com.cedarsoftware" % "json-io" % "2.6.0",
  "com.google.code.externalsortinginjava" % "externalsortinginjava" % "0.1.9",
  "edu.washington.cs.knowitall.taggers" % "taggers-core_2.10" % "0.4" excludeAll(ExclusionRule(organization = "com.googlecode.clearnlp")))
  

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
