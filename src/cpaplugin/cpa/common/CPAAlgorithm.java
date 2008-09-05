package cpaplugin.cpa.common;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

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
        Collection<AbstractElement> reached = createReachedSet(cpa);
     
        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, initialState,
                       " added as initial state to CPA");
        
        waitlist.addLast (initialState);
        reached.add(initialState);
        
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
            	    List<AbstractElement> toRemove =
            	        new Vector<AbstractElement>();
            	    List<AbstractElement> toAdd = new Vector<AbstractElement>();
            	    for (AbstractElement reachedElement : reached) {
            	        AbstractElement mergedElement = mergeOperator.merge(
            	                successor, reachedElement);
                        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
                                " Merged ", successor, " and ",
                                reachedElement, " --> ", mergedElement);
                        if (!mergedElement.equals(reachedElement)) {
                            LazyLogger.log(
                                    CustomLogLevel.CentralCPAAlgorithmLevel,
                                    "reached element ", reachedElement,
                                    " is removed from queue", 
                                    " and ", mergedElement,
                                    " is added to queue");
                            waitlist.remove(reachedElement);
                            waitlist.add(mergedElement);
                            
                            toRemove.add(reachedElement);
                            toAdd.add(mergedElement);
                        }
            	    }
            	    reached.removeAll(toRemove);
            	    reached.addAll(toAdd);
            	    
//            	    int numReached = reached.size (); // Need to iterate this way to avoid concurrent mod exceptions
//
//            	    for (int reachedIdx = 0; reachedIdx < numReached; reachedIdx++)
//            	    {
//            	        AbstractElement reachedElement = reached.pollFirst ();
//            	        AbstractElement mergedElement = mergeOperator.merge (successor, reachedElement);
//            	        reached.addLast (mergedElement);
//
//            	        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
//            	                " Merged ", successor, " and ",
//            	                reachedElement, " --> ", mergedElement);
//
//            	        if (!mergedElement.equals (reachedElement))
//            	        {
//            	            LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
//            	                    "reached element ", reachedElement,
//            	                    " is removed from queue", 
//            	                    " and ", mergedElement,
//            	            " is added to queue");
//            	            waitlist.remove (reachedElement);
//            	            waitlist.add (mergedElement);
//            	        }
//            	    }
            	}

                if (!stopOperator.stop (successor, reached))
                {
                    LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
                                   "No need to stop ", successor,
                                   " is added to queue");
                    waitlist.addLast (successor);
                    reached.add(successor);
                }
            }
            //CPACheckerStatistics.noOfReachedSet = reached.size();
        }
        
        return reached;
    }

    @SuppressWarnings("unchecked")
    private Collection<AbstractElement> createReachedSet(
            ConfigurableProblemAnalysis cpa) {
        // check whether the cpa provides a method for building a specialized
        // reached set. If not, just use a HashSet
        try {
            Method meth = cpa.getClass().getDeclaredMethod("newReachedSet");
            return (Collection<AbstractElement>)meth.invoke(cpa);
        } catch (Exception e) {
            // ignore, this is not an error
        }
        return new HashSet<AbstractElement>();
    }

    private void doRefinement(Collection<AbstractElement> reached,
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
