package cpa.common;

import java.util.List;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;

public class CompositeBottomElement implements BottomElement
{
    private final List<AbstractElement> bottoms;

    public CompositeBottomElement (List<AbstractElement> bottoms)
    {
        this.bottoms = bottoms;
    }

    @Override
    public boolean equals (Object o)
    {
        if (!(o instanceof CompositeBottomElement))
            return false;

        CompositeBottomElement otherComposite = (CompositeBottomElement) o;
        if (bottoms.size () != otherComposite.bottoms.size ())
            return false;

        for (int idx = 0; idx < bottoms.size (); idx++)
        {
            AbstractElement bottom1 = bottoms.get (idx);
            AbstractElement bottom2 = otherComposite.bottoms.get (idx);

            if (!bottom1.equals (bottom2))
                return false;
        }

        return true;
    }
    
    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }
}
