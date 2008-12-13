package cpa.common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;

import cmdline.CPAMain;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;
import cpa.common.CompositeElement;
import exceptions.ErrorReachedException;
import exceptions.RefinementNeededException;
import exceptions.CPAException;

public class CPAAlgorithm
{
    private final int GC_PERIOD = 100;
    private int gcCounter = 0;

	public Collection<AbstractElementWithLocation> CPA (ConfigurableProgramAnalysis cpa, AbstractElementWithLocation initialState) throws CPAException
	{
		List<AbstractElementWithLocation> waitlist = new ArrayList<AbstractElementWithLocation> ();
		Collection<AbstractElementWithLocation> reached = createReachedSet(cpa);

		LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, initialState,
		" added as initial state to CPA");

		waitlist.add (initialState);
		reached.add(initialState);

		TransferRelation transferRelation = cpa.getTransferRelation ();
		MergeOperator mergeOperator = cpa.getMergeOperator ();
		StopOperator stopOperator = cpa.getStopOperator ();
		while (!waitlist.isEmpty ())
		{
			// AG - BFS or DFS, according to the configuration
		  AbstractElementWithLocation e = null;
//			if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
//			e = waitlist.removeFirst();
//			} else {
//			e = waitlist.removeLast();
//			}

			e = choose(waitlist);

			LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, e,
			" is popped from queue");
			List<AbstractElementWithLocation> successors = null;
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

			for (AbstractElementWithLocation successor : successors)
			{
				LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
						"successor of ", e, " --> ", successor);

				// AG as an optimization, we allow the mergeOperator to be null,
				// as a synonym of a trivial operator that never merges

				if (mergeOperator != null) {
					List<AbstractElementWithLocation> toRemove =
						new Vector<AbstractElementWithLocation>();
					List<AbstractElementWithLocation> toAdd = new Vector<AbstractElementWithLocation>();
					for (AbstractElementWithLocation reachedElement : reached) {
					  AbstractElementWithLocation mergedElement = mergeOperator.merge(
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

//					int numReached = reached.size (); // Need to iterate this way to avoid concurrent mod exceptions

//					for (int reachedIdx = 0; reachedIdx < numReached; reachedIdx++)
//					{
//					AbstractElement reachedElement = reached.pollFirst ();
//					AbstractElement mergedElement = mergeOperator.merge (successor, reachedElement);
//					reached.addLast (mergedElement);

//					LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
//					" Merged ", successor, " and ",
//					reachedElement, " --> ", mergedElement);

//					if (!mergedElement.equals (reachedElement))
//					{
//					LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
//					"reached element ", reachedElement,
//					" is removed from queue",
//					" and ", mergedElement,
//					" is added to queue");
//					waitlist.remove (reachedElement);
//					waitlist.add (mergedElement);
//					}
//					}
				}

				if (!stopOperator.stop (successor, reached))
				{
					LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
							"No need to stop ", successor,
					" is added to queue");
					// end to the end

					waitlist.add (successor);
					reached.add(successor);
				}
			}
			//CPACheckerStatistics.noOfReachedSet = reached.size();
		}

		return reached;
	}

	private AbstractElementWithLocation choose(List<AbstractElementWithLocation> waitlist) {

    if(waitlist.size() == 1 || CPAMain.cpaConfig.getBooleanValue("analysis.bfs")){
      return waitlist.remove(0);
    } else if(CPAMain.cpaConfig.getBooleanValue("analysis.topSort")) {
      AbstractElementWithLocation currentElement = waitlist.get(0);
      for(int i=1; i<waitlist.size(); i++){
        AbstractElementWithLocation currentTempElement = waitlist.get(i);
        if(currentTempElement.getLocationNode().getTopologicalSortId() >
            currentElement.getLocationNode().getTopologicalSortId()){
          currentElement = currentTempElement;
        }
      }

      waitlist.remove(currentElement);
      return currentElement;
    } else {
      return waitlist.remove(waitlist.size()-1);
    }
	}

	private Collection<AbstractElementWithLocation> createReachedSet(
			ConfigurableProgramAnalysis cpa) {
		// check whether the cpa provides a method for building a specialized
		// reached set. If not, just use a HashSet
		try {
		  Method meth = cpa.getClass().getDeclaredMethod("newReachedSet");
			
			return (Collection<AbstractElementWithLocation>)meth.invoke(cpa);
		} catch (NoSuchMethodException e) {
			// ignore, this is not an error
		  
		} catch (Exception lException) {
		  lException.printStackTrace();
		  
		  System.exit(1);
		}
		
		return new HashSet<AbstractElementWithLocation>();
	}

	private void doRefinement(Collection<AbstractElementWithLocation> reached,
			List<AbstractElementWithLocation> waitlist,
			Collection<AbstractElementWithLocation> reachableToUndo,
			Collection<AbstractElementWithLocation> toWaitlist) {
		LazyLogger.log(CustomLogLevel.SpecificCPALevel,
		"Performing refinement");
		// remove from reached all the elements in reachableToUndo
		Collection<AbstractElementWithLocation> newreached =
			new LinkedList<AbstractElementWithLocation>();
		for (AbstractElementWithLocation e : reached) {
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
		for (AbstractElementWithLocation e : toWaitlist) {
			LazyLogger.log(CustomLogLevel.SpecificCPALevel,
					"Adding element: ", e, " to waitlist");
			if (useBfs) {
				// end to the end
				waitlist.add(e);
			} else {
				// at to the first index
				waitlist.add(0, e);
			}
		}
		LazyLogger.log(CustomLogLevel.SpecificCPALevel,
				"Waitlist now is: ", waitlist);
		LazyLogger.log(CustomLogLevel.SpecificCPALevel,
		"Refinement done");

        if ((++gcCounter % GC_PERIOD) == 0) {
            System.gc();
            gcCounter = 0;
        }
	}
}
