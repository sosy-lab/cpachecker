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

	public Collection<AbstractElement> CPA (ConfigurableProgramAnalysis cpa, AbstractElement initialState) throws CPAException
	{
		List<AbstractElement> waitlist = new ArrayList<AbstractElement> ();
		Collection<AbstractElement> reached = createReachedSet(cpa);

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
			AbstractElement e = null;
//			if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
//			e = waitlist.removeFirst();
//			} else {
//			e = waitlist.removeLast();
//			}

			e = choose(waitlist);

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

	private AbstractElement choose(List<AbstractElement> waitlist) {

		AbstractElement e;

		if(waitlist.size() == 1){
			e = waitlist.remove(0);
		}
		else{
			if(CPAMain.cpaConfig.getBooleanValue("analysis.topSort")){
				AbstractElement currentElement = waitlist.get(0);
				CompositeElement compElem = (CompositeElement)currentElement;
				AbstractElement firstElement = compElem.get(0);
				// TODO we require the first element to contain the location information
				if(!(firstElement instanceof AbstractElementWithLocation)){
					try {
						throw new CPAException("No Location information available, impossible to continue");
					} catch (CPAException e1) {
						e1.printStackTrace();
					}
				}

				AbstractElementWithLocation tempElem = (AbstractElementWithLocation)firstElement;
				for(int i=1; i<waitlist.size(); i++){
					AbstractElement currentTempElement = waitlist.get(i);
					CompositeElement compTempElem = (CompositeElement)currentTempElement;
					AbstractElement firstTempElement = compTempElem.get(0);
					AbstractElementWithLocation tempElem2 = (AbstractElementWithLocation)firstTempElement;
					if(tempElem2.getLocationNode().getTopologicalSortId() > tempElem.getLocationNode().getTopologicalSortId()){
						currentElement = currentTempElement;
						tempElem = tempElem2;
					}
				}

				e = currentElement;

				waitlist.remove(e);
			}

			else{
				if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
					e = waitlist.remove(0);
				} else {
					e = waitlist.remove(waitlist.size()-1);
				}
			}
		}
		return e;
	}

	@SuppressWarnings("unchecked")
	private Collection<AbstractElement> createReachedSet(
			ConfigurableProgramAnalysis cpa) {
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
			List<AbstractElement> waitlist,
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
