    package cpa.common;

import java.util.List;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.TopElement;

public class CompositeTopElement implements TopElement
{
    private final List<AbstractElement> tops;

    public CompositeTopElement (List<AbstractElement> tops)
    {
        this.tops = tops;
    }

    @Override
    public boolean equals (Object o)
    {
        if (!(o instanceof CompositeBottomElement))
            return false;

        CompositeTopElement otherComposite = (CompositeTopElement) o;
        if (tops.size () != otherComposite.tops.size ())
            return false;

        for (int idx = 0; idx < tops.size (); idx++)
        {
            AbstractElement top1 = tops.get (idx);
            AbstractElement top2 = otherComposite.tops.get (idx);

            if (!top1.equals (top2))
                return false;
        }

        return true;
    }
}
