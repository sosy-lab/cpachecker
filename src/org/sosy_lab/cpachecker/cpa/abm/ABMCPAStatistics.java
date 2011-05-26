package org.sosy_lab.cpachecker.cpa.abm;

import java.io.PrintStream;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

/**
 * Prints some ABM related statistics
 * @author dwonisch
 *
 */
class ABMCPAStatistics implements Statistics {

    private final ABMCPA cpa;

    public ABMCPAStatistics(ABMCPA cpa) {
      this.cpa = cpa;
    }

    @Override
    public String getName() {
      return "ABMCPA";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      
      ABMTransferRelation transferRelation = cpa.getTransferRelation();
      TimedReducer reducer = cpa.getReducer();
      
      int sumCalls = transferRelation.cacheMisses + transferRelation.partialCacheHits + transferRelation.fullCacheHits;

      out.println("Maximum block depth:                                            " + transferRelation.maxRecursiveDepth);
      out.println("Total number of recursive CPA calls:                            " + sumCalls);
      out.println("  Number of cache misses:                                       " + transferRelation.cacheMisses + " (" + toPercent(transferRelation.cacheMisses, sumCalls) + " of all calls)");
      out.println("  Number of partial cache hits:                                 " + transferRelation.partialCacheHits + " (" + toPercent(transferRelation.partialCacheHits, sumCalls) + " of all calls)");
      out.println("  Number of full cache hits:                                    " + transferRelation.fullCacheHits + " (" + toPercent(transferRelation.fullCacheHits, sumCalls) + " of all calls)");
      out.println("Time for reducing abstract elements:                            " + reducer.reduceTime + " (Calls: " + reducer.reduceTime.getNumberOfIntervals() + ")");
      out.println("Time for expanding abstract elements:                           " + reducer.expandTime + " (Calls: " + reducer.expandTime.getNumberOfIntervals() + ")");
      out.println("Time for checking equality of abstract elements:                " + transferRelation.equalsTimer + " (Calls: " + transferRelation.equalsTimer.getNumberOfIntervals() + ")");
      out.println("Time for computing the hashCode of abstract elements:           " + transferRelation.hashingTimer + " (Calls: " + transferRelation.hashingTimer.getNumberOfIntervals() + ")");
      out.println("Time for searching ARTElements in ReachedSets:                  " + ARTElementSearcher.searchForARTElementTimer+ " (Calls: " + ARTElementSearcher.searchForARTElementTimer.getNumberOfIntervals() + ")");

      out.println("Time for removing cached subtrees for refinement:               " + transferRelation.removeCachedSubtreeTimer);
      out.println("Time for recomputing ARTs during counterexample analysis:       " + transferRelation.recomputeARTTimer);
    }
    
    
    private String toPercent(double val, double full) {
      return String.format("%1.0f", val/full*100) + "%";
    }
}