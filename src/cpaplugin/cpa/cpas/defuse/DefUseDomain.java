package cpaplugin.cpa.cpas.defuse;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.TopElement;

public class DefUseDomain implements AbstractDomain
{
    private static class DefUseBottomElement extends DefUseElement implements BottomElement
    {
        public DefUseBottomElement ()
        {
            super (null);
        }
    }
    
    private static class DefUseTopElement implements TopElement
    {
        
    }
    
    private static class DefUsePartialOrder implements PartialOrder
    {
        public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
        {
            DefUseElement defUseElement1 = (DefUseElement) element1;
            DefUseElement defUseElement2 = (DefUseElement) element2;
            
            int numDefs = defUseElement1.getNumDefinitions ();
            for (int idx = 0; idx < numDefs; idx++)
            {
                DefUseDefinition definition = defUseElement1.getDefinition (idx);
                if (!defUseElement2.containsDefinition (definition))
                    return false;
            }
            
            return true;
        }
    }
    
    private static class DefUseJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
            // Useless code, but helps to catch bugs by causing cast exceptions
            DefUseElement defUseElement1 = (DefUseElement) element1;
            DefUseElement defUseElement2 = (DefUseElement) element2;
            
            List<DefUseDefinition> joined = new ArrayList<DefUseDefinition> ();
            for (int idx = 0; idx < defUseElement1.getNumDefinitions (); idx++)
                joined.add (defUseElement1.getDefinition (idx));
            
            for (int idx = 0; idx < defUseElement2.getNumDefinitions (); idx++)            
            {
                DefUseDefinition def = defUseElement2.getDefinition (idx);
                if (!joined.contains (def))
                    joined.add (def);
            }
            
            return new DefUseElement (joined);
        }        
    }
    
    private final static BottomElement bottomElement = new DefUseBottomElement ();
    private final static TopElement topElement = new DefUseTopElement ();
    private final static PartialOrder partialOrder = new DefUsePartialOrder ();
    private final static JoinOperator joinOperator = new DefUseJoinOperator ();
       
    public DefUseDomain ()
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
