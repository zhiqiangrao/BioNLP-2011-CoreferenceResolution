// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Annotation.java

package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval;


public class Annotation
{

    public Annotation(int begin, int end)
    {
        this.begin = begin;
        this.end = end;
    }

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int begin)
    {
        this.begin = begin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int end)
    {
        this.end = end;
    }

    protected int begin;
    protected int end;
}
