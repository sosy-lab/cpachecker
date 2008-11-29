    package cpaplugin.cpa.common;

import java.util.List;

import cpaplugin.cpa.common.interfaces.TopElement;

public class CompositeTopElement implements TopElement
{
    private List<TopElement> tops;
    
    public CompositeTopElement (List<TopElement> tops)
    {
        this.tops = tops;
    }
    
    public boolean equals (Object o)
    {
        if (!(o instanceof CompositeBottomElement))
            return false;
        
        CompositeTopElement otherComposite = (CompositeTopElement) o;
        if (tops.size () != otherComposite.tops.size ())
            return false;
        
        for (int idx = 0; idx < tops.size (); idx++)
        {
            TopElement top1 = tops.get (idx);
            TopElement top2 = otherComposite.tops.get (idx);
            
            if (!top1.equals (top2))
                return false;
        }
        
        return true;
    }
}
