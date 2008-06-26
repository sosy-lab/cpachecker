package cpaplugin.cpa.domains.location;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.TopElement;

public class LocationDomain implements AbstractDomain
{
    private static class LocationBottomElement implements BottomElement
    {
        
    }
    
    private static class LocationTopElement implements TopElement
    {
        
    }
    
    private static class LocationPreOrder implements PreOrder
    {
        public boolean satisfiesPreOrder (AbstractElement element1, AbstractElement element2)
        {
            if (element1.equals (element2))
                return true;
            
            if (element1 == bottomElement || element2 == topElement)
                return true;
            
            return false;
        }
    }
    
    private static class LocationJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
            // Useless code, but helps to catch bugs by causing cast exceptions
            LocationElement locElement1 = (LocationElement) element1;
            LocationElement locElement2 = (LocationElement) element2;
            
            if (locElement1.equals (locElement2))
                return locElement1;
            
            if (locElement1 == bottomElement)
                return locElement2;
            if (locElement2 == bottomElement)
                return locElement1;
                        
            return topElement;
        }        
    }
    
    private final static BottomElement bottomElement = new LocationBottomElement ();
    private final static TopElement topElement = new LocationTopElement ();
    private final static PreOrder preOrder = new LocationPreOrder ();
    private final static JoinOperator joinOperator = new LocationJoinOperator ();
       
    public LocationDomain ()
    {

    }
    
    public BottomElement getBottomElement ()
    {
        return bottomElement;
    }
    
    public TopElement getTopElement ()
    {
        return topElement;
    }

    public JoinOperator getJoinOperator ()
    {
        return joinOperator;
    }

    public PreOrder getPreOrder ()
    {
        return preOrder;
    }
}
