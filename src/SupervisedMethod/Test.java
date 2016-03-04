package SupervisedMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

import RuleBasedMethod.RuleBasedExtraction;
import utils.CorefFile;
import utils.CorefFile.Relation;
import utils.CorefFile.Trigger;
import utils.FileFilterImpl;
import utils.FileUtil;
import utils.ProteinFile;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Test {
	
	private final static Logger logger = Logger.getLogger(Train.class.getName());
	
	public static void main(String[] args) {
		
		test();
		
	}

	private static void test() {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma");
	    props.put("tokenize.language", "English");
	    props.put("ssplit.newlineIsSentenceBreak", "always");
	    props.put("ssplit.eolonly", "true");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    File txtdir = new File("./data/BioNLP-ST_2011_coreference_development_data");
	    File parsedir = new File("./data/enju/devel");
	    File ssdir = new File("./data/geniass/devel");
	    File resultdir = new File("./result");
	    if (!resultdir.exists())
	    	resultdir.mkdirs();
	    
	    LibSVM lsr = new LibSVM();
	    lsr.loadModel(new File("./model/train.relave.model"));
	
	    LibSVM lsp = new LibSVM();
	    lsp.loadModel(new File("./model/train.personal.model"));
	
	    LibSVM lsd = new LibSVM();
	    lsd.loadModel(new File("./model/train.dnp.model"));
	    
	    extractFromFile(pipeline, txtdir, parsedir, ssdir, resultdir, lsr, lsp, lsd);
	    
	    //LibSVM ls = new LibSVM();
	    //ls.loadModel(new File("./model/train.model"));
	    //extractFromFile(pipeline, txtdir, parsedir, ssdir, ls, ls, ls);
   
	}

	private static void extractFromFile(StanfordCoreNLP pipeline, File txtdir, File parsedir, File ssdir, File resultdir, LibSVM lsr, LibSVM lsp, LibSVM lsd) {
		if (txtdir.isDirectory()) {
			File[] txtfiles = txtdir.listFiles(new FileFilterImpl(".txt"));
			Arrays.sort(txtfiles);
			int filenum = 0;
			for (File txtfile : txtfiles) {
				File parsefile = new File(parsedir.getPath() + "/" + FileUtil.removeFileNameExtension(txtfile.getName()) + ".ptb");
				File ssfile = new File(ssdir.getPath() + "/" + FileUtil.removeFileNameExtension(txtfile.getName()) + ".ss");
				logger.info("Extracting from file: " + txtfile.getName() + " " + (++filenum));
				extractFromSingleFile(pipeline, txtfile, parsefile, ssfile, resultdir, lsr, lsp, lsd);
			}
		}
	}

	private static void extractFromSingleFile(StanfordCoreNLP pipeline, File txtfile, File parsefile, File ssfile, File resultdir, LibSVM lsr, LibSVM lsp, LibSVM lsd) {
		
		String text = FileUtil.readFile(ssfile);
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    RuleBasedExtraction.setEnjuParseTree(document, parsefile);
	    File a1file = new File(FileUtil.removeFileNameExtension(txtfile.getPath()) + ".a1");
	    ProteinFile.getProteins(a1file);
	    File a2file = new File(FileUtil.removeFileNameExtension(txtfile.getPath()) + ".a2");
	    CorefFile.readCorefFile(a2file);
	
	    ArrayList<Trigger> relatprons = new ArrayList<Trigger>();
	    ArrayList<Trigger> persprons = new ArrayList<Trigger>();
	    ArrayList<Trigger> dnps = new ArrayList<Trigger>();
	    ArrayList<Trigger> nps = new ArrayList<Trigger>();
	    RuleBasedExtraction.extractPredictedMentions(document, relatprons,  persprons, dnps, nps);
	
	    ArrayList<Trigger> alltriggers = new ArrayList<Trigger>();
	    alltriggers.addAll(relatprons);
	    alltriggers.addAll(persprons);
	    alltriggers.addAll(dnps);
	    alltriggers.addAll(nps);
	    ArrayList<Trigger> sortedtriggers = RuleBasedExtraction.setTriggerIdAndMentionId(alltriggers);
	
	    ArrayList<Relation> relatrelations = new ArrayList<Relation>();
	    ArrayList<Trigger> relatcands = new ArrayList<Trigger>();
	    relatcands.addAll(nps);
	    relatcands.addAll(dnps);
	    extractRelativePronounRelations(document, relatprons, relatcands, lsr, relatrelations);
	
	    ArrayList<Relation> persrelations = new ArrayList<Relation>();
	    ArrayList<Trigger> perscands = new ArrayList<Trigger>();
	    perscands.addAll(nps);
	    perscands.addAll(dnps);
	    extractPersonalPronounRelations(document, persprons, perscands, lsp, persrelations);
	
	    ArrayList<Relation> dnprelations = new ArrayList<Relation>();
	    ArrayList<Trigger> dnpcands = nps;
	    extractDefiniteNPRelations(document, dnps, dnpcands, lsd, dnprelations);
	    
	    ArrayList<Relation> allrelations = new ArrayList<Relation>();
	    allrelations.addAll(relatrelations);
	    allrelations.addAll(persrelations);
	    allrelations.addAll(dnprelations);
	    RuleBasedExtraction.writeResult(txtfile, resultdir, sortedtriggers, allrelations);
	}

	private static void extractRelativePronounRelations(Annotation document,
			ArrayList<Trigger> relatprons, ArrayList<Trigger> relatcands, LibSVM lsr, ArrayList<Relation> relatrelations) {
		
		for (Trigger trigger : relatprons) {
			
			// 句子窗口0
			ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    for (Trigger cand : relatcands) {
		    	if (cand.mention.sentNum == trigger.mention.sentNum && cand.end < trigger.start)
		    		cands.add(cand);
		    }
		    
		    Trigger anstrigger = null;
		    for (Trigger cand : cands) {
		    	Instance inst = new Instance();
	    		inst.setInstance(document, trigger, cand);
		    	if (lsr.predict(inst) == 1) {
		    		if (anstrigger == null)
		    			anstrigger = cand;
		    		else if (cand.id > anstrigger.id)
		    			anstrigger = cand;
		    	}
		    }
		    if (anstrigger != null) {
		    	if (!ProteinFile.containsProtein(anstrigger))
		    		continue;
		    	
		    	Relation relation = new Relation();
		    	relatrelations.add(relation);
		    	relation.anaTrigger = trigger;
		    	relation.anteTrigger = anstrigger;
		    }
		}
	}

	private static void extractPersonalPronounRelations(Annotation document,
			ArrayList<Trigger> persprons, ArrayList<Trigger> perscands, LibSVM lsp, ArrayList<Relation> persrelations) {

		for (Trigger trigger : persprons) {
			
			// 句子窗口1
			ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    for (Trigger cand : perscands) {
		    	if (trigger.mention.sentNum - cand.mention.sentNum <= 1 && cand.end < trigger.start)
		    		cands.add(cand);
		    }
		    
		    Trigger anstrigger = null;
		    for (Trigger cand : cands) {
		    	Instance inst = new Instance();
	    		inst.setInstance(document, trigger, cand);
		    	if (lsp.predict(inst) == 1) {
		    		if (anstrigger == null)
		    			anstrigger = cand;
		    		else if (cand.id > anstrigger.id)
		    			anstrigger = cand;
		    	}
		    }
		    if (anstrigger != null) {
		    	if (!ProteinFile.containsProtein(anstrigger))
		    		continue;
		    	
		    	Relation relation = new Relation();
		    	persrelations.add(relation);
		    	relation.anaTrigger = trigger;
		    	relation.anteTrigger = anstrigger;
		    }
		}
	}

	private static void extractDefiniteNPRelations(Annotation document,
			ArrayList<Trigger> dnps, ArrayList<Trigger> dnpcands, LibSVM lsd, ArrayList<Relation> dnprelations) {

		for (Trigger trigger : dnps) {
			
			// 句子窗口2
			ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    for (Trigger cand : dnpcands) {
		    	if (trigger.mention.sentNum - cand.mention.sentNum <= 2 && cand.end < trigger.start)
		    		cands.add(cand);
		    }
		    
		    Trigger anstrigger = null;
		    for (Trigger cand : cands) {
		    	Instance inst = new Instance();
	    		inst.setInstance(document, trigger, cand);
		    	if (lsd.predict(inst) == 1) {
		    		if (anstrigger == null)
		    			anstrigger = cand;
		    		else if (cand.id > anstrigger.id)
		    			anstrigger = cand;
		    	}
		    }
		    if (anstrigger != null) {
		    	if (!ProteinFile.containsProtein(anstrigger))
		    		continue;
		    	
		    	Relation relation = new Relation();
		    	dnprelations.add(relation);
		    	relation.anaTrigger = trigger;
		    	relation.anteTrigger = anstrigger;
		    }
		}

	}


}