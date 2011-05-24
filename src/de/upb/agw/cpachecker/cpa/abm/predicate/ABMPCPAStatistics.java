package de.upb.agw.cpachecker.cpa.abm.predicate;

import java.io.PrintStream;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import de.upb.agw.cpachecker.cpa.abm.util.ARTElementSearcher;
import de.upb.agw.cpachecker.cpa.abm.util.PredicateReducer;

/**
 * Prints some ABM related statistics
 * @author dwonisch
 *
 */
class ABMPCPAStatistics implements Statistics {

    private final ABMPredicateCPA cpa;
    private ABMPRefiner refiner = null;

    public ABMPCPAStatistics(ABMPredicateCPA cpa) throws InvalidConfigurationException {
      this.cpa = cpa;
    }
    
    void addRefiner(ABMPRefiner ref) {
      refiner = ref;
    }

    @Override
    public String getName() {
      return "ABMPredicateCPA";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      
      ABMPTransferRelation transferRelation = cpa.getTransferRelation();
      int sumCalls = transferRelation.cacheMisses + transferRelation.partialCacheHits + transferRelation.fullCacheHits;
      out.println("Number of cache misses:                                             " + transferRelation.cacheMisses + " (" + toPercent(transferRelation.cacheMisses, sumCalls) + " of all calls)");
      out.println("Number of partial cache hits:                                       " + transferRelation.partialCacheHits + " (" + toPercent(transferRelation.partialCacheHits, sumCalls) + " of all calls)");
      out.println("Number of full cache hits:                                          " + transferRelation.fullCacheHits + " (" + toPercent(transferRelation.fullCacheHits, sumCalls) + " of all calls)");
     // out.println("Time for determining relevant predicates:                       " + PredicateReducer.isRelevantTimer+ " (Calls: " + PredicateReducer.isRelevantTimer.getNumberOfIntervals() + ")");
      out.println("Time for reducing the state space of PredicateAbstractElements: " + PredicateReducer.reduceTimer+ " (Calls: " + PredicateReducer.reduceTimer.getNumberOfIntervals() + ")");
      out.println("Time for expanding state space of PredicateAbstractElements:    " + PredicateReducer.expandTimer+ " (Calls: " + PredicateReducer.expandTimer.getNumberOfIntervals() + ")");
      out.println("Time for computing returning elements of cached subgraphs:      " + transferRelation.returnElementsSearchTimer+ " (Calls: " + transferRelation.returnElementsSearchTimer.getNumberOfIntervals() + ")");
      out.println("Time for checking equality using SubgraphEntryHasher:           " + transferRelation.equalsTimer + " (Calls: " + transferRelation.equalsTimer.getNumberOfIntervals() + ")");
      out.println("Time for computing the hashCode using SubgraphEntryHashers:     " + transferRelation.hashingTimer + " (Calls: " + transferRelation.hashingTimer.getNumberOfIntervals() + ")");
      out.println("Time for searching ARTElements in ReachedSets:                  " + ARTElementSearcher.searchForARTElementTimer+ " (Calls: " + ARTElementSearcher.searchForARTElementTimer.getNumberOfIntervals() + ")");
      
      if(refiner != null) {
        out.println("Time for constructing subtrees to error locations:              " + refiner.computeSubtreeTimer);
        out.println("Time for SSA renaming subtree:                                  " + refiner.ssaRenamingTimer);
        out.println("Time for constructing actual paths to error locations:          " + refiner.computeCounterexampleTimer);
        out.println("Time for removing subtrees for refinement:                      " + refiner.removeSubtreeTimer);
        out.println("  Time for removing cached subtrees for refinement:             " + transferRelation.removeCachedSubtreeTimer);
        out.println("  Time for recomputing ARTs while counterexample analysis:      " + transferRelation.recomputeARTTimer);
      }
    }
    
    
    private String toPercent(double val, double full) {
      return String.format("%1.0f", val/full*100) + "%";
    }
}