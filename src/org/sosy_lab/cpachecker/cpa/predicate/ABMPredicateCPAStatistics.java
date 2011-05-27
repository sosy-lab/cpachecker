package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkState;

import java.io.PrintStream;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class ABMPredicateCPAStatistics extends PredicateCPAStatistics {

  private ABMPredicateRefiner refiner = null;
  
  public ABMPredicateCPAStatistics(ABMPredicateCPA pCpa) throws InvalidConfigurationException {
    super(pCpa);
  }

  void addRefiner(ABMPredicateRefiner pRef) {
    checkState(refiner == null);
    refiner = pRef;
  }
  
  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    super.printStatistics(out, pResult, pReached);
    
    if (refiner != null) {
      out.println("SSA renaming:                        " + refiner.ssaRenamingTimer);
    }
  }
}
