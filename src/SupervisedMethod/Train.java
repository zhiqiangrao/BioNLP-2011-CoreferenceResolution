package SupervisedMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

import RuleBasedMethod.RuleBasedExtraction;
import utils.CorefFile;
import utils.CorefFile.Trigger;
import utils.FileFilterImpl;
import utils.FileUtil;
import utils.ProteinFile;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Train {
	
	private final static Logger logger = Logger.getLogger(Train.class.getName());
	
	public static void main(String[] args) {
		
		train();
		
	}

	private static void train() {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma");
	    props.put("tokenize.language", "English");
	    props.put("ssplit.newlineIsSentenceBreak", "always");
	    props.put("ssplit.eolonly", "true");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    File txtdir = new File("./data/BioNLP-ST_2011_coreference_training_data");
	    File parsedir = new File("./data/enju/train");
	    File ssdir = new File("./data/geniass/train");

	    ArrayList<Instance> relatinsts = new ArrayList<Instance>();
	    ArrayList<Instance> persinsts = new ArrayList<Instance>();
	    ArrayList<Instance> dnpinsts = new ArrayList<Instance>();
	    extractFromFile(pipeline, txtdir, parsedir, ssdir, relatinsts, persinsts, dnpinsts);
	
	    Instance.saveInstance(relatinsts, "train.relative");
	    Instance.saveInstance(persinsts, "train.personal");
	    Instance.saveInstance(dnpinsts, "train.dnp");
	
	    ArrayList<Instance> allinsts = new ArrayList<Instance>();
	    allinsts.addAll(relatinsts);
	    allinsts.addAll(persinsts);
	    allinsts.addAll(dnpinsts);
	    
	    LibSVM ls = new LibSVM();
	    ls.train(allinsts);
	    ls.saveModel("./model/train.model");
	
	    LibSVM lsr = new LibSVM();
	    lsr.train(relatinsts);
	    lsr.saveModel("./model/train.relave.model");
	
	    LibSVM lsp = new LibSVM();
	    lsp.train(persinsts);
	    lsp.saveModel("./model/train.personal.model");
	
	    LibSVM lsd = new LibSVM();
	    lsd.train(dnpinsts);
	    lsd.saveModel("./model/train.dnp.model");
	}

	private static void extractFromFile(StanfordCoreNLP pipeline, File txtdir, File parsedir, File ssdir, ArrayList<Instance> relatinsts, ArrayList<Instance> perinsts, ArrayList<Instance> dnpinsts) {
		if (txtdir.isDirectory()) {
			File[] txtfiles = txtdir.listFiles(new FileFilterImpl(".txt"));
			Arrays.sort(txtfiles);
			int filenum = 0;
			for (File txtfile : txtfiles) {
				File parsefile = new File(parsedir.getPath() + "/" + FileUtil.removeFileNameExtension(txtfile.getName()) + ".ptb");
				File ssfile = new File(ssdir.getPath() + "/" + FileUtil.removeFileNameExtension(txtfile.getName()) + ".ss");
				logger.info("Extracting from file: " + txtfile.getName() + " " + (++filenum));
				extractFromSingleFile(pipeline, txtfile, parsefile, ssfile, relatinsts, perinsts, dnpinsts);
			}
		}
	}

	private static void extractFromSingleFile(StanfordCoreNLP pipeline, File txtfile, File parsefile, File ssfile, ArrayList<Instance> relatinsts, ArrayList<Instance> persinsts, ArrayList<Instance> dnpinsts) {
		
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
	
	    ArrayList<Trigger> relatcands = new ArrayList<Trigger>();
	    relatcands.addAll(nps);
	    relatcands.addAll(dnps);
	    extractRelativePronounInstances(document, relatprons, relatcands, relatinsts);
	
	    ArrayList<Trigger> perscands = new ArrayList<Trigger>();
	    perscands.addAll(nps);
	    perscands.addAll(dnps);
	    extractPersonalPronounInstances(document, persprons, perscands, persinsts);
	
	    ArrayList<Trigger> dnpcands = nps;
	    extractDefiniteNPInstances(document, dnps, dnpcands, dnpinsts);
	}

	private static void extractRelativePronounInstances(Annotation document,
			ArrayList<Trigger> relatprons, ArrayList<Trigger> relatcands,
			ArrayList<Instance> relatinsts) {
		
		for (Trigger trigger : relatprons) {
			
			// 句子窗口0
			ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    for (Trigger cand : relatcands) {
		    	if (cand.mention.sentNum == trigger.mention.sentNum && cand.end < trigger.start)
		    		cands.add(cand);
		    }
		    
		    // positive instance
		    Trigger anstrigger = null;
		    for (Trigger cand : cands) {
		    	if (RuleBasedExtraction.isGoldGGP(trigger, cand)) {
		    		anstrigger = cand;
		    		
		    		Instance inst = new Instance();
		    		relatinsts.add(inst);
		    		inst.setInstance(document, trigger, cand);
		    		inst.label = 1;
		    	}
		    }
		    
		    // negative instances
		    // 正确antecedent与anaphora中间的mentions设置为负样本
		    if (anstrigger != null) {
		    	for (Trigger cand : cands) {
		    		if (cand.id > anstrigger.id) {
		    			Instance inst = new Instance();
			    		relatinsts.add(inst);
			    		inst.setInstance(document, trigger, cand);
			    		inst.label = 0;
		    		}
		    	}
		    } else {
		    	for (Trigger cand : cands) {
	    			Instance inst = new Instance();
		    		relatinsts.add(inst);
		    		inst.setInstance(document, trigger, cand);
		    		inst.label = 0;
		    	}
		    }
		}
		
	}

	private static void extractPersonalPronounInstances(Annotation document,
			ArrayList<Trigger> persprons, ArrayList<Trigger> perscands,
			ArrayList<Instance> persinsts) {

		for (Trigger trigger : persprons) {
			
			// 句子窗口1
			ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    for (Trigger cand : perscands) {
		    	if (trigger.mention.sentNum - cand.mention.sentNum <= 1 && cand.end < trigger.start)
		    		cands.add(cand);
		    }
		    
		    // positive instance
		    Trigger anstrigger = null;
		    for (Trigger cand : cands) {
		    	if (RuleBasedExtraction.isGoldGGP(trigger, cand)) {
		    		anstrigger = cand;
		    		
		    		Instance inst = new Instance();
		    		persinsts.add(inst);
		    		inst.setInstance(document, trigger, cand);
		    		inst.label = 1;
		    	}
		    }
		    
		    // negative instances
		    if (anstrigger != null) {
		    	for (Trigger cand : cands) {
		    		if (cand.id > anstrigger.id) {
		    			Instance inst = new Instance();
			    		persinsts.add(inst);
			    		inst.setInstance(document, trigger, cand);
			    		inst.label = 0;
		    		}
		    	}
		    } else {
		    	for (Trigger cand : cands) {
	    			Instance inst = new Instance();
		    		persinsts.add(inst);
		    		inst.setInstance(document, trigger, cand);
		    		inst.label = 0;
		    	}
		    }
		}
	}

	private static void extractDefiniteNPInstances(Annotation document,
			ArrayList<Trigger> dnps, ArrayList<Trigger> dnpcands,
			ArrayList<Instance> dnpinsts) {

		for (Trigger trigger : dnps) {
			
			// 句子窗口2
			ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    for (Trigger cand : dnpcands) {
		    	if (trigger.mention.sentNum - cand.mention.sentNum <= 2 && cand.end < trigger.start)
		    		cands.add(cand);
		    }
		    
		    // positive instance
		    Trigger anstrigger = null;
		    for (Trigger cand : cands) {
		    	if (RuleBasedExtraction.isGoldGGP(trigger, cand)) {
		    		anstrigger = cand;
		    		
		    		Instance inst = new Instance();
		    		dnpinsts.add(inst);
		    		inst.setInstance(document, trigger, cand);
		    		inst.label = 1;
		    	}
		    }
		    
		    // negative instances
		    if (anstrigger != null) {
		    	for (Trigger cand : cands) {
		    		if (cand.id > anstrigger.id) {
		    			Instance inst = new Instance();
			    		dnpinsts.add(inst);
			    		inst.setInstance(document, trigger, cand);
			    		inst.label = 0;
		    		}
		    	}
		    } else {
		    	for (Trigger cand : cands) {
	    			Instance inst = new Instance();
		    		dnpinsts.add(inst);
		    		inst.setInstance(document, trigger, cand);
		    		inst.label = 0;
		    	}
		    }
		}

	}


}