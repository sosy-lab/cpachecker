package cpaplugin.cpa.common;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;

public class CPAAlgorithm
{
    public Collection<AbstractElement> CPA (ConfigurableProblemAnalysis cpa, AbstractElement initialState) throws CPAException
    {
        Deque<AbstractElement> waitlist = new ArrayDeque<AbstractElement> ();
        Deque<AbstractElement> reached = new ArrayDeque<AbstractElement> ();
     
        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, initialState,
                       " added as initial state to CPA");
        
        waitlist.addLast (initialState);
        reached.addLast (initialState);
        
        TransferRelation transferRelation = cpa.getTransferRelation ();
        MergeOperator mergeOperator = cpa.getMergeOperator ();
        StopOperator stopOperator = cpa.getStopOperator ();
        while (!waitlist.isEmpty ())
        {
            // AG - BFS or DFS, according to the configuration
            AbstractElement e = null;
            if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
                e = waitlist.removeFirst();
            } else {
                e = waitlist.removeLast();
            }
            LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, e,
                           " is popped from queue");
            List<AbstractElement> successors = null;
            try {
                successors = transferRelation.getAllAbstractSuccessors (e);
            } catch (ErrorReachedException err) {
                System.out.println("Reached error state! Message is:");
                System.out.println(err.toString());
                return reached;
            } catch (RefinementNeededException re) {
                doRefinement(reached, waitlist, re.getReachableToUndo(),
                             re.getToWaitlist());
                continue;
            } catch (CPATransferException e1) {
                e1.printStackTrace();
                assert(false); // should not happen
            }
            
            for (AbstractElement successor : successors)
            {
            	LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
                               "successor of ", e, " --> ", successor);
            	
            	// AG as an optimization, we allow the mergeOperator to be null,
            	// as a synonym of a trivial operator that never merges 
            	if (mergeOperator != null) {
            	    int numReached = reached.size (); // Need to iterate this way to avoid concurrent mod exceptions

            	    for (int reachedIdx = 0; reachedIdx < numReached; reachedIdx++)
            	    {
            	        AbstractElement reachedElement = reached.pollFirst ();
            	        AbstractElement mergedElement = mergeOperator.merge (successor, reachedElement);
            	        reached.addLast (mergedElement);

            	        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
            	                " Merged ", successor, " and ",
            	                reachedElement, " --> ", mergedElement);

            	        if (!mergedElement.equals (reachedElement))
            	        {
            	            LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
            	                    "reached element ", reachedElement,
            	                    " is removed from queue", 
            	                    " and ", mergedElement,
            	            " is added to queue");
            	            waitlist.remove (reachedElement);
            	            waitlist.add (mergedElement);
            	        }
            	    }
            	}

                if (!stopOperator.stop (successor, reached))
                {
                    LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
                                   "No need to stop ", successor,
                                   " is added to queue");
                    waitlist.addLast (successor);
                    reached.addLast (successor);
                }
            }
            //CPACheckerStatistics.noOfReachedSet = reached.size();
        }
        
        return reached;
    }

    private void doRefinement(Deque<AbstractElement> reached,
            Deque<AbstractElement> waitlist,
            Collection<AbstractElement> reachableToUndo,
            Collection<AbstractElement> toWaitlist) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                       "Performing refinement");        
        // remove from reached all the elements in reachableToUndo
        Collection<AbstractElement> newreached =
            new LinkedList<AbstractElement>();
        for (AbstractElement e : reached) {
            if (!reachableToUndo.contains(e)) {
                newreached.add(e);
            } else {
                LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                               "Removing element: ", e, " from reached");
                if (waitlist.remove(e)) {
                    LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                                   "Removing element: ", e,
                                   " also from waitlist");                    
                }
            }
        }
        reached.clear();
        reached.addAll(newreached);
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                       "Reached now is: ", newreached);
        // and add to the wait list all the elements in toWaitlist
        boolean useBfs = CPAMain.cpaConfig.getBooleanValue("analysis.bfs");
        for (AbstractElement e : toWaitlist) {
            LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                           "Adding element: ", e, " to waitlist");
            if (useBfs) {
                waitlist.addLast(e);
            } else {
                waitlist.addFirst(e);
            }
        }
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                       "Waitlist now is: ", waitlist);
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       "Refinement done");
        
        System.gc();
    }
}
