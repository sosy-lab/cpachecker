package cpaplugin.cpa.common;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.exceptions.CPAException;

public class CompositeJoinOperator implements JoinOperator
{
    private List<JoinOperator> joinOperators;
    
    public CompositeJoinOperator (List<JoinOperator> joinOperators)
    {
        this.joinOperators = joinOperators;
    }
    
    public AbstractElement join (AbstractElement element1, AbstractElement element2) throws CPAException
    {      
        CompositeElement comp1 = (CompositeElement) element1;
        CompositeElement comp2 = (CompositeElement) element2;
        
        List<AbstractElement> comp1Elements = comp1.getElements ();
        List<AbstractElement> comp2Elements = comp2.getElements ();
        
        if (comp1Elements.size () != comp2Elements.size ())
            throw new CPAException ("Must join composite elements of the same size");
        if (comp1Elements.size () != joinOperators.size ())
            throw new CPAException ("Wrong number of join operators");
        
        List<AbstractElement> results = new ArrayList<AbstractElement> ();
        
        for (int idx = 0; idx < comp1Elements.size (); idx++)
        {
            JoinOperator joinOperator = joinOperators.get (idx);
            AbstractElement result = joinOperator.join (comp1Elements.get (idx), comp2Elements.get (idx));
            results.add (result);
        }
                
        // TODO do we ever use this function?
        return new CompositeElement (results, null);
    }

}
