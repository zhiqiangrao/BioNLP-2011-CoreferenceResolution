// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   CRScorer.java

package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval;

import java.io.*;
import java.util.*;
import ml.options.*;

// Referenced classes of package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval:
//            Document, Mention, CorefLink

public class CRScorer
{

    public CRScorer()
    {
    }

    public void eval(String goldDataDir, String responseDataDir, String outputDir, String mentionEvalOption, String linkEvalOption, String systemEvalOption, boolean log)
    {
        mTxtFiles = new ArrayList();
        mA1Files = new ArrayList();
        mGoldA2Files = new ArrayList();
        mResA2Files = new ArrayList();
        loadData(goldDataDir, responseDataDir, outputDir, log);
        if(log)
        {
            logWriter.append("********************************************************************************************************\r\n");
            logWriter.append("*** Evaluation details\r\n");
            logWriter.append((new StringBuilder("*** Evaluation settings: mention-option = ")).append(mentionEvalOption).append("\tlink-option = ").append(linkEvalOption).append("\trecall-option = ").append(systemEvalOption).append("\r\n").toString());
            logWriter.append("********************************************************************************************************\r\n\r\n");
        }
        int totalCorrectMention = 0;
        int totalMissingMention = 0;
        int totalSpuriousMention = 0;
        int totalGoldMention = 0;
        int totalResMention = 0;
        int totalCorrectLink = 0;
        int totalMissingLink = 0;
        int totalSpuriousLink = 0;
        int totalGoldLink = 0;
        int totalResLink = 0;
        int totalCorrectMentionLen = 0;
        int size = mTxtFiles.size();
        for(int i = 0; i < size; i++)
        {
            Document document = new Document((File)mTxtFiles.get(i), (File)mA1Files.get(i), (File)mGoldA2Files.get(i), (File)mResA2Files.get(i));
            document.eval(mentionEvalOption, linkEvalOption, systemEvalOption);
            totalGoldMention += document.getGoldMentionCount();
            totalGoldLink += document.getGoldLinkCount();
            totalCorrectMention += document.getCorrectMentionCount();
            totalMissingMention += document.getMissingMentionCount();
            totalSpuriousMention += document.getSpuriousMentionCount();
            totalResMention += document.getResMentionCount();
            totalCorrectLink += document.getCorrectLinkCount();
            totalMissingLink += document.getMissingLinkCount();
            totalSpuriousLink += document.getSpuriousLinkCount();
            totalResLink += document.getResLinkCount();
            totalCorrectMentionLen = (int)((double)totalCorrectMentionLen + document.getCorrectMentionTotalLen());
            if(log)
                writeToLogFile(document);
        }

        evalResultWriter.append("********************************************************************************************************\r\n");
        evalResultWriter.append("*** Evaluation results\r\n");
        evalResultWriter.append((new StringBuilder("*** Evaluation settings: mention-option = ")).append(mentionEvalOption).append("\tlink-option = ").append(linkEvalOption).append("\trecall-option = ").append(systemEvalOption).append("\r\n").toString());
        evalResultWriter.append("********************************************************************************************************\r\n\r\n");
        evalResultWriter.append("===============================================\r\n");
        evalResultWriter.append("EVALUATION OF MENTION DETECTION\r\n");
        evalResultWriter.append((new StringBuilder("Number of gold mentions : ")).append(totalGoldMention).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Number of response mentions : ")).append(totalResMention).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Number of correct response mentions : ")).append(totalCorrectMention).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Number of missing gold mentions : ")).append(totalMissingMention).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Number of spurious response mentions : ")).append(totalSpuriousMention).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Average length in token of correct response mentions : ")).append((double)totalCorrectMentionLen / (double)totalCorrectMention).append("\r\n").toString());
        double P = (double)totalCorrectMention / (double)totalResMention;
        double R = (double)(totalGoldMention - totalMissingMention) / (double)totalGoldMention;
        double F = (2D * P * R) / (P + R);
        evalResultWriter.append((new StringBuilder("P = ")).append(P).append("\tR = ").append(R).append("\tF = ").append(F).append("\r\n").toString());
        System.out.println((new StringBuilder(String.valueOf(totalGoldMention))).append("\t").append(totalResMention).append("\t").append(totalCorrectMention).append("\t").append(totalMissingMention).append("\t").append(totalSpuriousMention).append("\t").append(P).append("\t").append(R).append("\t").append(F).append("\t").append((double)totalCorrectMentionLen / (double)totalCorrectMention).toString());
        evalResultWriter.append((new StringBuilder(String.valueOf(totalGoldMention))).append("\t").append(totalResMention).append("\t").append(totalCorrectMention).append("\t").append(totalMissingMention).append("\t").append(totalSpuriousMention).append("\t").append(P).append("\t").append(R).append("\t").append(F).append("\t").append((double)totalCorrectMentionLen / (double)totalCorrectMention).append("\r\n").toString());
        evalResultWriter.append("===============================================\r\n");
        evalResultWriter.append("EVALUATION OF MENTION LINKING\r\n");
        evalResultWriter.append((new StringBuilder("Number of gold links (points): ")).append(totalGoldLink).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Number of response links (points): ")).append(totalResLink).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Number of correct response links (points): ")).append(totalCorrectLink).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Number of missing gold links (points) : ")).append(totalMissingLink).append("\r\n").toString());
        evalResultWriter.append((new StringBuilder("Number of spurious response links (points): ")).append(totalSpuriousLink).append("\r\n").toString());
        P = (double)totalCorrectLink / (double)totalResLink;
        R = (double)(totalGoldLink - totalMissingLink) / (double)totalGoldLink;
        F = (2D * P * R) / (P + R);
        evalResultWriter.append((new StringBuilder("P = ")).append(P).append("\tR = ").append(R).append("\tF = ").append(F).append("\r\n").toString());
        System.out.println((new StringBuilder(String.valueOf(totalGoldLink))).append("\t").append(totalResLink).append("\t").append(totalCorrectLink).append("\t").append(totalMissingLink).append("\t").append(totalSpuriousLink).append("\t").append(P).append("\t").append(R).append("\t").append(F).toString());
        evalResultWriter.append((new StringBuilder(String.valueOf(totalGoldLink))).append("\t").append(totalResLink).append("\t").append(totalCorrectLink).append("\t").append(totalMissingLink).append("\t").append(totalSpuriousLink).append("\t").append(P).append("\t").append(R).append("\t").append(F).append("\r\n").toString());
        evalResultWriter.close();

        if(evalResultWriter != null)
            evalResultWriter.close();
        if(log && logWriter != null)
            logWriter.close();
        
        if(evalResultWriter != null)
            evalResultWriter.close();
        if(log && logWriter != null)
            logWriter.close();
        return;
    }

    private void writeToLogFile(Document doc)
    {
        String doctext = doc.getDoctext();
        logWriter.append("***********************************************\r\n");
        logWriter.append((new StringBuilder("PMID = ")).append(doc.getPmid()).append("\r\n").toString());
        logWriter.append("EVALUATION OF MENTION DETECTION\r\n");
        logWriter.append("-----------------------------------------------\r\n");
        HashMap correctMentionMap = doc.getCorrectMentionMap();
        Set keySet = correctMentionMap.keySet();
        Mention keyMention;
        for(Iterator iterator = keySet.iterator(); iterator.hasNext(); logWriter.append((new StringBuilder("----------- gold mention\t")).append(keyMention).append(doctext.substring(keyMention.getBegin(), keyMention.getEnd())).append("\t").append("\r\n").toString()))
        {
            Mention resMention = (Mention)iterator.next();
            keyMention = (Mention)correctMentionMap.get(resMention);
            logWriter.append((new StringBuilder("Correct response mention\t")).append(resMention).append(doctext.substring(resMention.getBegin(), resMention.getEnd())).append("\t").append("\r\n").toString());
        }

        logWriter.append("-----------------------------------------------\r\n");
        ArrayList spuriousMentions = doc.getSpuriousMentions();
        Mention resMention;
        for(Iterator iterator1 = spuriousMentions.iterator(); iterator1.hasNext(); logWriter.append((new StringBuilder("Spurious response mention\t")).append(resMention).append(doctext.substring(resMention.getBegin(), resMention.getEnd())).append("\t").append("\r\n").toString()))
            resMention = (Mention)iterator1.next();

        logWriter.append("-----------------------------------------------\r\n");
        ArrayList missingMentions = doc.getMissingMentions();
        Mention goldMention;
        for(Iterator iterator2 = missingMentions.iterator(); iterator2.hasNext(); logWriter.append((new StringBuilder("Missing gold mention\t")).append(goldMention).append(doctext.substring(goldMention.getBegin(), goldMention.getEnd())).append("\t").append("\r\n").toString()))
            goldMention = (Mention)iterator2.next();

        logWriter.append("===============================================\r\n");
        logWriter.append("EVALUATION OF MENTION LINKING\r\n");
        logWriter.append("-----------------------------------------------\r\n");
        HashMap correctLinkMap = doc.getCorrectLinkMap();
        Set keyLinkSet = correctLinkMap.keySet();
        CorefLink keyLink;
        for(Iterator iterator3 = keyLinkSet.iterator(); iterator3.hasNext(); logWriter.append((new StringBuilder("----------- gold link\t")).append(keyLink).toString()))
        {
            CorefLink resLink = (CorefLink)iterator3.next();
            keyLink = (CorefLink)correctLinkMap.get(resLink);
            logWriter.append((new StringBuilder("Correct response link\t")).append(resLink).toString());
        }

        logWriter.append("-----------------------------------------------\r\n");
        ArrayList spuriousLinks = doc.getSpuriousLinks();
        CorefLink resLink;
        for(Iterator iterator4 = spuriousLinks.iterator(); iterator4.hasNext(); logWriter.append((new StringBuilder("Spurious response link\t")).append(resLink).toString()))
            resLink = (CorefLink)iterator4.next();

        logWriter.append("-----------------------------------------------\r\n");
        ArrayList missingLinks = doc.getMissingLinks();
        CorefLink goldLink;
        for(Iterator iterator5 = missingLinks.iterator(); iterator5.hasNext(); logWriter.append((new StringBuilder("Missing gold link\t")).append(goldLink).toString()))
            goldLink = (CorefLink)iterator5.next();

    }

    public void loadData(String goldDataDir, String responseDataDir, String outputDir, boolean log)
    {
        File goldDir = new File(goldDataDir);
        File resDir = new File(responseDataDir);
        File outDir = new File(outputDir);
        if(goldDir.exists() && goldDir.isDirectory() && resDir.exists() && resDir.isDirectory() && outDir.exists())
            outDir.isDirectory();
        mTxtFiles = new ArrayList();
        File goldFiles[] = goldDir.listFiles();
        for(int i = 0; i < goldFiles.length; i++)
            if(!goldFiles[i].isDirectory() && goldFiles[i].getName().endsWith(".txt"))
                mTxtFiles.add(goldFiles[i]);

        HashMap name2Files2Map = new HashMap();
        for(int i = 0; i < goldFiles.length; i++)
            if(!goldFiles[i].isDirectory() && goldFiles[i].getName().endsWith(".a1"))
                name2Files2Map.put(goldFiles[i].getName().substring(0, goldFiles[i].getName().length() - 3), goldFiles[i]);

        HashMap name2Files3Map = new HashMap();
        for(int i = 0; i < goldFiles.length; i++)
            if(!goldFiles[i].isDirectory() && goldFiles[i].getName().endsWith(".a2"))
                name2Files3Map.put(goldFiles[i].getName().substring(0, goldFiles[i].getName().length() - 3), goldFiles[i]);

        File resFiles[] = resDir.listFiles();
        HashMap name2Files4Map = new HashMap();
        for(int i = 0; i < resFiles.length; i++)
            if(!resFiles[i].isDirectory() && resFiles[i].getName().endsWith(".a2"))
                name2Files4Map.put(resFiles[i].getName().substring(0, resFiles[i].getName().length() - 3), resFiles[i]);

        mA1Files = new ArrayList();
        mGoldA2Files = new ArrayList();
        for(int i = 0; i < mTxtFiles.size(); i++)
        {
            String name = ((File)mTxtFiles.get(i)).getName();
            name = name.substring(0, name.length() - 4);
            if(name2Files2Map.containsKey(name) && name2Files3Map.containsKey(name) && name2Files4Map.containsKey(name))
            {
                mA1Files.add((File)name2Files2Map.get(name));
                mGoldA2Files.add((File)name2Files3Map.get(name));
                mResA2Files.add((File)name2Files4Map.get(name));
            }
        }

        try
        {
            evalResultWriter = new PrintWriter(new File((new StringBuilder(String.valueOf(outputDir))).append(File.separatorChar).append("eval.results").toString()));
            if(log)
                logWriter = new PrintWriter(new File((new StringBuilder(String.valueOf(outputDir))).append(File.separatorChar).append("eval.details").toString()));
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
            if(evalResultWriter != null)
                evalResultWriter.close();
            if(logWriter != null)
                logWriter.close();
        }
    }

    public static void main(String args[])
    {
        /*Options opt = new Options(args, 3);
        opt.getSet().addOption("details", ml.options.Options.Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("mention", ml.options.Options.Separator.EQUALS, ml.options.Options.Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("link", ml.options.Options.Separator.EQUALS, ml.options.Options.Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("recall", ml.options.Options.Separator.EQUALS, ml.options.Options.Multiplicity.ZERO_OR_ONE);
        String mentionEvalOption = PARTIAL_MENTION;
        String linkEvalOption = GGP_LINK;
        String systemEvalOption = SYSTEM_EVAL;
        boolean details = false;
        if(!opt.check())
        {
            System.out.println("Invalid option");
            System.exit(1);
        }
        boolean invalid = false;
        if(opt.getSet().isSet("details"))
            details = true;
        if(opt.getSet().isSet("mention"))
        {
            mentionEvalOption = opt.getSet().getOption("mention").getResultValue(0);
            if(!mentionEvalOption.equals(STRICT_MENTION) && !mentionEvalOption.equals(PARTIAL_MENTION) && !mentionEvalOption.equals(HEAD_MENTION))
                invalid = true;
        }
        if(opt.getSet().isSet("link"))
        {
            linkEvalOption = opt.getSet().getOption("link").getResultValue(0);
            if(!linkEvalOption.equals(SURFACE_LINK) && !linkEvalOption.equals(ATOM_LINK) && !linkEvalOption.equals(GGP_LINK))
                invalid = true;
        }
        if(opt.getSet().isSet("recall"))
        {
            systemEvalOption = opt.getSet().getOption("recall").getResultValue(0);
            if(!systemEvalOption.equals(SYSTEM_EVAL) && !systemEvalOption.equals(ALG_EVAL))
                invalid = true;
        }
        if(invalid)
        {
            System.out.println("Invalid option value.");
            System.exit(1);
        }
        String indir1 = (String)opt.getSet().getData().get(0);
        String indir2 = (String)opt.getSet().getData().get(1);
        String outdir = (String)opt.getSet().getData().get(2);*/
    	
    	String indir1 = "./data/BioNLP-ST_2011_coreference_development_data";
    	String indir2 = "./result";
    	String outdir = "./result";
    	String mentionEvalOption = PARTIAL_MENTION;
    	String linkEvalOption = GGP_LINK;
    	String systemEvalOption = SYSTEM_EVAL;
    	boolean details = true;
    	
        CRScorer crScorer = new CRScorer();
        crScorer.eval(indir1, indir2, outdir, mentionEvalOption, linkEvalOption, systemEvalOption, details);
        System.out.println("Done");
    }

    public static String STRICT_MENTION = "strict";
    public static String PARTIAL_MENTION = "partial";
    public static String HEAD_MENTION = "head";
    public static String ATOM_LINK = "atom";
    public static String SURFACE_LINK = "surface";
    public static String GGP_LINK = "ggp";
    public static String ALG_EVAL = "algorithm";
    public static String SYSTEM_EVAL = "system";
    private ArrayList mTxtFiles;
    private ArrayList mA1Files;
    private ArrayList mGoldA2Files;
    private ArrayList mResA2Files;
    private PrintWriter evalResultWriter;
    private PrintWriter logWriter;

}
