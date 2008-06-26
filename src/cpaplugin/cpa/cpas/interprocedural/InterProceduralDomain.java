package cpaplugin.cpa.cpas.interprocedural;


import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PreOrder;
import cpaplugin.cpa.common.interfaces.TopElement;

public class InterProceduralDomain implements AbstractDomain
{
    private static class InterProceduralBottomElement implements BottomElement
    {
        
    }
    
    private static class InterProceduralTopElement implements TopElement
    {
        
    }
    
    private static class InterProceduralPreOrder implements PreOrder
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
    
    private static class InterProceduralJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
            // Useless code, but helps to catch bugs by causing cast exceptions
        	return topElement;
        }
    }
    
    private final static BottomElement bottomElement = new InterProceduralBottomElement ();
    private final static TopElement topElement = new InterProceduralTopElement ();
    private final static PreOrder preOrder = new InterProceduralPreOrder ();
    private final static JoinOperator joinOperator = new InterProceduralJoinOperator ();
       
    public InterProceduralDomain ()
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
