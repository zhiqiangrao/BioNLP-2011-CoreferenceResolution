// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   CorefLink.java

package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval;

import java.util.ArrayList;

// Referenced classes of package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval:
//            Annotation, Mention

public class CorefLink extends Annotation
{

    public CorefLink(int begin, int end, String id, Mention anaphor, ArrayList antecedents)
    {
        super(begin, end);
        this.antecedents = new ArrayList();
        this.id = id;
        this.anaphor = anaphor;
        evalpoint = 1;
        this.antecedents = antecedents;
    }

    public CorefLink(int begin, int end, String id, Mention anaphor)
    {
        super(begin, end);
        antecedents = new ArrayList();
        this.id = id;
        this.anaphor = anaphor;
        evalpoint = 1;
    }

    public String getReferenceGGPs()
    {
        return referenceGGPs;
    }

    public void setReferenceGGPs(String referenceGGPs)
    {
        this.referenceGGPs = referenceGGPs;
    }

    public int getEvalpoint()
    {
        return evalpoint;
    }

    public void setEvalpoint(int evalpoint)
    {
        this.evalpoint = evalpoint;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public Mention getAnaphor()
    {
        return anaphor;
    }

    public void setAnaphor(Mention anaphor)
    {
        this.anaphor = anaphor;
    }

    public ArrayList getAntecedents()
    {
        return antecedents;
    }

    public void setAntecedent(ArrayList antecedents)
    {
        this.antecedents = antecedents;
    }

    public void addAntecedent(Mention antecedent)
    {
        antecedents.add(antecedent);
    }

    public Mention getAntecedent(int i)
    {
        if(i < antecedents.size())
            return (Mention)antecedents.get(i);
        else
            return null;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append((new StringBuilder(String.valueOf(id))).append("\t").toString());
        sb.append((new StringBuilder("Anaphor=")).append(anaphor.getId()).append("\t").toString());
        for(int i = 0; i < antecedents.size(); i++)
        {
            sb.append((new StringBuilder("Antecedent")).append(i + 1).append("=").toString());
            sb.append((new StringBuilder(String.valueOf(((Mention)antecedents.get(i)).getId()))).append("\t").toString());
        }

        sb.append((new StringBuilder(String.valueOf(evalpoint))).append(" point(s)\t").toString());
        sb.append("\r\n");
        return sb.toString();
    }

    protected String id;
    protected String referenceGGPs;
    protected Mention anaphor;
    protected ArrayList antecedents;
    protected int evalpoint;
}
