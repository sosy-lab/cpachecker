package cpaplugin.cpa.common;

import java.util.List;

import cpaplugin.cpa.common.interfaces.BottomElement;

public class CompositeBottomElement implements BottomElement
{
    private List<BottomElement> bottoms;
    
    public CompositeBottomElement (List<BottomElement> bottoms)
    {
        this.bottoms = bottoms;
    }
    
    public boolean equals (Object o)
    {
        if (!(o instanceof CompositeBottomElement))
            return false;
        
        CompositeBottomElement otherComposite = (CompositeBottomElement) o;
        if (bottoms.size () != otherComposite.bottoms.size ())
            return false;
        
        for (int idx = 0; idx < bottoms.size (); idx++)
        {
            BottomElement bottom1 = bottoms.get (idx);
            BottomElement bottom2 = otherComposite.bottoms.get (idx);
            
            if (!bottom1.equals (bottom2))
                return false;
        }
        
        return true;
    }
}
