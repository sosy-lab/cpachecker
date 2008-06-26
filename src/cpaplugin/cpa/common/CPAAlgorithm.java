package cpaplugin.cpa.common;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

public class CPAAlgorithm
{
    public Collection<AbstractElement> CPA (ConfigurableProblemAnalysis cpa, AbstractElement initialState) throws CPAException
    {
        Deque<AbstractElement> waitlist = new ArrayDeque<AbstractElement> ();
        Deque<AbstractElement> reached = new ArrayDeque<AbstractElement> ();
        
        waitlist.addLast (initialState);
        reached.addLast (initialState);
        
        TransferRelation transferRelation = cpa.getTransferRelation ();
        MergeOperator mergeOperator = cpa.getMergeOperator ();
        StopOperator stopOperator = cpa.getStopOperator ();
        
        while (!waitlist.isEmpty ())
        {
            AbstractElement e = waitlist.pollFirst ();
            List<AbstractElement> successors = transferRelation.getAllAbstractSuccessors (e);
            
            for (AbstractElement successor : successors)
            {
                int numReached = reached.size (); // Need to iterate this way to avoid concurrent mod exceptions
                
                for (int reachedIdx = 0; reachedIdx < numReached; reachedIdx++)
                {
                    AbstractElement reachedElement = reached.pollFirst ();                   
                    AbstractElement mergedElement = mergeOperator.merge (successor, reachedElement);
                    reached.addLast (mergedElement);
                    
                    if (!mergedElement.equals (reachedElement))
                    {
                        waitlist.remove (reachedElement);
                        waitlist.add (mergedElement);
                    }
                }

                if (!stopOperator.stop (successor, reached))
                {
                    waitlist.addLast (successor);
                    reached.addLast (successor);
                }
            }
        }
        
        return reached;
    }
}
