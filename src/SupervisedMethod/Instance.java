package SupervisedMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import utils.CorefFile.Trigger;
import utils.FileUtil;
import utils.ProteinFile;

public class Instance {
	
	protected ArrayList<Integer> vector;
	protected int label;
	
	private int[] anaType;	//4, RELATIVE, PERSONAL, DNP, NP
	private int[] anaNum;	//2, UNKNOWN, SINGULAR, PLURAL
	private int anaHeadStr;	//1 protein,gene;  0 others
	
	private int[] anteType;	//4, RELATIVE, PERSONAL, DNP, NP
	private int[] anteNum;	//2, UNKNOWN, SINGULAR, PLURAL
	private int anteProt;	//1,
	private int[] anteProtNum;	//2: 0(00), 1(10), >1(01)
	private int anteBioEntity;	//1,
	private int[] anteBioNum;	//2: 0(00), 1(10), >1(01)
	
	private int[] commonNodeLabel;	//4: NP, S, SBAR, (PRN, VP, WHNP...)
	//private int existPRN;	//1,
	
	private int existNPCC;
	private int anteIsNodeOfNPCC;
	private int existVPCC;
	private int anteIsNodeOfVPCC;
	private int existSCC;
	private int anteIsNodeOfSCC;
	private int anteIsNodeOfNearestS;
	private int anteIsNearestProt;
	
	private int headMatch;
	private int partHeadMatch;
	//private int anteHeadIsFamily;
	
	private int[] menDist;	//3: 1-2, 3-5, 6-
	private int[] tokenDist;		//3: 1-2, 3-5, 6-
	private int[] senDist;	//3: 0, 1, 2 
	
	public void setInstance(Annotation document, Trigger ana, Trigger ante) {
		
		anaType = setType(ana);
		anaNum = setNum(ana);
		anaHeadStr = setAnaHeadStr(ana);
		
		anteType = setType(ante);
		anteNum = setNum(ante);
		anteProt = setProt(ante);
		anteProtNum = setProtNum(ante);
		anteBioEntity = setBioEntity(ante);
		anteBioNum = setBioNum(ante);
		
		commonNodeLabel = setCommonNodeLabel(document, ana, ante);
		
		existNPCC = setNPCC(document, ana);
		anteIsNodeOfNPCC = isNodeOfNPCC(document, ana, ante);
		existVPCC = setVPCC(document, ana);
		anteIsNodeOfVPCC = isNodeOfVPCC(document, ana, ante);
		existSCC = setSCC(document, ana);
		anteIsNodeOfSCC = isNodeOfSCC(document, ana, ante);
		anteIsNodeOfNearestS = isNodeOfNearestS(document, ana, ante);
		anteIsNearestProt = isNearestProt(ana, ante);
		
		headMatch = setHeadMatch(ana, ante);
		partHeadMatch = setPartHeadMatch(ana, ante);
		
		menDist = setMenDist(ana, ante);
		tokenDist = setTokenDist(document, ana, ante);
		senDist = setSenDist(ana, ante);
		
		
		vector = new ArrayList();
		for (int i : anaType) {
			vector.add(i);
		}
		for (int i : anaNum) {
			vector.add(i);
		}
		vector.add(anaHeadStr);
		
		for (int i : anteType) {
			vector.add(i);
		}
		for (int i : anteNum) {
			vector.add(i);
		}
		vector.add(anteProt);
		for (int i : anteProtNum) {
			vector.add(i);
		}
		vector.add(anteBioEntity);
		for (int i : anteBioNum) {
			vector.add(i);
		}
		
		for (int i : commonNodeLabel) {
			vector.add(i);
		}
		
		vector.add(existNPCC);
		vector.add(anteIsNodeOfNPCC);
		vector.add(existVPCC);
		vector.add(anteIsNodeOfVPCC);
		vector.add(existSCC);
		vector.add(anteIsNodeOfSCC);
		vector.add(anteIsNodeOfNearestS);
		vector.add(anteIsNearestProt);
		
		vector.add(headMatch);
		vector.add(partHeadMatch);
		
		for (int i : menDist) {
			vector.add(i);
		}
		for (int i : tokenDist) {
			vector.add(i);
		}
		for (int i : senDist) {
			vector.add(i);
		}
		
	}
	

	private int[] setSenDist(Trigger ana, Trigger ante) {
		int n = ana.mention.sentNum - ante.mention.sentNum;
		if (n == 0)
			return new int[]{1, 0, 0};
		if (n == 1)
			return new int[]{0, 1, 0};
		return new int[]{0, 0, 1};
	}


	private int[] setTokenDist(Annotation document, Trigger ana, Trigger ante) {
		List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);
		CoreLabel token1 = ante.mention.originalSpan.get(ante.mention.originalSpan.size() -1 );
		CoreLabel token2 = ana.mention.originalSpan.get(0);
		int idx1 = -1;
		int idx2 = -1;
		for (int i = 0; i < tokens.size(); i++) {
			if (tokens.get(i) == token1)
				idx1 = i;
			if (tokens.get(i) == token2)
				idx2 = i;
		}
		
		if (idx1 == -1 || idx2 == -1)
			return new int[]{0, 0, 1};
		
		int n = idx2 - idx1;
		if (n >=1 && n <=2)
			return new int[]{1, 0, 0};
		if (n >=3 && n <=5)
			return new int[]{0, 1, 0};
		return new int[]{0, 0, 1};
	}

	private int[] setMenDist(Trigger ana, Trigger ante) {
		int n = ana.id - ante.id;
		if (n >=1 && n <=2)
			return new int[]{1, 0, 0};
		if (n >=3 && n <=5)
			return new int[]{0, 1, 0};
		return new int[]{0, 0, 1};
	}

	private int setPartHeadMatch(Trigger ana, Trigger ante) {
		return ante.name.contains(ana.mention.headString)? 1 : 0;
	}

	private int setHeadMatch(Trigger ana, Trigger ante) {
		return ana.mention.headString.equalsIgnoreCase(ante.mention.headString)? 1 : 0;
	}

	private int isNearestProt(Trigger ana, Trigger ante) {
		return ProteinFile.containsProtein(ana.end + 1, ante.start)? 0 : 1;
	}

	private int isNodeOfNearestS(Annotation document, Trigger ana, Trigger ante) {
		if (existSCC == 0)
			return 0;
		
		Tree root = document.get(CoreAnnotations.SentencesAnnotation.class)
				.get(ana.mention.sentNum-1)
				.get(TreeCoreAnnotations.TreeAnnotation.class);
	    Tree anatree = ana.mention.mentionSubTree;    
	    
	    Tree sontree = anatree;
	    Tree fatree = sontree;
	    while (true) {
	    	fatree = sontree.parent(root);
	    	if (fatree == null || fatree.label().value().equals("ROOT"))
	    		break;
	    	if (fatree.label().value().equals("S")) {
				if (fatree.dominates(ante.mention.mentionSubTree))
					return 1;
				break;
	    	}
    		sontree = fatree;
	    }

		return 0;
	}
	
	private int isNodeOfSCC(Annotation document, Trigger ana, Trigger ante) {
		if (existSCC == 0)
			return 0;
		
		Tree root = document.get(CoreAnnotations.SentencesAnnotation.class)
				.get(ana.mention.sentNum-1)
				.get(TreeCoreAnnotations.TreeAnnotation.class);
	    Tree anatree = ana.mention.mentionSubTree;    
	    // S-CC, SBAR-CC
	    Tree sontree = anatree;
	    Tree fatree = sontree;
	    while (true) {
	    	fatree = sontree.parent(root);
	    	if (fatree == null || fatree.label().value().equals("ROOT"))
	    		break;
	    	if (sontree.label().value().equals("S") || sontree.label().value().equals("SBAR")) {
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
	    			//在第一个S或SBAR中查找
		            for (Tree chd : chdtrees) {
		            	if (!chd.label().value().equals("S") && !chd.label().value().equals("SBAR"))
		            		continue;
	              		if (chd == sontree)
	              			break;
		                if (chd.dominates(ante.mention.mentionSubTree))
		                	return 1;
		            }
	    		}
	    	}
	    	sontree = fatree;
    	}

		return 0;
	}
	
	private int setSCC(Annotation document, Trigger ana) {
		Tree root = document.get(CoreAnnotations.SentencesAnnotation.class)
				.get(ana.mention.sentNum-1)
				.get(TreeCoreAnnotations.TreeAnnotation.class);
	    Tree anatree = ana.mention.mentionSubTree;    
	    // S-CC, SBAR-CC
	    Tree sontree = anatree;
	    Tree fatree = sontree;
	    while (true) {
	    	fatree = sontree.parent(root);
	    	if (fatree == null || fatree.label().value().equals("ROOT"))
	    		break;
	    	if (sontree.label().value().equals("S") || sontree.label().value().equals("SBAR")) {
	    		Tree[] chdtrees = fatree.children();
	    		for (Tree chd : chdtrees) {
	    			if (chd == sontree)
	    				break;
	    			if (chd.label().value().equals("CC")) {
	    				return 1;
	    			}
	    		}
	    	}
	    	sontree = fatree;
    	}
		return 0;
	}
	
	private int isNodeOfVPCC(Annotation document, Trigger ana, Trigger ante) {
		if (existVPCC == 0)
			return 0;
		
		Tree root = document.get(CoreAnnotations.SentencesAnnotation.class)
				.get(ana.mention.sentNum-1)
				.get(TreeCoreAnnotations.TreeAnnotation.class);
	    Tree anatree = ana.mention.mentionSubTree;    
	    // VP-CC
	    Tree sontree = anatree;
	    Tree fatree = sontree;
	    while (true) {
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
	    			while(true) {
	    				sontree = fatree;
	    				fatree = sontree.parent(root);
	    				if (fatree == null || fatree.label().value().equals("ROOT"))
	    					break;
	    				if (fatree.label().value().equals("S")) {
							if (fatree.dominates(ante.mention.mentionSubTree))
								return 1;
							break;
	    				}
    				}
	    		}
	    	}
    		sontree = fatree;
    	}

		return 0;
	}
	
	private int setVPCC(Annotation document, Trigger ana) {
		Tree root = document.get(CoreAnnotations.SentencesAnnotation.class)
				.get(ana.mention.sentNum-1)
				.get(TreeCoreAnnotations.TreeAnnotation.class);
	    Tree anatree = ana.mention.mentionSubTree;    
	    // VP-CC
	    Tree sontree = anatree;
	    Tree fatree = sontree;
	    while (true) {
	    	fatree = sontree.parent(root);
	    	if (fatree == null || fatree.label().value().equals("ROOT"))
	    		break;
	    	if (sontree.label().value().equals("VP")) {
	    		Tree[] chdtrees = fatree.children();
	    		for (Tree chd : chdtrees) {
	    			if (chd == sontree)
	    				break;
	    			if (chd.label().value().equals("CC")) {
	    				return 1;
	    			}
	    		}
	    	}
	    	sontree = fatree;
    	}
		return 0;
	}

	private int isNodeOfNPCC(Annotation document, Trigger ana, Trigger ante) {
		if (existNPCC == 0)
			return 0;
		
		Tree root = document.get(CoreAnnotations.SentencesAnnotation.class)
				.get(ana.mention.sentNum-1)
				.get(TreeCoreAnnotations.TreeAnnotation.class);
	    Tree anatree = ana.mention.mentionSubTree;    
	    // NP-CC
	    Tree sontree = anatree;
	    Tree fatree = sontree;
	    while (true) {
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
		                if (chd.dominates(ante.mention.mentionSubTree))
		                	return 1;
		            }
		            // 第一个NP中没有，在右边结点中查找
                	//if (fatree.dominates(ante.mention.mentionSubTree))
                	//	return 1;
	    		}
	    	}
	    	sontree = fatree;
    	}

		return 0;
	}

	private int setNPCC(Annotation document, Trigger ana) {
		Tree root = document.get(CoreAnnotations.SentencesAnnotation.class)
				.get(ana.mention.sentNum-1)
				.get(TreeCoreAnnotations.TreeAnnotation.class);
	    Tree anatree = ana.mention.mentionSubTree;    
	    // NP-CC
	    Tree sontree = anatree;
	    Tree fatree = sontree;
	    while (true) {
	    	fatree = sontree.parent(root);
	    	if (fatree == null || fatree.label().value().equals("ROOT"))
	    		break;
	    	if (sontree.label().value().equals("NP")) {
	    		Tree[] chdtrees = fatree.children();
	    		for (Tree chd : chdtrees) {
	    			if (chd == sontree)
	    				break;
	    			if (chd.label().value().equals("CC")) {
	    				return 1;
	    			}
	    		}
	    	}
	    	sontree = fatree;
    	}
		return 0;
	}

	private int[] setCommonNodeLabel(Annotation document, Trigger ana, Trigger ante) {
		if (ana.mention.sentNum != ante.mention.sentNum)
			return new int[]{0, 0, 0, 1};
		Tree root = document.get(CoreAnnotations.SentencesAnnotation.class)
				.get(ana.mention.sentNum-1)
				.get(TreeCoreAnnotations.TreeAnnotation.class);
	    Tree anatree = ana.mention.mentionSubTree;
    	Tree antetree = ante.mention.mentionSubTree;
    	List<Tree> pathtrees = root.pathNodeToNode(anatree, antetree);
    	Tree comtree = null;
    	for (Tree tree : pathtrees) {
    		if (tree.dominates(anatree) && tree.dominates(antetree))
    			comtree = tree;
    	}
    	if (comtree.label().value().equals("NP"))
    		return new int[]{1, 0, 0, 0};
    	if (comtree.label().value().equals("S"))
    		return new int[]{0, 1, 0, 0};
    	if (comtree.label().value().equals("SBAR"))
    		return new int[]{0, 0, 1, 0};
    	
		return new int[]{0, 0, 0, 1};
	}

	private int[] setType(Trigger t) {
		if (t.type.equals("RELATIVE"))
			return new int[]{1, 0, 0, 0};
		if (t.type.equals("PERSONAL"))
			return new int[]{0, 1, 0, 0};
		if (t.type.equals("DNP"))
			return new int[]{0, 0, 1, 0};
		if (t.type.equals("NP"))
			return new int[]{0, 0, 0, 1};
		return new int[]{0, 0, 0, 0};
	}
	
	private int[] setNum(Trigger t) {
		if (t.number.equals("UNKNOWN"))
			return new int[]{1,1};
		if (t.number.equals("SINGULAR"))
			return new int[]{1, 0};
		if (t.number.equals("PLURAL"))
			return new int[]{0, 1};
		return new int[]{0, 0};
	}
	
	private int setAnaHeadStr(Trigger t) {
		if (t.mention.headString.equalsIgnoreCase("protein")
				|| t.mention.headString.equalsIgnoreCase("proteins")
				|| t.mention.headString.equalsIgnoreCase("gene")
				|| t.mention.headString.equalsIgnoreCase("genes"))
			return 1;
		return 0;
	}
	
	private int setProt(Trigger t) {
		return ProteinFile.containsProtein(t)? 1: 0;
	}
	
	private int[] setProtNum(Trigger t) {
		int n = ProteinFile.containsProtNum(t);
		if (n == 0)
			return new int[]{0, 0};
		if (n == 1)
			return new int[]{1, 0};
		return new int[]{0, 1};
	}
	
	private int setBioEntity(Trigger t) {
		return ProteinFile.containsBioEntity(t)? 1: 0;
	}
	
	private int[] setBioNum(Trigger t) {
		int n = ProteinFile.containsBioNum(t);
		if (n == 0)
			return new int[]{0, 0};
		if (n == 1)
			return new int[]{1, 0};
		return new int[]{0, 1};
	}
	

	public static void saveInstance(ArrayList<Instance> instances, String name) {
		StringBuilder labsb = new StringBuilder();
		StringBuilder vecsb = new StringBuilder();
		for (Instance inst : instances) {
			labsb.append(inst.label);
			labsb.append("\n");
			for (int i : inst.vector) {
				vecsb.append(i);
				vecsb.append(" ");
			}
			vecsb.append("\n");
		}
		FileUtil.saveFile(labsb.toString(), new File("./model/" + name + ".lab"));
		FileUtil.saveFile(vecsb.toString(), new File("./model/" + name + ".vec"));
	}

	public static ArrayList<Instance> readInstance(String name) {
		ArrayList<Instance> instances = new ArrayList<Instance>();
		
		String labstr = FileUtil.readFile(new File("./model/" + name + ".lab"));
		String vecstr = FileUtil.readFile(new File("./model/" + name + ".vec"));
		String[] labstrs = labstr.split("\n");
		String[] vecstrs = vecstr.split("\n");
		
		if (labstrs.length == vecstrs.length) {
			for (int i = 0; i < labstrs.length; i++) {
				Instance inst = new Instance();
				instances.add(inst);
				inst.label = Integer.valueOf(vecstrs[i]);
				String[] vec= vecstrs[i].split(" ");
				for (String v : vec) {
					inst.vector.add(Integer.valueOf(v));
				}
			}	
		} 
		
		return instances;
	}
	
	
	public static void main(String[] args) {

	}
	
}