// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Document.java

package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval;

import java.io.*;
import java.util.*;

// Referenced classes of package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval:
//            GGP, Mention, CorefLink, CRScorer

public class Document
{

    public Document(File txtFile, File a1File, File golda2File, File respa2File)
    {
        ggps = new ArrayList();
        ggp2mentionMap = new HashMap();
        goldMentions = new ArrayList();
        goldLinks = new ArrayList();
        resMentions = new ArrayList();
        resLinks = new ArrayList();
        goldTargetLinks = new ArrayList();
        resTargetLinks = new ArrayList();
        goldTargetMentions = new ArrayList();
        resTargetMentions = new ArrayList();
        correctMentionMap = new HashMap();
        spuriousMentions = new ArrayList();
        missingMentions = new ArrayList();
        correctLinkMap = new HashMap();
        spuriousLinks = new ArrayList();
        missingLinks = new ArrayList();
        allMentionMap = new HashMap();
        try
        {
            String docText = read(txtFile);
            setDoctext(docText);
            setPmid(txtFile.getName().substring(0, txtFile.getName().indexOf('.')));
            readA1(a1File, ggps);
            Mention mention;
            for(Iterator iterator = ggps.iterator(); iterator.hasNext(); allMentionMap.put(mention, mention))
            {
                GGP ggp = (GGP)iterator.next();
                mention = new Mention(ggp.getBegin(), ggp.getEnd(), ggp.getId(), docText.substring(ggp.getBegin(), ggp.getEnd()));
                ggp2mentionMap.put(ggp, mention);
            }

            readA2New(golda2File, goldMentions, goldLinks, 0);
            readA2(respa2File, resMentions, resLinks, 0);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void setDoctext(String doctext)
    {
        this.doctext = doctext;
    }

    public String getPmid()
    {
        return pmid;
    }

    public void setPmid(String pmid)
    {
        this.pmid = pmid;
    }

    public String getDoctext()
    {
        return doctext;
    }

    public HashMap getCorrectMentionMap()
    {
        return correctMentionMap;
    }

    public ArrayList getSpuriousMentions()
    {
        return spuriousMentions;
    }

    public ArrayList getMissingMentions()
    {
        return missingMentions;
    }

    public HashMap getCorrectLinkMap()
    {
        return correctLinkMap;
    }

    public ArrayList getSpuriousLinks()
    {
        return spuriousLinks;
    }

    public ArrayList getMissingLinks()
    {
        return missingLinks;
    }

    private String read(File file)
        throws IOException
    {
        StringBuilder text;
        BufferedReader scanner;
        text = new StringBuilder();
        scanner = new BufferedReader(new FileReader(file));
        for(String tmp = null; (tmp = scanner.readLine()) != null;)
            text.append((new StringBuilder(String.valueOf(tmp))).append("\n").toString());

        scanner.close();

        return text.toString();
    }

    private void readA1(File file, ArrayList ggps)
        throws IOException
    {
        BufferedReader scanner = new BufferedReader(new FileReader(file));
        for(String tmp = null; (tmp = scanner.readLine()) != null;)
        {
            tmp = (new StringBuilder(String.valueOf(tmp))).append(' ').toString();
            StringTokenizer st = new StringTokenizer(tmp);
            if(st.hasMoreTokens())
            {
                String id = st.nextToken();
                st.nextToken();
                int begin = Integer.valueOf(st.nextToken()).intValue();
                int end = Integer.valueOf(st.nextToken()).intValue();
                GGP ggp = new GGP(begin, end, id);
                ggps.add(ggp);
            }
        }

        scanner.close();

        return;
    }

    private void readA2(File file, ArrayList mentions, ArrayList links, int adjustOffset)
        throws IOException
    {
        BufferedReader scanner = new BufferedReader(new FileReader(file));
        HashMap id2corefMap = new HashMap();
        for(String tmp = null; (tmp = scanner.readLine()) != null;)
        {
            tmp = (new StringBuilder(String.valueOf(tmp))).append(' ').toString();
            StringTokenizer st = new StringTokenizer(tmp);
            if(st.hasMoreTokens())
            {
                String t1 = st.nextToken();
                String t2 = st.nextToken();
                String t3 = st.nextToken();
                String t4 = st.nextToken();
                if(t2.equals("Exp"))
                {
                    Mention coref = new Mention(Integer.valueOf(t3).intValue() + adjustOffset, Integer.valueOf(t4).intValue() + adjustOffset, t1, null);
                    String splits[] = tmp.split("\t");
                    if(splits.length == 5)
                    {
                        String minindice = splits[splits.length - 2];
                        coref.setMinBegin(Integer.valueOf(minindice.substring(0, minindice.indexOf(' '))).intValue());
                        coref.setMinEnd(Integer.valueOf(minindice.substring(minindice.indexOf(' ') + 1, minindice.length())).intValue());
                        coref.setMin(splits[splits.length - 1].trim());
                    }
                    mentions.add(coref);
                    id2corefMap.put(t1, coref);
                } else
                if(t2.equals("Coref"))
                {
                    String t3S[] = t3.split(":");
                    String t4S[] = t4.split(":");
                    Mention anaCoref = (Mention)id2corefMap.get(t3S[1]);
                    Mention antCoref = (Mention)id2corefMap.get(t4S[1]);
                    CorefLink link = new CorefLink(anaCoref.getBegin(), anaCoref.getEnd(), t1, anaCoref);
                    link.addAntecedent(antCoref);
                    String tmpS = null;
                    while(st.hasMoreTokens()) 
                    {
                        tmpS = st.nextToken();
                        if(tmpS.startsWith("Antecedent"))
                        {
                            t4S = t4.split(":");
                            antCoref = (Mention)id2corefMap.get(t4S[1]);
                            link.addAntecedent(antCoref);
                        } else
                        {
                            link.setReferenceGGPs(tmpS);
                        }
                    }
                    if(anaCoref != null && antCoref != null)
                        links.add(link);
                }
            }
        }

        scanner.close();
        return;
    }

    private void readA2New(File file, ArrayList mentions, ArrayList links, int adjustOffset)
        throws IOException
    {
        BufferedReader scanner = new BufferedReader(new FileReader(file));
        HashMap id2corefMap = new HashMap();
        for(String tmp = null; (tmp = scanner.readLine()) != null;)
        {
            tmp = (new StringBuilder(String.valueOf(tmp))).append(' ').toString();
            StringTokenizer st = new StringTokenizer(tmp);
            if(st.hasMoreTokens())
            {
                String t1 = st.nextToken();
                String t2 = st.nextToken();
                String t3 = st.nextToken();
                String t4 = st.nextToken();
                if(t2.equals("Exp"))
                {
                    Mention coref = new Mention(Integer.valueOf(t3).intValue() + adjustOffset, Integer.valueOf(t4).intValue() + adjustOffset, t1, null);
                    String splits[] = tmp.split("\t");
                    if(splits.length == 5)
                    {
                        coref.setType(splits[4].trim());
                        String subsplits[] = (new StringBuilder(String.valueOf(splits[3]))).append(" ").toString().split(" ");

                        coref.setMinBegin(Integer.valueOf(subsplits[0]).intValue());
                        coref.setMinEnd(Integer.valueOf(subsplits[1]).intValue());
                        coref.setMin(splits[4]);
                    } else
                    {
                        coref.setMin(splits[2]);
                    }
                    mentions.add(coref);
                    id2corefMap.put(t1, coref);
                } else
                if(t2.equals("Coref"))
                {
                    String t3S[] = t3.split(":");
                    String t4S[] = t4.split(":");
                    Mention anaCoref = (Mention)id2corefMap.get(t3S[1]);
                    Mention antCoref = (Mention)id2corefMap.get(t4S[1]);
                    CorefLink link = new CorefLink(anaCoref.getBegin(), anaCoref.getEnd(), t1, anaCoref);
                    link.addAntecedent(antCoref);
                    String tmpS = null;
                    while(st.hasMoreTokens()) 
                    {
                        tmpS = st.nextToken();
                        if(tmpS.startsWith("Antecedent"))
                        {
                            t4S = t4.split(":");
                            antCoref = (Mention)id2corefMap.get(t4S[1]);
                            link.addAntecedent(antCoref);
                        } else
                        {
                            link.setReferenceGGPs(tmpS);
                        }
                    }
                    if(anaCoref != null && antCoref != null)
                        links.add(link);
                }
            }
        }

        
        scanner.close();
        return;
    }

    private void prepareTargetMentionsAndLinks(boolean gold, ArrayList mentions, ArrayList originalLinks, ArrayList targetMentions, ArrayList targetLinks, String evalMode)
    {
        if(evalMode.equals(CRScorer.ATOM_LINK) || evalMode.equals(CRScorer.GGP_LINK))
        {
            targetMentions.clear();
            ArrayList retArray = new ArrayList();
            int n = ggps.size();
            int noOfMention = mentions.size();
            for(int j = 0; j < noOfMention; j++)
            {
                retArray.clear();
                Mention mention = (Mention)mentions.get(j);
                for(int i = 0; i < n; i++)
                {
                    GGP ggp = (GGP)ggps.get(i);
                    if(mention.getBegin() <= ggp.getBegin() && mention.getEnd() >= ggp.getEnd())
                        retArray.add(ggp);
                }

                mention.setEvalpoint(retArray.size());
                mention.setGgps(retArray);
            }

            targetLinks.clear();
            int noOfLink = originalLinks.size();
            HashMap linkMap = new HashMap();
            for(int j = 0; j < noOfLink; j++)
            {
                CorefLink link = (CorefLink)originalLinks.get(j);
                Mention anaphor = link.getAnaphor();
                linkMap.put(anaphor, link.getAntecedents());
            }

            for(int j = 0; j < noOfLink; j++)
            {
                CorefLink link = (CorefLink)originalLinks.get(j);
                Mention anaphor = link.getAnaphor();
                ArrayList antecedents = link.getAntecedents();
                ArrayList newAnts = antecedents;
                ArrayList inclusiveGGPs = new ArrayList();
                ArrayList parentInclusiveGGPs = new ArrayList();
                int evalpoint = 0;
                do
                {
                    if(anaphor.getEvalpoint() >= evalpoint)
                    {
                        evalpoint = anaphor.getEvalpoint();
                        inclusiveGGPs.clear();
                        inclusiveGGPs.addAll(anaphor.getGgps());
                    }
                    int totalAntPoint = 0;
                    parentInclusiveGGPs.clear();
                    for(int k = 0; k < antecedents.size(); k++)
                    {
                        Mention ante = (Mention)antecedents.get(k);
                        totalAntPoint += ante.getEvalpoint();
                        parentInclusiveGGPs.addAll(ante.getGgps());
                    }

                    if(totalAntPoint >= evalpoint)
                    {
                        evalpoint = totalAntPoint;
                        newAnts = antecedents;
                        inclusiveGGPs.clear();
                        inclusiveGGPs.addAll(parentInclusiveGGPs);
                    }
                    if(antecedents.size() > 0 && linkMap.containsKey(antecedents.get(0)))
                        antecedents = (ArrayList)linkMap.get(antecedents.get(0));
                    else
                        antecedents = null;
                } while(antecedents != null);
                if(evalpoint > 0)
                {
                    if(evalMode.equals(CRScorer.ATOM_LINK))
                    {
                        CorefLink newLink = new CorefLink(link.getBegin(), link.getEnd(), link.getId(), anaphor, newAnts);
                        newLink.setEvalpoint(evalpoint);
                        targetLinks.add(newLink);
                    } else
                    if(evalMode.equals(CRScorer.GGP_LINK))
                    {
                        CorefLink newLink;
                        for(Iterator iterator3 = inclusiveGGPs.iterator(); iterator3.hasNext(); targetLinks.add(newLink))
                        {
                            GGP ggp = (GGP)iterator3.next();
                            Mention ggpMention = (Mention)ggp2mentionMap.get(ggp);
                            newLink = new CorefLink(link.getBegin(), link.getEnd(), link.getId(), anaphor);
                            newLink.addAntecedent(ggpMention);
                            newLink.setEvalpoint(1);
                        }

                    }
                    if(gold)
                    {
                        if(targetMentions.indexOf(anaphor) == -1)
                        {
                            anaphor.setEvalpoint(evalpoint);
                            targetMentions.add(anaphor);
                        }
                        antecedents = link.getAntecedents();
                        for(int k = 0; k < antecedents.size(); k++)
                        {
                            Mention ante = (Mention)antecedents.get(k);
                            ante.setEvalpoint(evalpoint);
                            if(targetMentions.indexOf(ante) == -1)
                                targetMentions.add(ante);
                        }

                    }
                }
            }

            if(!gold)
            {
                targetMentions.addAll(mentions);
                Mention mention;
                for(Iterator iterator2 = targetMentions.iterator(); iterator2.hasNext(); mention.setEvalpoint(1))
                    mention = (Mention)iterator2.next();

            }
        } else
        if(evalMode.equals(CRScorer.SURFACE_LINK))
        {
            targetLinks.addAll(originalLinks);
            CorefLink corefLink;
            for(Iterator iterator = targetLinks.iterator(); iterator.hasNext(); corefLink.setEvalpoint(1))
                corefLink = (CorefLink)iterator.next();

            targetMentions.addAll(mentions);
            Mention mention;
            for(Iterator iterator1 = targetMentions.iterator(); iterator1.hasNext(); mention.setEvalpoint(1))
                mention = (Mention)iterator1.next();

        }
    }

    public void eval(String mentionOption, String linkOption, String systemEvalOption)
    {
        System.out.println((new StringBuilder("Evaluating ")).append(getPmid()).append("...").toString());
        preprocessing();
        prepareTargetMentionsAndLinks(true, goldMentions, goldLinks, goldTargetMentions, goldTargetLinks, linkOption);
        prepareTargetMentionsAndLinks(false, resMentions, resLinks, resTargetMentions, resTargetLinks, linkOption);
        mentionDetectionEval(mentionOption);
        linkEval(systemEvalOption);
    }

    private void preprocessing()
    {
        for(int i = 0; i < goldLinks.size(); i++)
        {
            CorefLink link = (CorefLink)goldLinks.get(i);
            ArrayList antecedents = link.antecedents;
            for(Iterator iterator = antecedents.iterator(); iterator.hasNext();)
            {
                Mention ante = (Mention)iterator.next();
                if(link.getAnaphor().getBegin() <= ante.getBegin())
                {
                    System.out.println((new StringBuilder("Invalid gold link id=")).append(link.getId()).append(" removed (antecedent appears after anaphor).").toString());
                    goldLinks.remove(i);
                    i--;
                }
            }

        }

        for(int i = 0; i < resLinks.size(); i++)
        {
            CorefLink link = (CorefLink)resLinks.get(i);
            ArrayList antecedents = link.antecedents;
            for(Iterator iterator1 = antecedents.iterator(); iterator1.hasNext();)
            {
                Mention ante = (Mention)iterator1.next();
                if(link.getAnaphor().getBegin() <= ante.getBegin())
                {
                    System.out.println((new StringBuilder("Invalid response link id=")).append(link.getId()).append(" removed (antecedent appears after anaphor).").toString());
                    resLinks.remove(i);
                    i--;
                }
            }

        }

        ArrayList anaphors = new ArrayList();
        for(int i = 0; i < resLinks.size(); i++)
        {
            CorefLink link = (CorefLink)resLinks.get(i);
            Mention anaphor = link.getAnaphor();
            if(anaphors.contains(anaphor))
            {
                resLinks.remove(i);
                i--;
            } else
            {
                anaphors.add(anaphor);
            }
        }

    }

    public void printToDebug(ArrayList mentions, ArrayList originalLinks, ArrayList targetMentions, ArrayList targetLinks)
    {
        System.out.println("DEBUG Mentions: ");
        Mention mention;
        for(Iterator iterator = mentions.iterator(); iterator.hasNext(); System.out.print((new StringBuilder("(")).append(mention.getId()).append(",").append(mention.getEvalpoint()).append(")").append("\t").toString()))
            mention = (Mention)iterator.next();

        System.out.println();
        System.out.println("DEBUG Target Mentions: ");
        Mention mention1;
        for(Iterator iterator1 = targetMentions.iterator(); iterator1.hasNext(); System.out.print((new StringBuilder("(")).append(mention1.getId()).append(",").append(mention1.getEvalpoint()).append(")").append("\t").toString()))
            mention1 = (Mention)iterator1.next();

        System.out.println();
        System.out.println("DEBUG Links: ");
        CorefLink link;
        for(Iterator iterator2 = originalLinks.iterator(); iterator2.hasNext(); System.out.print(link))
            link = (CorefLink)iterator2.next();

        System.out.println();
        System.out.println("DEBUG Target Links: ");
        CorefLink link1;
        for(Iterator iterator3 = targetLinks.iterator(); iterator3.hasNext(); System.out.print(link1))
            link1 = (CorefLink)iterator3.next();

        System.out.println();
    }

    public void mentionDetectionEval(String mentionCriterion)
    {
        correctMentionMap.clear();
        missingMentions.clear();
        spuriousMentions.clear();
        missingMentions.addAll(goldTargetMentions);
        if(mentionCriterion.equals(CRScorer.STRICT_MENTION))
        {
            int goldMentionSize = goldTargetMentions.size();
            int resMentionSize = resTargetMentions.size();
            for(int i = 0; i < resMentionSize; i++)
            {
                Mention resMention = (Mention)resTargetMentions.get(i);
                boolean found = false;
                for(int j = 0; j < goldMentionSize && !found; j++)
                {
                    Mention goldMention = (Mention)goldTargetMentions.get(j);
                    if(goldMention.getBegin() == resMention.getBegin() && goldMention.getEnd() == resMention.getEnd())
                    {
                        found = true;
                        correctMentionMap.put(resMention, goldMention);
                    }
                }

                if(!found)
                    spuriousMentions.add(resMention);
                else
                    missingMentions.remove(correctMentionMap.get(resMention));
            }

        } else
        if(mentionCriterion.equals(CRScorer.PARTIAL_MENTION))
        {
            int goldMentionSize = goldTargetMentions.size();
            int resMentionSize = resTargetMentions.size();
            for(int i = 0; i < resMentionSize; i++)
            {
                Mention resMention = (Mention)resTargetMentions.get(i);
                boolean found = false;
                for(int j = 0; j < goldMentionSize && !found; j++)
                {
                    Mention goldMention = (Mention)goldTargetMentions.get(j);
                    if(goldMention.getBegin() <= resMention.getBegin() && goldMention.getEnd() >= resMention.getEnd() && goldMention.getMinBegin() >= resMention.getBegin() && goldMention.getMinEnd() <= resMention.getEnd())
                    {
                        found = true;
                        correctMentionMap.put(resMention, goldMention);
                    }
                }

                if(!found)
                    spuriousMentions.add(resMention);
                else
                    missingMentions.remove(correctMentionMap.get(resMention));
            }

        }
        allMentionMap.putAll(correctMentionMap);
    }

    public void linkEval(String evalMode)
    {
        correctLinkMap.clear();
        missingLinks.clear();
        spuriousLinks.clear();
        if(evalMode.equals(CRScorer.ALG_EVAL))
        {
            int n = goldTargetLinks.size();
            for(int j = 0; j < n; j++)
            {
                CorefLink link = (CorefLink)goldTargetLinks.get(j);
                Mention ana = link.getAnaphor();
                ArrayList ants = link.getAntecedents();
                boolean addThisLink = true;
                if(!allMentionMap.containsValue(ana))
                    addThisLink = false;
                for(int i = 0; i < ants.size() && addThisLink; i++)
                    if(!allMentionMap.containsValue(ants.get(i)))
                        addThisLink = false;

                if(addThisLink)
                    missingLinks.add(link);
            }

            goldTargetLinks.clear();
            goldTargetLinks.addAll(missingLinks);
        } else
        {
            missingLinks.addAll(goldTargetLinks);
        }
        int resLinkSize = resTargetLinks.size();
        for(int i = 0; i < resLinkSize; i++)
        {
            CorefLink resLink = (CorefLink)resTargetLinks.get(i);
            Mention resAna = resLink.getAnaphor();
            ArrayList resAnts = resLink.getAntecedents();
            boolean found = false;
            int goldLinkSize = goldTargetLinks.size();
            for(int j = 0; j < goldLinkSize && !found; j++)
            {
                CorefLink goldLink = (CorefLink)goldTargetLinks.get(j);
                Mention goldAna = goldLink.getAnaphor();
                ArrayList goldAnts = goldLink.getAntecedents();
                boolean allAntsMatch = true;
                if(resAnts.size() != goldAnts.size())
                {
                    allAntsMatch = false;
                } else
                {
                    for(int k = 0; k < resAnts.size() && allAntsMatch; k++)
                    {
                        Mention resAnt = (Mention)resAnts.get(k);
                        Mention goldAnt = (Mention)goldAnts.get(k);
                        if(!allMentionMap.containsKey(resAnt) || allMentionMap.get(resAnt) != goldAnt)
                            allAntsMatch = false;
                    }

                }
                if(allMentionMap.containsKey(resAna) && allMentionMap.get(resAna) == goldAna && allAntsMatch)
                {
                    found = true;
                    correctLinkMap.put(resLink, goldLink);
                }
            }

            if(!found)
                spuriousLinks.add(resLink);
            else
                missingLinks.remove(correctLinkMap.get(resLink));
        }

    }

    public double getCorrectMentionTotalLen()
    {
        int totalLen = 0;
        Collection keys = correctMentionMap.keySet();
        for(Iterator iterator = keys.iterator(); iterator.hasNext();)
        {
            Mention mention = (Mention)iterator.next();
            String s = doctext.substring(mention.getBegin(), mention.getEnd());
            String splits[] = (new StringBuilder(String.valueOf(s))).append(" ").toString().split(" ");
            totalLen += splits.length;
        }

        if(keys.size() != 0)
            System.out.println((new StringBuilder("\tDEBUG ")).append((double)totalLen / (double)keys.size()).toString());
        return (double)totalLen;
    }

    public int getCorrectMentionCount()
    {
        return correctMentionMap.size();
    }

    public int getMissingMentionCount()
    {
        return missingMentions.size();
    }

    public int getSpuriousMentionCount()
    {
        return spuriousMentions.size();
    }

    public int getGoldMentionCount()
    {
        return goldTargetMentions.size();
    }

    public int getResMentionCount()
    {
        return resTargetMentions.size();
    }

    public int getCorrectLinkCount()
    {
        int ret = 0;
        Collection values = correctLinkMap.values();
        for(Iterator iterator = values.iterator(); iterator.hasNext();)
        {
            CorefLink corefLink = (CorefLink)iterator.next();
            ret += corefLink.getEvalpoint();
        }

        return ret;
    }

    public int getMissingLinkCount()
    {
        int ret = 0;
        for(Iterator iterator = missingLinks.iterator(); iterator.hasNext();)
        {
            CorefLink corefLink = (CorefLink)iterator.next();
            ret += corefLink.getEvalpoint();
        }

        return ret;
    }

    public int getSpuriousLinkCount()
    {
        int ret = 0;
        for(Iterator iterator = spuriousLinks.iterator(); iterator.hasNext();)
        {
            CorefLink corefLink = (CorefLink)iterator.next();
            ret += corefLink.getEvalpoint();
        }

        return ret;
    }

    public int getGoldLinkCount()
    {
        int ret = 0;
        for(Iterator iterator = goldTargetLinks.iterator(); iterator.hasNext();)
        {
            CorefLink corefLink = (CorefLink)iterator.next();
            ret += corefLink.getEvalpoint();
        }

        return ret;
    }

    public int getResLinkCount()
    {
        int ret = 0;
        for(Iterator iterator = resTargetLinks.iterator(); iterator.hasNext();)
        {
            CorefLink corefLink = (CorefLink)iterator.next();
            ret += corefLink.getEvalpoint();
        }

        return ret;
    }

    protected String doctext;
    protected String pmid;
    protected ArrayList ggps;
    protected HashMap ggp2mentionMap;
    protected ArrayList goldMentions;
    protected ArrayList goldLinks;
    protected ArrayList resMentions;
    protected ArrayList resLinks;
    protected ArrayList goldTargetLinks;
    protected ArrayList resTargetLinks;
    protected ArrayList goldTargetMentions;
    protected ArrayList resTargetMentions;
    protected HashMap correctMentionMap;
    protected ArrayList spuriousMentions;
    protected ArrayList missingMentions;
    protected HashMap correctLinkMap;
    protected ArrayList spuriousLinks;
    protected ArrayList missingLinks;
    protected HashMap allMentionMap;
}
