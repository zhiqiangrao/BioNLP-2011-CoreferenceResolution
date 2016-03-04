package RuleBasedMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import utils.CorefFile;
import utils.FileFilterImpl;
import utils.FileUtil;
import utils.ProteinFile;
import utils.CorefFile.Relation;
import utils.CorefFile.Trigger;
import utils.ProteinFile.Protein;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.IntPair;

public class RuleBasedExtraction {
	
	private final static Logger logger = Logger.getLogger(RuleBasedExtraction.class.getName());
	
	public static void main(String[] args) {
		
		extract();
		
	}

	private static void extract() {
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
	    
	    extractFromFile(pipeline, txtdir, parsedir, ssdir, resultdir);
	}

	private static void extractFromFile(StanfordCoreNLP pipeline, File txtdir, File parsedir, File ssdir, File resultdir) {
		if (txtdir.isDirectory()) {
			File[] txtfiles = txtdir.listFiles(new FileFilterImpl(".txt"));
			Arrays.sort(txtfiles);
			int filenum = 0;
			for (File txtfile : txtfiles) {
				File parsefile = new File(parsedir.getPath() + "/" + FileUtil.removeFileNameExtension(txtfile.getName()) + ".ptb");
				File ssfile = new File(ssdir.getPath() + "/" + FileUtil.removeFileNameExtension(txtfile.getName()) + ".ss");
				logger.info("Extracting from file: " + txtfile.getName() + " " + (++filenum));
				extractFromSingleFile(pipeline, txtfile, parsefile, ssfile, resultdir);
			}
		}
	}

	private static void extractFromSingleFile(StanfordCoreNLP pipeline, File txtfile, File parsefile, File ssfile, File resultdir) {

		String text = FileUtil.readFile(ssfile);
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    setEnjuParseTree(document, parsefile);
	    File a1file = new File(FileUtil.removeFileNameExtension(txtfile.getPath()) + ".a1");
	    ProteinFile.getProteins(a1file);
	    File a2file = new File(FileUtil.removeFileNameExtension(txtfile.getPath()) + ".a2");
	    CorefFile.readCorefFile(a2file);

	    ArrayList<Trigger> relatprons = new ArrayList<Trigger>();
	    ArrayList<Trigger> persprons = new ArrayList<Trigger>();
	    ArrayList<Trigger> dnps = new ArrayList<Trigger>();
	    ArrayList<Trigger> nps = new ArrayList<Trigger>();
	    extractPredictedMentions(document, relatprons, persprons, dnps, nps);

	    ArrayList<Trigger> alltriggers = new ArrayList<Trigger>();
	    alltriggers.addAll(relatprons);
	    alltriggers.addAll(persprons);
	    alltriggers.addAll(dnps);
	    alltriggers.addAll(nps);
	    ArrayList<Trigger> sortedtriggers = setTriggerIdAndMentionId(alltriggers);

	    ArrayList<Relation> relatrelations = new ArrayList<Relation>();
	    ArrayList<Trigger> relatcands = new ArrayList<Trigger>();
	    relatcands.addAll(nps);
	    relatcands.addAll(dnps);
	    extractRelativePronounRelations(document, relatprons, relatcands, relatrelations);
	
	    ArrayList<Relation> persrelations = new ArrayList<Relation>();
	    ArrayList<Trigger> perscands = new ArrayList<Trigger>();
	    perscands.addAll(nps);
	    perscands.addAll(dnps);
	    extractPersonalPronounRelations(document, persprons, perscands, persrelations);

	    ArrayList<Relation> dnprelations = new ArrayList<Relation>();
	    ArrayList<Trigger> dnpcands = nps;
	    extractDefiniteNPRelations(document, dnps, dnpcands, dnprelations);
	    
	    ArrayList<Relation> allrelations = new ArrayList<Relation>();
	    allrelations.addAll(relatrelations);
	    allrelations.addAll(persrelations);
	    allrelations.addAll(dnprelations);
	    writeResult(txtfile, resultdir, sortedtriggers, allrelations);
	}

	public static String getTextFile(File file) {
	    BufferedReader br;
	    StringBuilder sb = new StringBuilder();
	    String s;
		try {
			br = new BufferedReader(new FileReader(file));
		    while ((s = br.readLine()) != null) {
		    	sb.append(s);
		    	sb.append("\n");
		    }
		    br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    return sb.toString();
	}
	
	public static void setEnjuParseTree(Annotation document, File parsefile) {
		BufferedReader br;
		String s;
		int index = 0;
		try {
			br = new BufferedReader(new FileReader(parsefile));
		    while ((s = br.readLine()) != null) {
		    	document.get(CoreAnnotations.SentencesAnnotation.class)
		    		.get(index++)
		    		.set(TreeCoreAnnotations.TreeAnnotation.class, Tree.valueOf(s));
		    }
		    br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void extractPredictedMentions(Annotation document, ArrayList<Trigger> relatprons,  ArrayList<Trigger> persprons, ArrayList<Trigger> dnps, ArrayList<Trigger> nps) {
	    MyRuleBasedCorefMentionFinder rbcmf = new MyRuleBasedCorefMentionFinder();
		for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
	    	Set<IntPair> anaspanset = Generics.newHashSet();
	    	Set<IntPair> antespanset = Generics.newHashSet();
	    	extractRelativePronouns(document, sentence, relatprons, anaspanset, rbcmf);
	    	extractPersonalPronouns(document, sentence, persprons, anaspanset, rbcmf);
	    	extractDefiniteNPs(document, sentence, dnps, anaspanset, rbcmf);
	    	extractNPs(document, sentence, nps, anaspanset, antespanset, rbcmf);
	    }
	}

	private static void extractRelativePronouns(Annotation document, CoreMap sentence, ArrayList<Trigger> relatprons, Set<IntPair> anaspanset, MyRuleBasedCorefMentionFinder rbcmf) {
	    List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
	    Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
	    tree.indexLeaves();
	    SemanticGraph dependency = sentence.get(SemanticGraphCoreAnnotations.AlternativeDependenciesAnnotation.class);
	    
	    TregexPattern tgrepPattern = TregexPattern.compile("/^(?:WDT|WP)/");
	    TregexMatcher matcher = tgrepPattern.matcher(tree);
	    while (matcher.find()) {
	    	Tree mtree = matcher.getMatch();
	        List<Tree> mLeaves = mtree.getLeaves();
	        int beginIdx = ((CoreLabel)mLeaves.get(0).label()).get(CoreAnnotations.IndexAnnotation.class)-1;
	        int endIdx = ((CoreLabel)mLeaves.get(mLeaves.size()-1).label()).get(CoreAnnotations.IndexAnnotation.class);
	        
	        // modify location
	        int start = -1;
	    	int end = -1;
	    	for (CoreLabel token : tokens) {
	    		if (token.get(CoreAnnotations.TextAnnotation.class).equals(mLeaves.get(0).toString())) {
	    			if (start == -1) {
	    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
	    				end = token.get(CoreAnnotations.IndexAnnotation.class);
	    			}
	    			else if(Math.abs(token.get(CoreAnnotations.IndexAnnotation.class)-1-beginIdx) < Math.abs(start-beginIdx)) {
	    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
	    				end = token.get(CoreAnnotations.IndexAnnotation.class);
	    			}
	    		}
	    	}

	    	if (start==-1)
	    		continue;
	    	
	        IntPair mSpan = new IntPair(start, end);
	        if(!anaspanset.contains(mSpan)) {
	        	int dummyMentionId = -1;
	        	Mention m = new Mention(dummyMentionId, start, end, dependency, new ArrayList<>(tokens.subList(start, end)), mtree);
	        	m.sentNum = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class)+1;
				rbcmf.findHead2(sentence, m, beginIdx, endIdx);
				if (m.headWord == null)
					continue;
				anaspanset.add(mSpan);
				
				Trigger trigger = new Trigger();
				relatprons.add(trigger);
				trigger.mention = m;
				trigger.start = m.originalSpan.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
				trigger.end = m.originalSpan.get(m.originalSpan.size()-1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
				trigger.name = document.get(CoreAnnotations.TextAnnotation.class).substring(trigger.start, trigger.end);
				trigger.type = "RELATIVE";
				trigger.number = "UNKNOWN";
	        }
	    }
	}

	private static void extractPersonalPronouns(Annotation document, CoreMap sentence, ArrayList<Trigger> persprons, Set<IntPair> anaspanset, MyRuleBasedCorefMentionFinder rbcmf) {
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
	    tree.indexLeaves();
	    SemanticGraph dependency = sentence.get(SemanticGraphCoreAnnotations.AlternativeDependenciesAnnotation.class);
	  
	    TregexPattern tgrepPattern = TregexPattern.compile("/^(?:PRP)/");
	    TregexMatcher matcher = tgrepPattern.matcher(tree);
	    while (matcher.find()) {
	    	Tree mtree = matcher.getMatch();
	        List<Tree> mLeaves = mtree.getLeaves();
	        
	        if (!mLeaves.get(0).toString().equalsIgnoreCase("it")
	        		&& !mLeaves.get(0).toString().equalsIgnoreCase("itself")
	        		&& !mLeaves.get(0).toString().equalsIgnoreCase("they")
	        		&& !mLeaves.get(0).toString().equalsIgnoreCase("them")
	        		&& !mLeaves.get(0).toString().equalsIgnoreCase("themselves")
	        		&& !mLeaves.get(0).toString().equalsIgnoreCase("its")
	        		&& !mLeaves.get(0).toString().equalsIgnoreCase("their"))
	        	continue;
	        
	        // 过滤后面接从句的
	        Tree parent = mtree.parent(tree).parent(tree);
	        Tree[] chds = parent.children();
	        Tree nextree = null;
	        for (int i=0; i < chds.length; i++) {
	        	if (chds[i] == mtree && i != chds.length-1) {
	        		nextree = chds[i+1];
	        		break;
	        	}
	        }
	        if (nextree != null && nextree.toString().contains("SBAR"))
	        	continue;
	        
	        int beginIdx = ((CoreLabel)mLeaves.get(0).label()).get(CoreAnnotations.IndexAnnotation.class)-1;
	        int endIdx = ((CoreLabel)mLeaves.get(mLeaves.size()-1).label()).get(CoreAnnotations.IndexAnnotation.class);
	        
	        // modify location
	        int start = -1;
	    	int end = -1;
	    	for (CoreLabel token : tokens) {
	    		if (token.get(CoreAnnotations.TextAnnotation.class).equals(mLeaves.get(0).toString())) {
	    			if (start == -1) {
	    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
	    				end = token.get(CoreAnnotations.IndexAnnotation.class);
	    			}
	    			else if(Math.abs(token.get(CoreAnnotations.IndexAnnotation.class)-1-beginIdx) < Math.abs(start-beginIdx)) {
	    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
	    				end = token.get(CoreAnnotations.IndexAnnotation.class);
	    			}
	    		}
	    	}
	    	if (start==-1)
	    		continue;
	    	
	        IntPair mSpan = new IntPair(start, end);
	        if(!anaspanset.contains(mSpan)) {
	        	int dummyMentionId = -1;
	        	Mention m = new Mention(dummyMentionId, start, end, dependency, new ArrayList<>(tokens.subList(start, end)), mtree);
	        	m.sentNum = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class)+1;
				rbcmf.findHead2(sentence, m, beginIdx, endIdx);
				if (m.headWord == null)
					continue;
				if(MyRuleBasedCorefMentionFinder.isPleonastic(m, tree))
					continue;
				anaspanset.add(mSpan);
				
				Trigger trigger = new Trigger();
				persprons.add(trigger);
				trigger.mention = m;
				trigger.start = m.originalSpan.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
				trigger.end = m.originalSpan.get(m.originalSpan.size()-1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
				trigger.name = document.get(CoreAnnotations.TextAnnotation.class).substring(trigger.start, trigger.end);
				trigger.type = "PERSONAL";
				if (trigger.name.equalsIgnoreCase("it")
						|| trigger.name.equalsIgnoreCase("itself")
						|| trigger.name.equalsIgnoreCase("its"))
					trigger.number = "SINGULAR";
				else
					trigger.number = "PLURAL";
	        }
	    }
	}

	private static void extractDefiniteNPs(Annotation document, CoreMap sentence, ArrayList<Trigger> dnps, Set<IntPair> anaspanset, MyRuleBasedCorefMentionFinder rbcmf) {
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
	    tree.indexLeaves();
	    SemanticGraph dependency = sentence.get(SemanticGraphCoreAnnotations.AlternativeDependenciesAnnotation.class);
	  
	    TregexPattern tgrepPattern = TregexPattern.compile("NP < (/^(?:DT)/)");
	    TregexMatcher matcher = tgrepPattern.matcher(tree);
	    while (matcher.find()) {
	    	Tree mtree = matcher.getMatch();
	        List<Tree> mLeaves = mtree.getLeaves();
	        
	        if (!mLeaves.get(0).toString().equalsIgnoreCase("this")
	        		&& !mLeaves.get(0).toString().equalsIgnoreCase("these")
	        		&& !mLeaves.get(0).toString().equalsIgnoreCase("those")
	        		&& !mLeaves.get(0).toString().equals("the"))
	        	continue;
	        
	        int beginIdx = ((CoreLabel)mLeaves.get(0).label()).get(CoreAnnotations.IndexAnnotation.class)-1;
	        int endIdx = ((CoreLabel)mLeaves.get(mLeaves.size()-1).label()).get(CoreAnnotations.IndexAnnotation.class);
	        
	        int start = -1;
	    	int end = -1;
	    	// 第一个word的长度等于token
	    	for (CoreLabel token : tokens) {
	    		if (token.get(CoreAnnotations.TextAnnotation.class).equals(mLeaves.get(0).toString())) {
	    			if (start == -1)
	    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
	    			else if(Math.abs(token.get(CoreAnnotations.IndexAnnotation.class)-1-beginIdx) < Math.abs(start-beginIdx))
	    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
	    		}
	    	}

	    	if (start==-1)
	    		continue;
	    	
	    	// 查找最后一个word
	    	int mlen = 0;
	    	for (Tree ml : mLeaves) {
	    		mlen += ml.toString().length();
	    	}
	    	int len = 0;
	    	for (int i = start; i < tokens.size(); i++) {
	    		len += tokens.get(i).get(CoreAnnotations.TextAnnotation.class).length();
	    		if (len > mlen) {
	    			end = i;
	    			break;
	    		}
	    		else if (len == mlen) {
	    			end = i+1;
	    			break;
	    		}
	    	}
	    	
	    	IntPair mSpan = new IntPair(start, end);
	        if(!anaspanset.contains(mSpan)) {
	        	int dummyMentionId = -1;
	        	Mention m = new Mention(dummyMentionId, start, end, dependency, new ArrayList<>(tokens.subList(start, end)), mtree);
	        	
	        	// 含实体
	        	if (ProteinFile.containsBioEntity(m) || ProteinFile.containsProtein(m))
	        		continue;
	        	// 含大写字母
	        	String mstr = m.toString();
	        	if (m.startIndex == 0)
	    			mstr = mstr.substring(1).trim();
	        	if (!mstr.toLowerCase().equals(mstr))
	        		continue;
	        	// 含符号
	        	if (m.toString().contains(",") || m.toString().contains("/") || m.toString().contains("-"))
	        		continue;
	        	// 中间含其他修饰词
	        	if (m.originalSpan.size()>2)
	    	        continue;
	          
	        	m.sentNum = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class)+1;
				rbcmf.findHead2(sentence, m, beginIdx, endIdx);
				if (m.headWord == null)
					continue;
				
				if (!m.headString.startsWith("protein")
						&& !m.headString.equalsIgnoreCase("gene")
						&& !m.headString.equalsIgnoreCase("genes")
						&& !m.headString.startsWith("factor")
						//&& !m.headString.startsWith("molecule")
						&& !m.headString.startsWith("element")
						//&& !m.headString.startsWith("inhibitor")
						&& !m.headString.startsWith("receptor")
						&& !m.headString.startsWith("complex")
						&& !m.headString.startsWith("construct")
						)
			        continue;
				anaspanset.add(mSpan);
				
				 // 过滤后面接从句的
		        Tree parent = mtree.parent(tree);
		        Tree[] chds = parent.children();
		        Tree nextree = null;
		        for (int i=0; i < chds.length; i++) {
		        	if (chds[i] == mtree && i != chds.length-1) {
		        		nextree = chds[i+1];
		        		break;
		        	}
		        }
		        if (nextree != null && nextree.toString().contains("SBAR"))
		        	continue;
				
				Trigger trigger = new Trigger();
				dnps.add(trigger);
				trigger.mention = m;
				trigger.start = m.originalSpan.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
				trigger.end = m.originalSpan.get(m.originalSpan.size()-1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
				trigger.name = document.get(CoreAnnotations.TextAnnotation.class).substring(trigger.start, trigger.end);
				trigger.type = "DNP";
				setNumber(trigger);
	        }
	    }
	}

	private static void extractNPs(Annotation document, CoreMap sentence, ArrayList<Trigger> nps, Set<IntPair> anaspanset, Set<IntPair> antespanset, MyRuleBasedCorefMentionFinder rbcmf) {
		ArrayList<Trigger> nounps = new ArrayList<Trigger>();
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
	    tree.indexLeaves();
	    SemanticGraph dependency = sentence.get(SemanticGraphCoreAnnotations.AlternativeDependenciesAnnotation.class);
	  
	    TregexPattern tgrepPattern = TregexPattern.compile("/^(?:NP)/");
	    TregexMatcher matcher = tgrepPattern.matcher(tree);
	    while (matcher.find()) {
	    	Tree mtree = matcher.getMatch();
	    	List<Tree> mLeaves = mtree.getLeaves();
	    	// 含从句
	    	if (mtree.toString().contains("(S"))
	    		continue;
	        
	    	int beginIdx = ((CoreLabel)mLeaves.get(0).label()).get(CoreAnnotations.IndexAnnotation.class)-1;
	        int endIdx = ((CoreLabel)mLeaves.get(mLeaves.size()-1).label()).get(CoreAnnotations.IndexAnnotation.class);
	        
	        // modify location
	        int start = -1;
	    	int end = -1;
	    	
	    	// 第一个word的长度等于token
	    	for (CoreLabel token : tokens) {
	    		if (token.get(CoreAnnotations.TextAnnotation.class).equals(mLeaves.get(0).toString())) {
	    			if (start == -1)
	    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
	    			else if(Math.abs(token.get(CoreAnnotations.IndexAnnotation.class)-1-beginIdx) < Math.abs(start-beginIdx))
	    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
	    		}
	    	}
	    	
	    	// 第一个word的长度大于token
	    	if (start == -1) {
	    		for (CoreLabel token : tokens) {
		    		if (mLeaves.get(0).toString().contains(token.get(CoreAnnotations.TextAnnotation.class))) {
		    			if (start == -1)
		    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
		    			else if(Math.abs(token.get(CoreAnnotations.IndexAnnotation.class)-1-beginIdx) < Math.abs(start-beginIdx))
		    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
		    		}
		    	}
	    	}
	    	
	    	// 第一个word的长度小于token
	    	if (start == -1) {
	    		for (CoreLabel token : tokens) {
		    		if (token.get(CoreAnnotations.TextAnnotation.class).contains(mLeaves.get(0).toString())) {
		    			if (start == -1)
		    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
		    			else if(Math.abs(token.get(CoreAnnotations.IndexAnnotation.class)-1-beginIdx) < Math.abs(start-beginIdx))
		    				start = token.get(CoreAnnotations.IndexAnnotation.class)-1;
		    		}
		    	}
	    	}
	    	
	    	if (start==-1)
	    		continue;
	    	
	    	// 查找最后一个word
	    	int mlen = 0;
	    	for (Tree ml : mLeaves) {
	    		mlen += ml.toString().length();
	    	}
	    	int len = 0;
	    	for (int i = start; i < tokens.size(); i++) {
	    		len += tokens.get(i).get(CoreAnnotations.TextAnnotation.class).length();
	    		if (len > mlen) {
	    			end = i;
	    			break;
	    		}
	    	}
	    	
	    	if (start >= end)
	    		continue;
	    	
	        IntPair mSpan = new IntPair(start, end);
	        if(!containsSpanOfSet(mSpan, anaspanset) && !insideSpanOfSet(mSpan,antespanset)) {
	        	int dummyMentionId = -1;
	        	Mention m = new Mention(dummyMentionId, start, end, dependency, new ArrayList<>(tokens.subList(start, end)), mtree);
	        	m.sentNum = sentence.get(CoreAnnotations.SentenceIndexAnnotation.class)+1;
				rbcmf.findHead2(sentence, m, beginIdx, endIdx);
				if (m.headWord == null)
					continue;
				antespanset.add(mSpan);
				
				Trigger trigger = new Trigger();
				nounps.add(trigger);
				trigger.mention = m;
				trigger.start = m.originalSpan.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
				trigger.end = m.originalSpan.get(m.originalSpan.size()-1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
				trigger.name = document.get(CoreAnnotations.TextAnnotation.class).substring(trigger.start, trigger.end);
				trigger.type = "NP";
				setNumber(trigger);
	        }
    	}
	    
	    // 选择更长的的mention
	    Set<Trigger> remove = Generics.newHashSet();
	    for (Trigger t1 : nounps) {
	    	for (Trigger t2 : nounps) {
	    		if (t1 == t2 || remove.contains(t1) || remove.contains(t2))
	    			continue;
	    		if (t2.start > t1.start && t2.end < t1.end)
	    			remove.add(t2);
	    		else if (t1.start > t2.start && t1.end < t2.end)
	    			remove.add(t1);
	    	}
	    }
	    nounps.removeAll(remove);
	    nps.addAll(nounps);
	    
	}

	private static boolean containsSpanOfSet(IntPair mSpan, Set<IntPair> spanset) {
		int start = mSpan.getSource();
		int end = mSpan.getTarget();
		for (IntPair span : spanset) {
			if (start <= span.getSource() && end >= span.getTarget())
				return true;
		}
		return false;
	}
	
	private static boolean insideSpanOfSet(IntPair mSpan, Set<IntPair> spanset) {
		int start = mSpan.getSource();
		int end = mSpan.getTarget();
		for (IntPair span : spanset) {
			if (start >= span.getSource() && end <= span.getTarget())
				return true;
		}
		return false;
	}
	
	private static void setNumber(Trigger trigger) {
		String tag = trigger.mention.headWord.get(CoreAnnotations.PartOfSpeechAnnotation.class);
		if (tag.startsWith("N") && tag.endsWith("S")) {
			trigger.number = "PLURAL";
		} else if (trigger.name.contains(" and ") || trigger.name.contains("family")) {
			trigger.number = "PLURAL";
		} else if (tag.startsWith("N")) {
			trigger.number = "SINGULAR";
		} else {
			trigger.number = "SINGULAR";
		}
	}
	
	public static ArrayList<Trigger> setTriggerIdAndMentionId(ArrayList<Trigger> triggers) {
		ArrayList<Trigger> sortedtriggers = new ArrayList<Trigger>();
		int id = 1;
		while (triggers.size() > 0) {
		    Trigger mintrigger = triggers.get(0);
		    for (Trigger tri : triggers) {
		    	if (tri == mintrigger)
		    		continue;
		    	if (tri.start < mintrigger.start)
		    		mintrigger = tri;
		    }
		    mintrigger.mention.mentionID = id++;
		    mintrigger.id = mintrigger.mention.mentionID + ProteinFile.lastId;
		    sortedtriggers.add(mintrigger);
		    triggers.remove(mintrigger);
		}
		return sortedtriggers;
	}

	private static void extractRelativePronounRelations(Annotation document, ArrayList<Trigger> relatprons, ArrayList<Trigger> relatcands, ArrayList<Relation> relatrelations) {
	  
		for (Trigger trigger : relatprons) {
			
		    ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    for (Trigger cand : relatcands) {
		    	// 可以是非实体
		    	if (cand.mention.sentNum == trigger.mention.sentNum && cand.end < trigger.start)
		    		cands.add(cand);
		    }

		    Trigger anstrigger = null;
		    Tree root = document.get(CoreAnnotations.SentencesAnnotation.class).get(trigger.mention.sentNum-1).get(TreeCoreAnnotations.TreeAnnotation.class);
		    Tree tritree = trigger.mention.mentionSubTree;
		    for (Trigger cand : cands) {
		    	Tree candtree = cand.mention.mentionSubTree;
		    	List<Tree> pathtrees = root.pathNodeToNode(tritree, candtree);
		    	Tree comtree = null;
		    	for (Tree tree : pathtrees) {
		    		if (tree.dominates(tritree) && tree.dominates(candtree))
		    			comtree = tree;
		    	}
		    	
		    	if (!comtree.label().value().equals("NP")
		    			&& !comtree.label().value().equals("VP"))
		    		continue;
		    	
		    	String pathstr = null;
		    	for (Tree tree : pathtrees) {
		    		if (pathstr == null)
		    			pathstr = tree.label().value();
		    		else if (tree == comtree)
		    			pathstr = pathstr + "-[" + tree.label().value() + "]";
		    		else
		    			pathstr = pathstr + "-" + tree.label().value();
		    	}
		    	
		    	if (pathstr.contains("PRN"))
		    		continue;
		      
		    	if (anstrigger == null) {
		    		anstrigger = cand;
		    	}
		    	else if (cand.mention.mentionID > anstrigger.mention.mentionID) {
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

	private static void extractPersonalPronounRelations(Annotation document, ArrayList<Trigger> persprons, ArrayList<Trigger> perscands, ArrayList<Relation> persrelations) {
		
		for (Trigger trigger : persprons) {

		    ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    ArrayList<Trigger> cands1 = new ArrayList<Trigger>();	// 前一个句子中的
		    for (Trigger cand : perscands) {
		    	// 只能是实体
		    	if (trigger.mention.sentNum - cand.mention.sentNum == 0
		    			&& cand.end < trigger.start )
		    		cands.add(cand);
		    	else if (trigger.mention.sentNum - cand.mention.sentNum == 1
		    			&& cand.end < trigger.start )
		    		cands1.add(cand);
		    }
		    
		    Trigger anstrigger = null;
		    if (cands.size() !=0 ) {
			    Tree root = document.get(CoreAnnotations.SentencesAnnotation.class).get(trigger.mention.sentNum-1).get(TreeCoreAnnotations.TreeAnnotation.class);
			    Tree tritree = trigger.mention.mentionSubTree;    
			    boolean find = false;
			    
			    // NP-CC
			    Tree sontree = tritree;
			    Tree fatree = sontree;
			    while (!find) {
			    	fatree = sontree.parent(root);
			    	if (fatree == null || fatree.label().value().equals("ROOT"))
			    		break;
			    	if (sontree.label().value().equals("NP")) {
			    		boolean existnpcc = false;
			    		Tree[] chdtrees = fatree.children();
			    		for (Tree chd : chdtrees) {
			    			if (chd == sontree)
			    				break;
			    			if (chd.label().value().equals("CC")) {
			    				existnpcc = true;
			    				break;
			    			}
			    		}
			    		if (existnpcc) {
			    			// 在第一个NP中查找
				            for (Tree chd : chdtrees) {
				            	if (!chd.label().value().equals("NP"))
				            		continue;
			              		if (chd == sontree)
			              			break;
			              		for (Trigger cand : cands) {
			              			if (!ProteinFile.containsProtein(cand) && !ProteinFile.containsBioEntity(cand))
			              				continue;
					                if (chd.dominates(cand.mention.mentionSubTree)) {
					                	if (anstrigger == null)
					                		anstrigger = cand;
					                	else if (cand.mention.mentionID < anstrigger.mention.mentionID)
					                		anstrigger = cand;
					                	find = true;
					                }
			              		}
				            }
				            // 第一个NP中没有，在右边结点中查找
		              		if (!find) {
				                for (Trigger cand : cands) {
				                	if (!ProteinFile.containsProtein(cand) && !ProteinFile.containsBioEntity(cand))
			              				continue;
				                	if (fatree.dominates(cand.mention.mentionSubTree)) {
					                    if (anstrigger == null)
					                    	anstrigger = cand;
					                    else if (cand.mention.mentionID < anstrigger.mention.mentionID)
					                    	anstrigger = cand;
					                    find = true;
				                	}
				                }
		              		}
			    		}
			    	}
			    	sontree = fatree;
		    	}
	
			    // VP-CC
			    sontree = tritree;
			    fatree = sontree;
			    while (!find) {
			    	fatree = sontree.parent(root);
			    	if (fatree == null || fatree.label().value().equals("ROOT"))
			    		break;
			    	if (sontree.label().value().equals("VP")) {
			    		boolean existvpcc = false;
			    		Tree[] chdtrees = fatree.children();
			    		for (Tree chd : chdtrees) {
			    			if (chd == sontree)
			    				break;
			    			if (chd.label().value().equals("CC")) {
			    				existvpcc = true;
			    				break;
			    			}
			    		}
			    		if (existvpcc) {
			    			// 在上层最近S中寻找
			    			while(!find) {
			    				sontree = fatree;
			    				fatree = sontree.parent(root);
			    				if (fatree == null || fatree.label().value().equals("ROOT"))
			    					break;
			    				if (fatree.label().value().equals("S")) {
			    					for (Trigger cand : cands) {
			    						if (!ProteinFile.containsProtein(cand) && !ProteinFile.containsBioEntity(cand))
				              				continue;
			    						if (fatree.dominates(cand.mention.mentionSubTree)) {
			    							if (anstrigger == null)
			    								anstrigger = cand;
			    							else if (cand.mention.mentionID < anstrigger.mention.mentionID)
			    								anstrigger = cand;
			    							find = true;
			    						}
			    					}
			    				}
		    				}
			    		}
			    	}
		    		sontree = fatree;
		    	}
	
			    // S-CC, SBAR-CC
			    sontree = tritree;
			    fatree = sontree;
			    while (!find) {
			    	fatree = sontree.parent(root);
			    	if (fatree == null || fatree.label().value().equals("ROOT"))
			    		break;
			    	if (sontree.label().value().equals("S") || sontree.label().value().equals("SBAR")) {
			    		boolean existscc = false;
			    		Tree[] chdtrees = fatree.children();
				        for (Tree chd : chdtrees) {
			        		if (chd == sontree)
			    				break;
			    			if (chd.label().value().equals("CC")) {
			    				existscc = true;
			    				break;
			    			}
				        }
		        		//在第一个S或SBAR中查找
				        if (existscc) {
				            for (Tree chd : chdtrees) {
				            	if (!chd.label().value().equals("S") && !chd.label().value().equals("SBAR"))
				            		continue;
			              		if (chd == sontree)
			              			break;
				            	for (Trigger cand : cands) {
				            		if (!ProteinFile.containsProtein(cand) && !ProteinFile.containsBioEntity(cand))
			              				continue;
				            		if (chd.dominates(cand.mention.mentionSubTree)) {
				            			if (anstrigger == null)
				            				anstrigger = cand;
				            			else if (cand.mention.mentionID < anstrigger.mention.mentionID)
				            				anstrigger = cand;
				            			find = true;
				            		}
				            	}
				            }
				        }
			    	}
			    	sontree = fatree;
		    	}
	
			    // 在上层最近S中查找
			    sontree = tritree;
			    fatree = sontree;
			    while (!find) {
			    	fatree = sontree.parent(root);
			    	if (fatree == null || fatree.label().value().equals("ROOT"))
			    		break;
			    	if (fatree.label().value().equals("S")) {
			    		for (Trigger cand : cands) {
			    			if (!ProteinFile.containsProtein(cand) && !ProteinFile.containsBioEntity(cand))
	              				continue;
			    			if (fatree.dominates(cand.mention.mentionSubTree)) {
			    				if (anstrigger == null)
			    					anstrigger = cand;
			    				else if (cand.mention.mentionID < anstrigger.mention.mentionID)	//选择最远的
			    					anstrigger = cand;
			    				find = true;
			    			}
			    		}
			    	}
		    		sontree = fatree;
			    }
			    
		    }
		    else if (cands1.size() != 0) {
		    	// 查找上句中最近的protein mention
		    	for (Trigger cand : cands1) {
		    		if (!ProteinFile.containsProtein(cand))
		    			continue;
		    		if (anstrigger == null)
    					anstrigger = cand;
    				else if (cand.mention.mentionID > anstrigger.mention.mentionID)
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


	private static void extractDefiniteNPRelations(Annotation document, ArrayList<Trigger> dnpprons, ArrayList<Trigger> dnpcands, ArrayList<Relation> dnprelations) {
		
		for (Trigger trigger : dnpprons) {
	
			ArrayList<Trigger> cands = new ArrayList<Trigger>();
		    for (Trigger cand : dnpcands) {
		    	// 只能是实体
	    		if (trigger.mention.sentNum - cand.mention.sentNum <= 2 
	    				&& cand.end < trigger.start
	    				&& ProteinFile.containsEntity(cand.mention))
	    			cands.add(cand);
		    }
	
		    Trigger anstrigger = null;
		    ArrayList<Trigger> anscands = new ArrayList<Trigger>();
		    if (trigger.number.equals("PLURAL")) {
		    	// proteins, genes
		    	if (trigger.mention.headString.equals("proteins")
		    			|| trigger.mention.headString.equals("genes")) {
		    		ArrayList<Trigger> headcands = new ArrayList<Trigger>();
		    		//ArrayList<Trigger> famcands = new ArrayList<Trigger>();
		    		ArrayList<Trigger> protnumcands = new ArrayList<Trigger>();
		    		for (Trigger cand : cands) {
		    			if (!ProteinFile.containsProtein(cand))	// 必须含有蛋白质
		    				continue;
		    			if (trigger.mention.headString.equals(cand.mention.headString))
		    				headcands.add(cand);
		    			//else if (trigger.mention.headString.equals("family"))
		    			//	famcands.add(cand);
		    			else if (ProteinFile.containsProtNum(cand) > 1)
		    				protnumcands.add(cand);
		    		}
			        if (headcands.size() > 0)
			        	anscands = headcands;
			        //else if (famcands.size() > 0)
			        //	anscands = famcands;
			        else if (protnumcands.size() > 0)
			        	anscands = protnumcands;
		    	} else {
		    		// factors, elements, receptors, complexes, constructs
			        ArrayList<Trigger> headcands = new ArrayList<Trigger>();
			        ArrayList<Trigger> partcands = new ArrayList<Trigger>();
			        ArrayList<Trigger> bionumcands = new ArrayList<Trigger>();
			        ArrayList<Trigger> protnumcands = new ArrayList<Trigger>();
			        for (Trigger cand : cands) {
			        	if (trigger.mention.headString.equals(cand.mention.headString))
			        		headcands.add(cand);
			        	else if (cand.name.contains(trigger.mention.headString))
			        		partcands.add(cand);
			        	else if (ProteinFile.containsBioNum(cand) > 1)
			        		bionumcands.add(cand);
			        	else if (ProteinFile.containsProtNum(cand) > 1)
			        		protnumcands.add(cand);
		        	}
		        	if (headcands.size() > 0)
		        		anscands = headcands;
		        	else if (partcands.size() > 0)
			        	anscands = partcands;
			        else if (bionumcands.size() > 0)
			        	anscands = bionumcands;
			        else if (protnumcands.size() > 0)
			        	anscands = protnumcands;
		    	}
	
	    	} else {
	    		// protein, gene
	    		if (trigger.mention.headString.equals("protein")
	    				|| trigger.mention.headString.equals("gene")) {
		        ArrayList<Trigger> headcands = new ArrayList<Trigger>();
		        ArrayList<Trigger> protnumcands = new ArrayList<Trigger>();
		        for (Trigger cand : cands) {
		        	if (!ProteinFile.containsProtein(cand))	// 必须含有蛋白质
		        		continue;
		        	if (trigger.mention.headString.equals(cand.mention.headString))
		        		headcands.add(cand);
		        	else if (ProteinFile.containsProtNum(cand) == 1)
		        		protnumcands.add(cand);
		        }
		        if (headcands.size() > 0)
		        	anscands = headcands;
		        else if (protnumcands.size() > 0)
		        	anscands = protnumcands;
	    		} else {
	    			// factor, element, receptor, complex, construct
	    			ArrayList<Trigger> headcands = new ArrayList<Trigger>();
	    			ArrayList<Trigger> partcands = new ArrayList<Trigger>();
	    			ArrayList<Trigger> bionumcands = new ArrayList<Trigger>();
	    			ArrayList<Trigger> protnumcands = new ArrayList<Trigger>();
			        for (Trigger cand : cands) {
			        	if (trigger.mention.headString.equals(cand.mention.headString))
			        		headcands.add(cand);
			        	else if (cand.name.contains(trigger.mention.headString))
			        		partcands.add(cand);
			        	else if (ProteinFile.containsBioNum(cand) == 1)
			        		bionumcands.add(cand);
			        	else if (ProteinFile.containsProtNum(cand) == 1)
			        		protnumcands.add(cand);
			        }
			        if (headcands.size() > 0)
			        	anscands = headcands;
			        else if (partcands.size() > 0)
			        	anscands = partcands;
			        else if (bionumcands.size() > 0)
			        	anscands = bionumcands;
			        else if (protnumcands.size() > 0)
			        	anscands = protnumcands;
    			}
	    	}
	
		    for (Trigger cand : anscands) {
		    	if (anstrigger == null)
		    		anstrigger = cand;
		    	else if (cand.mention.mentionID > anstrigger.mention.mentionID)
		    		anstrigger = cand;
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


	public static void writeResult(File txtfile, File resultdir, ArrayList<Trigger> triggers, ArrayList<Relation> relations) {
		if (!resultdir.exists())
			resultdir.mkdirs();
			
		StringBuilder sb = new StringBuilder();
		for (Trigger trigger : triggers) {
			sb.append("T" + trigger.id+ "\tExp " + trigger.start + " " + trigger.end + "\t" + trigger.name);
			sb.append("\n");
		}

		int relatid = 1;
		for (Relation relat : relations) {
			sb.append("R" + relatid + "\tCoref Anaphora:T" + relat.anaTrigger.id + " Antecedent:T" + relat.anteTrigger.id);
			sb.append("\n");
			relatid++;
		}

		File a2file = new File(resultdir.getPath() + "/" + FileUtil.removeFileNameExtension(txtfile.getName()) + ".a2");
		FileUtil.saveFile(sb.toString(), a2file);
	}
	
	public static boolean isGoldGGP(Relation relation) {
		return isGoldGGP(relation.anaTrigger, relation.anteTrigger);
	}
	
	public static boolean isGoldGGP(Trigger anatrigger, Trigger antetrigger) {
		for (Relation r : CorefFile.relations) {
			if (r.protIds == null)
				continue;
			if (anatrigger.start >= r.anaTrigger.start && anatrigger.end <= r.anaTrigger.end
					&& anatrigger.start <= r.anaTrigger.headstart && anatrigger.end >= r.anaTrigger.headend) {
				ArrayList<Integer> protids = new ArrayList<Integer>();
				for (Protein p : ProteinFile.proteins) {
					if (antetrigger.start <= p.start && antetrigger.end >= p.end)
						protids.add(p.id);
				}
				for (int i : r.protIds) {
					if (protids.contains(i))
						return true;
				}
			}
		}
		return false;
	}

	
	private static void processGoldData() {
		File txtdir = new File("./data/result0120-wdt+prp");
		if (txtdir.isDirectory()) {
			File[] txtfiles = txtdir.listFiles(new FileFilterImpl(".txt"));
			Arrays.sort(txtfiles);
			int filenum = 0;
			for (File txtfile : txtfiles) {
				logger.info("Extracting from file: " + txtfile.getName() + " " + (++filenum));
				File a1file = new File(FileUtil.removeFileNameExtension(txtfile.getPath()) + ".a1");
			    ProteinFile.getProteins(a1file);
			    File a2file = new File(FileUtil.removeFileNameExtension(txtfile.getPath()) + ".a2");
			    CorefFile.readCorefFile(a2file);
		
			    ArrayList<Relation> relatrelations = new ArrayList<Relation>();
			    ArrayList<Relation> persrelations = new ArrayList<Relation>();
			    ArrayList<Relation> dnprelations = new ArrayList<Relation>();
			    
			    for (Relation r : CorefFile.relations) {
			    	if (!ProteinFile.containsProtein(r.anteTrigger))
			    		continue;
			    	if (r.anaTrigger.name.equalsIgnoreCase("which")
			    			|| r.anaTrigger.name.equalsIgnoreCase("that")
			    			|| r.anaTrigger.name.equalsIgnoreCase("who")
			    			|| r.anaTrigger.name.equalsIgnoreCase("whom")
			    			|| r.anaTrigger.name.equalsIgnoreCase("whose"))
			    		relatrelations.add(r);
			    	else if (r.anaTrigger.name.equalsIgnoreCase("it")
			    			|| r.anaTrigger.name.equalsIgnoreCase("itself")
			    			|| r.anaTrigger.name.equalsIgnoreCase("they")
			    			|| r.anaTrigger.name.equalsIgnoreCase("them")
			    			|| r.anaTrigger.name.equalsIgnoreCase("its")
			    			|| r.anaTrigger.name.equalsIgnoreCase("their"))
			    		persrelations.add(r);
			    	else
			    		dnprelations.add(r);
			    }
			    
			    writeResult(txtfile, new File("./result0120-wdt"), CorefFile.triggers, relatrelations);
			    writeResult(txtfile, new File("./result0120-prp"), CorefFile.triggers, persrelations);
			    writeResult(txtfile, new File("./result0120-dnp"), CorefFile.triggers, dnprelations);
			}
		}
		
	    
	}

}

