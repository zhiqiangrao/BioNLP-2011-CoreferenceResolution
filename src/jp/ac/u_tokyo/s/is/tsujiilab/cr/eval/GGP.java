// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   GGP.java

package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval;


// Referenced classes of package jp.ac.u_tokyo.s.is.tsujiilab.cr.eval:
//            Annotation

public class GGP extends Annotation
{

    public GGP(int begin, int end, String id)
    {
        super(begin, end);
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    protected String id;
}
