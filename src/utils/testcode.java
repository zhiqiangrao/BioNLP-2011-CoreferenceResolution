package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import utils.CorefFile.Relation;

public class testcode {
	
	private static void statistic() {
		File txtdir = new File("./data/BioNLP-ST_2011_coreference_development_data");
		if (txtdir.isDirectory()) {
			File[] txtfiles = txtdir.listFiles(new FileFilterImpl(".txt"));
			Arrays.sort(txtfiles);
			for (File txtfile : txtfiles) {
			    File a1file = new File(txtfile.getPath().substring(0, txtfile.getPath().lastIndexOf(".")) + ".a1");
			    ProteinFile.getProteins(a1file);
			    File a2file = new File(txtfile.getPath().substring(0, txtfile.getPath().lastIndexOf(".")) + ".a2");
			    CorefFile.readCorefFile(a2file);
			    
			    for (Relation r : CorefFile.relations) {
			    	if (r.protIds != null) {
				    	if (r.anaTrigger.name.equalsIgnoreCase("which")
				    			|| r.anaTrigger.name.equalsIgnoreCase("that")
				    			|| r.anaTrigger.name.equalsIgnoreCase("who")
				    			|| r.anaTrigger.name.equalsIgnoreCase("whom")
				    			|| r.anaTrigger.name.equalsIgnoreCase("whose"))
				    		continue;
				    		//System.out.println(r.anteTrigger.name);
				    	if (r.anaTrigger.name.equalsIgnoreCase("it")
				    			|| r.anaTrigger.name.equalsIgnoreCase("itself")
				    			|| r.anaTrigger.name.equalsIgnoreCase("they")
				    			|| r.anaTrigger.name.equalsIgnoreCase("them")
				    			|| r.anaTrigger.name.equalsIgnoreCase("themselves")
				    			|| r.anaTrigger.name.equalsIgnoreCase("its")
				    			|| r.anaTrigger.name.equalsIgnoreCase("their"))
				    		continue;
				    		//System.out.println(r.anteTrigger.name);
				    	if (r.anaTrigger.name.startsWith("this")
				    			|| r.anaTrigger.name.startsWith("these")
				    			|| r.anaTrigger.name.startsWith("those")
				    			|| r.anaTrigger.name.startsWith("the ")
				    			|| r.anaTrigger.name.startsWith("both")
				    			|| r.anaTrigger.name.startsWith("each")
				    			|| r.anaTrigger.name.startsWith("a")
				    			|| r.anaTrigger.name.startsWith("an")
				    			)
				    		System.out.println(r.anaTrigger.name + "\t:\t" + r.anteTrigger.name);
				    		//System.out.println(r.anteTrigger.name);
			    	}
			    }
			}
		}
	}
	
	private static void enjuparse() {
		ProcessBuilder pb = new ProcessBuilder("/home/raines/workspace/BioNLPCR/tools/enju/share/enju2ptb/myconvert");
		try {
			Process process = pb.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done!");
		
		/*Runtime rt = Runtime.getRuntime();
		try {
			Process process = rt.exec("/home/raines/workspace/BioNLPCR/tools/enju/share/enju2ptb/myconvert");
			OutputStreamWriter outStreamWriter = new OutputStreamWriter(process.getOutputStream());
			BufferedWriter inputWriter = new BufferedWriter(outStreamWriter);
			InputStreamReader inStreamReader = new InputStreamReader(process.getInputStream());
			BufferedReader outputReader = new BufferedReader(inStreamReader);
			InputStreamReader errorStreamReader = new InputStreamReader(process.getErrorStream());
			BufferedReader errorReader = new BufferedReader(errorStreamReader);
			System.out.println("Done!");
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		
	}
	
	private static void parse() {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
	    //props.put("tokenize.language", "English");
	    //props.put("ssplit.newlineIsSentenceBreak", "always");
	    //props.put("ssplit.eolonly", "true");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String text = "In the majority of patients with advanced RCC, peripheral blood T cells express TCRzeta and p56(lck), and in a subset, reduced levels of these TCRzeta associated molecules are seen that may increase during cytokine-based therapy.";
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    Tree t = document.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
	    System.out.println("ok");
	}
	
	public static void main(String[] args) {
		statistic();
		//enjuparse();
		//parse();
	}
	
}