package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import utils.CorefFile.Trigger;
import edu.stanford.nlp.dcoref.Mention;

public class ProteinFile {

	public static class Protein {
		public int id;
		public int start;
		public int end;
		public String name;
    }

	public static ArrayList<Protein> proteins;
	public static int lastId;

	public static void getProteins(File a1file) {
	    BufferedReader br;
	    String s;
	    proteins = new ArrayList<Protein>();
	    lastId = 0;
		try {
			br = new BufferedReader(new FileReader(a1file));
			while ((s = br.readLine()) != null) {
				Protein protein = new Protein();
				proteins.add(protein);
				String[] ss = s.split("\t");
				protein.id = Integer.valueOf(ss[0].substring(1));
				String[] ss1 = ss[1].split(" ");
				protein.start = Integer.valueOf(ss1[1]);
				protein.end = Integer.valueOf(ss1[2]);
				protein.name = ss[2];
				if (protein.id > lastId)
					lastId = protein.id;
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean containsProtein(Trigger trigger) {
		int start = trigger.start;
		int end = trigger.end;
		return containsProtein(start, end);
	}
	
	public static boolean containsProtein(Mention m) {
		int start = m.startIndex;
		int end = m.endIndex;
		return containsProtein(start, end);
	}

	public static boolean containsProtein(int start, int end) {
	    for (Protein protein : proteins) {
	      if (start <= protein.start && end >= protein.end)
	        return true;
	    }
	    return false;
	}

	public static int containsProtNum(Trigger trigger) {
	    if (trigger.mention.originalSpan.size() == 1 && containsProtein(trigger))
	      return 1;
	    int start = trigger.start;
	    int end = trigger.end;
	    return containsProtNum(start, end);
	}

	private static int containsProtNum(int start, int end) {
	    int num = 0;
	    for (Protein protein : proteins) {
	      if (start <= protein.start && end >= protein.end)
	        num++;
	    }
	    return num;
	}
	
	public static boolean containsBioEntity(Trigger trigger) {
		return containsBioEntity(trigger.mention);
	}
	
	public static boolean containsBioEntity(Mention m) {
		String mstring = m.toString();
		
		// 位于句首
		if (m.startIndex == 0)
			mstring = m.toString().substring(1).trim();
		
		if (mstring.length() == 0)	//空字符串
			return false;
		
		String[] words = mstring.split(" ");
		for (String word : words) {
			if (!word.toLowerCase().equals(word))
				return true;
			if (containsBioEntity(word))
				return true;
		}
		return false;
	}
	
	private static boolean containsBioEntity(String word) {

    	boolean startLower=true;
    	//begin with digit
    	if(word.charAt(0)>='0' && word.charAt(0)<='9'){
    		boolean containsLetter=false;
    		for(int i=0;i<word.length();i++){
    			if((word.charAt(i)<='z' && word.charAt(i)>='a')||(word.charAt(i)<='Z' && word.charAt(i)>='A'))
    				containsLetter=true;
    		}
    		if(!containsLetter)
    			return false;
    		else
    			return true;
    	}
    	if(word.charAt(0)<='Z' && word.charAt(0)>='A')
    		startLower=false;
    	boolean hasSmall=false;
    	for(int i=1;i<word.length();i++){
    		if(startLower){
    			if((word.charAt(i)<='Z' && word.charAt(i)>='A')||(word.charAt(i)>='0' && word.charAt(i)<='9')|| word.charAt(i)=='-')
    					return true;
    		}
    		else{
    			if((word.charAt(i)>='0' && word.charAt(i)<='9')|| word.charAt(i)=='-')
    				return true;
    			if((word.charAt(i)>='a' && word.charAt(i)<='z')|| word.charAt(i)=='-')
    				hasSmall=true;
    			if(((word.charAt(i)>='A' && word.charAt(i)<='Z')|| word.charAt(i)=='-')&&hasSmall)
    				return true;
    			
    		}
    			
    	}
    	
    	return false;
	}
	
	public static int containsBioNum(Trigger trigger) {
		Mention m = trigger.mention;
		String mstring = m.toString();
		
		// 位于句首
		if (m.startIndex == 0)
			mstring = m.toString().substring(1).trim();
		
		if (mstring.length() == 0)	//空字符串
			return 0;
		
		String[] words = mstring.split(" ");
		int bionum = 0;
		for (String word : words) {
			if (containsBioEntity(word) || !word.toLowerCase().equals(word))
				bionum++;
		}
		return bionum;
	}
	
	// 包括蛋白质，非蛋白质实体
	public static boolean containsEntity(Mention m) {
		if (containsProtein(m))
			return true;
		if (containsBioEntity(m))
			return true;
		if (m.toString().contains("protein")
				|| m.toString().contains("gene")
				|| m.toString().contains("factor")
				|| m.toString().contains("element")
				|| m.toString().contains("receptor")
				|| m.toString().contains("complex")
				|| m.toString().contains("construct")
				//|| m.toString().contains("molecule")
				//|| m.toString().contains("family")
				//|| m.toString().contains("inhibitor")
				//|| m.toString().contains("cell")
				//|| m.toString().contains("compound")
				//|| m.toString().contains("cytokine")
				//|| m.toString().contains("kinases")
				//|| m.toString().contains("domin")
				)
			return true;
		
		return false;
	}

	public static void main(String[] args) {
		File a1file = new File("./data/BioNLP-ST_2011_coreference_development_data/PMID-1335418.a1");
		getProteins(a1file);
	}
 	 
}
