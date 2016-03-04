// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Mention.java

package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval;

import java.util.ArrayList;

// Referenced classes of package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval:
//            Annotation

public class Mention extends Annotation
{

    public Mention(int begin, int end, String id, String min)
    {
        super(begin, end);
        ggps = new ArrayList();
        minBegin = begin;
        minEnd = end;
        this.id = id;
        this.min = min;
        evalpoint = 0;
        type = "OTHERS";
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setMinBegin(int minBegin)
    {
        this.minBegin = minBegin;
    }

    public void setMinEnd(int minEnd)
    {
        this.minEnd = minEnd;
    }

    public ArrayList getGgps()
    {
        return ggps;
    }

    public void setGgps(ArrayList ggps)
    {
        this.ggps.addAll(ggps);
    }

    public int getMinBegin()
    {
        return minBegin;
    }

    public int getMinEnd()
    {
        return minEnd;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getMin()
    {
        return min;
    }

    public void setMin(String min)
    {
        this.min = min;
    }

    public int getEvalpoint()
    {
        return evalpoint;
    }

    public void setEvalpoint(int evalpoint)
    {
        this.evalpoint = evalpoint;
    }

    public String getStr()
    {
        return str;
    }

    public void setStr(String str)
    {
        this.str = str;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append((new StringBuilder(String.valueOf(id))).append("\t").toString());
        sb.append((new StringBuilder(String.valueOf(begin))).append("\t").toString());
        sb.append((new StringBuilder(String.valueOf(end))).append("\t").toString());
        sb.append((new StringBuilder(String.valueOf(minBegin))).append("\t").toString());
        sb.append((new StringBuilder(String.valueOf(minEnd))).append("\t").toString());
        sb.append((new StringBuilder(String.valueOf(min))).append("\t").toString());
        sb.append((new StringBuilder(String.valueOf(type))).append("\t").toString());
        return sb.toString();
    }

    protected String str;
    protected String id;
    protected String min;
    protected int evalpoint;
    protected int minBegin;
    protected int minEnd;
    protected String type;
    protected ArrayList ggps;
}
