package cpaplugin.cpa.common;

import java.util.List;

import cpaplugin.cpa.common.interfaces.AbstractElement;

public class CompositeElement implements AbstractElement
{
    private List<AbstractElement> elements;
    
    public CompositeElement (List<AbstractElement> elements)
    {
        this.elements = elements;
    }
    
    public List<AbstractElement> getElements ()
    {
        return elements;
    }
    
    public int getNumberofElements(){
    	return elements.size();
    }
    
    public boolean equals (Object other)
    {
        if (other == this)
            return true;
        
        if (!(other instanceof CompositeElement))
            return false;
        
        CompositeElement otherComposite = (CompositeElement) other;
        List<AbstractElement> otherElements = otherComposite.elements;
        
        if (otherElements.size () != this.elements.size ())
            return false;
        
        for (int idx = 0; idx < elements.size (); idx++)
        {
            AbstractElement element1 = otherElements.get (idx);
            AbstractElement element2 = this.elements.get (idx);
            
            if (!element1.equals (element2))
                return false;
        }
        
        return true;
    }
    
    public String toString ()
    {
        StringBuilder builder = new StringBuilder ();
        builder.append ('(');
        for (AbstractElement element : elements)
            builder.append (element.toString ()).append (',');
        builder.replace (builder.length () - 1, builder.length (), ")");
        
        return builder.toString ();
    }
}
