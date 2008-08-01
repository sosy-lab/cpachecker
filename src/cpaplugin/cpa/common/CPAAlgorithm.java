package cpaplugin.cpa.common;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import cpaplugin.CPACheckerStatistics;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class CPAAlgorithm
{
    public Collection<AbstractElement> CPA (ConfigurableProblemAnalysis cpa, AbstractElement initialState) throws CPAException
    {
        Deque<AbstractElement> waitlist = new ArrayDeque<AbstractElement> ();
        Deque<AbstractElement> reached = new ArrayDeque<AbstractElement> ();
     
        CPACheckerLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, initialState + " added as initial state to CPA");
        
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
            CPACheckerLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, e + " is popped from queue");
            List<AbstractElement> successors = null;
            try {
                successors = transferRelation.getAllAbstractSuccessors (e);
            } catch (ErrorReachedExeption err) {
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
            	CPACheckerLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, "successor of " + e + " --> " + successor);
                int numReached = reached.size (); // Need to iterate this way to avoid concurrent mod exceptions
                
                for (int reachedIdx = 0; reachedIdx < numReached; reachedIdx++)
                {
                    AbstractElement reachedElement = reached.pollFirst ();
                    AbstractElement mergedElement = mergeOperator.merge (successor, reachedElement);
                    reached.addLast (mergedElement);
                    
                    CPACheckerLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, " Merged " + successor
                    		+ " and " + reachedElement + " --> "+ mergedElement);
                    
                    if (!mergedElement.equals (reachedElement))
                    {
                    	CPACheckerLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, "reached element " + reachedElement + " is removed from queue" + 
                    			" and " + mergedElement + " is added to queue");
                        waitlist.remove (reachedElement);
                        waitlist.add (mergedElement);
                    }
                }

                if (!stopOperator.stop (successor, reached))
                {
                    CPACheckerLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, "No need to stop " + successor + " is added to queue");
                    waitlist.addLast (successor);
                    reached.addLast (successor);
                }
            }
            CPACheckerStatistics.noOfReachedSet = reached.size();
        }
        
        return reached;
    }

    private void doRefinement(Deque<AbstractElement> reached,
            Deque<AbstractElement> waitlist,
            Collection<AbstractElement> reachableToUndo,
            Collection<AbstractElement> toWaitlist) {
        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                             "Performing refinement");        
        // remove from reached all the elements in reachableToUndo
        Collection<AbstractElement> newreached =
            new LinkedList<AbstractElement>();
        for (AbstractElement e : reached) {
            if (!reachableToUndo.contains(e)) {
                newreached.add(e);
            } else {
                CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                                    "Removing element; " + e + " from reached");
                if (waitlist.remove(e)) {
                    CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                            "Removing element; " + e + " also from waitlist");                    
                }
            }
        }
        reached.clear();
        reached.addAll(newreached);
        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
                "Reached now is: " + newreached.toString());
        // and add to the wait list all the elements in toWaitlist
        for (AbstractElement e : toWaitlist) {
            CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                                 "Adding element; " + e + " to waitlist");
            waitlist.addLast(e);
        }
        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                             "Refinement done");
    }
}
