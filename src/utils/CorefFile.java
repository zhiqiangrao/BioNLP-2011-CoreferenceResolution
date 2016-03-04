package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import edu.stanford.nlp.dcoref.Mention;

public class CorefFile {

	public static class Trigger {
	    public int id;
	    public int start;
	    public int end;
	    public String name;
	    public int headstart;
	    public int headend;
	    public String headname;
	    public Mention mention;
	    public String type;
	    public String number;
	}

	public static class Relation {
		public int id;
		public int anaId;
		public int anteId;
		public int[] protIds;
	    public Trigger anaTrigger;
	    public Trigger anteTrigger;
	}

	public static ArrayList<Trigger> triggers;
	public static ArrayList<Relation> relations;

	public static void readCorefFile(File a2file) {
		String s;
	    triggers = new ArrayList<Trigger>();
	    relations = new ArrayList<Relation>();
	    try {
	    	BufferedReader br = new BufferedReader(new FileReader(a2file));
			while ((s = br.readLine()) != null) {
				if (s.startsWith("T")) {
					Trigger trigger = new Trigger();
					triggers.add(trigger);
					String[] ss = s.split("\t");
					trigger.id = Integer.valueOf(ss[0].substring(1));
					String[] locs = ss[1].split(" ");
			        trigger.start = Integer.valueOf(locs[1]);
			        trigger.end = Integer.valueOf(locs[2]);
			        trigger.name = ss[2];
			        if (ss.length == 5) {
			        	String[] hlocs = ss[3].split(" ");
			        	trigger.headstart = Integer.valueOf(hlocs[0]);
			        	trigger.headend = Integer.valueOf(hlocs[1]);
			        	trigger.headname = ss[4];
			        } else {
			        	trigger.headstart = trigger.start;
			        	trigger.headend = trigger.end;
			        	trigger.headname = trigger.name;
			        }
				} else if (s.startsWith("R")) {
					Relation relation = new Relation();
					relations.add(relation);
					String[] ss = s.split("\t");
					relation.id = Integer.valueOf(ss[0].substring(1));
					String[] locs = ss[1].split(" ");
					relation.anaId = Integer.valueOf(locs[1].substring(locs[1].indexOf("T") + 1));
					relation.anteId = Integer.valueOf(locs[2].substring(locs[2].indexOf("T") + 1));
					if (ss.length == 3) {
						String[] prots = ss[2].substring(1, ss[2].length()-1).split(", ");
						relation.protIds = new int[prots.length];
						for (int i=0; i<prots.length; i++) {
							relation.protIds[i] = Integer.valueOf(prots[i].substring(1));
						}
					}
			        for (Trigger trigger : triggers) {
			        	if (relation.anaTrigger != null && relation.anteTrigger != null)
			        		break;
			        	if (trigger.id == relation.anaId)
			        		relation.anaTrigger = trigger;
			        	else if (trigger.id == relation.anteId)
			        		relation.anteTrigger = trigger;
			        }
			  	}
			}
			br.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		File a2file = new File("./data/BioNLP-ST_2011_coreference_development_data/PMID-1675604.a2");
		readCorefFile(a2file);
	}
  
}