package cpaplugin.cpa.cpas.location;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.TopElement;

public class LocationDomain implements AbstractDomain
{
    private static class LocationBottomElement implements BottomElement
    {
        
    }
    
    private static class LocationTopElement implements TopElement
    {
        
    }
    
    private static class LocationPartialOrder implements PartialOrder
    {
        public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
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
    private final static PartialOrder partialOrder = new LocationPartialOrder ();
    private final static JoinOperator joinOperator = new LocationJoinOperator ();
       
    public LocationDomain ()
    {

    }
    
    public BottomElement getBottomElement ()
    {
        return bottomElement;
    }
    
	public boolean isBottomElement(AbstractElement element) {
		// TODO Auto-generated method stub
		return false;
	}
    
    public TopElement getTopElement ()
    {
        return topElement;
    }

    public JoinOperator getJoinOperator ()
    {
        return joinOperator;
    }

    public PartialOrder getPartialOrder ()
    {
        return partialOrder;
    }
}
