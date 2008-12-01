package cpa.common;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.Set;

import logging.CustomLogLevel;
import logging.LazyLogger;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.common.CPATransferException;
import exceptions.CPAException;

import cpa.common.worklist.FIFOWorklist;
import cpa.common.worklist.LIFOWorklist;
import cpa.common.worklist.SortedWorklist;
import cpa.common.worklist.Worklist;

public class CPAAlgorithm {
	
	public Collection<AbstractElement> bfsCPA(ConfigurableProgramAnalysis cpa, AbstractElement initialElement) throws CPAException {
		Worklist<AbstractElement> worklist = new FIFOWorklist<AbstractElement>(initialElement);
		Set<AbstractElement> reachedSet = new HashSet<AbstractElement>();
		reachedSet.add(initialElement);
		
		return CPA(cpa, worklist, reachedSet);
	}
	
	public Collection<AbstractElement> dfsCPA(ConfigurableProgramAnalysis cpa, AbstractElement initialElement) throws CPAException {
		Worklist<AbstractElement> worklist = new LIFOWorklist<AbstractElement>(initialElement);
		Set<AbstractElement> reachedSet = new HashSet<AbstractElement>();
		reachedSet.add(initialElement);
		
		return CPA(cpa, worklist, reachedSet);
	}
	
	public Collection<AbstractElement> topologicalCPA(ConfigurableProgramAnalysis cpa, AbstractElement initialElement) throws CPAException {
		// AH: as it is a hack this comparator does not deserve an own file
		Comparator<AbstractElement> topologicalComparator = 
			new Comparator<AbstractElement>() {
			public int compare(AbstractElement e1, AbstractElement e2) {
				AbstractElementWithLocation element1 = (AbstractElementWithLocation)e1;
				AbstractElementWithLocation element2 = (AbstractElementWithLocation)e2;
				
				return (element1.getLocationNode().getTopologicalSortId() - element2.getLocationNode().getTopologicalSortId());
			}
		};
		
		Worklist<AbstractElement> worklist = new SortedWorklist<AbstractElement>(topologicalComparator, initialElement);
		Set<AbstractElement> reachedSet = new HashSet<AbstractElement>();
		reachedSet.add(initialElement);
		
		return CPA(cpa, worklist, reachedSet);
	}
	
	public Collection<AbstractElement> CPA(ConfigurableProgramAnalysis cpa, Worklist<AbstractElement> waitlist, Set<AbstractElement> reached) throws CPAException {
    	TransferRelation transferRelation = cpa.getTransferRelation();
		MergeOperator mergeOperator = cpa.getMergeOperator();
		StopOperator stopOperator = cpa.getStopOperator();

		while (waitlist.hasMoreElements()) {
			AbstractElement e = waitlist.nextElement();
			
			LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, e, " is popped from queue");
			
			List<AbstractElement> successors = null;
			
			// AH:
			// TODO: There was stuff about refinement and reaching error states
			// put them again here?
			try {
				successors = transferRelation.getAllAbstractSuccessors(e);
			} catch (CPATransferException e1) {
				e1.printStackTrace();
				assert(false); // should not happen
			}

			for (AbstractElement successor : successors) {
				LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, "successor of ", e, " --> ", successor);

				// AG as an optimization, we allow the mergeOperator to be null,
				// as a synonym of a trivial operator that never merges

				if (mergeOperator != null) {
					List<AbstractElement> toRemove = new Vector<AbstractElement>();
					List<AbstractElement> toAdd = new Vector<AbstractElement>();
					
					for (AbstractElement reachedElement : reached) {
						AbstractElement mergedElement = mergeOperator.merge(successor, reachedElement);
						LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, " Merged ", successor, " and ", reachedElement, " --> ", mergedElement);
						
						if (!mergedElement.equals(reachedElement)) {
							LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, "reached element ", reachedElement, " is removed from queue", " and ", mergedElement, " is added to queue");
							
							waitlist.replace(reachedElement, mergedElement);
							
							toRemove.add(reachedElement);
							toAdd.add(mergedElement);
						}
					}
					
					reached.removeAll(toRemove);
					reached.addAll(toAdd);
				}

				if (!stopOperator.stop(successor, reached)) {
					LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, "No need to stop ", successor, " is added to queue");
					// end to the end

					waitlist.add(successor);
					reached.add(successor);
				}
			}
		}

		return reached;
	}
}
