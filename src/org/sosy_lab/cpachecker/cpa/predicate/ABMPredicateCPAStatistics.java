package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkState;

import java.io.PrintStream;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.ABMPredicateRefiner.ExtendedPredicateRefiner;

public class ABMPredicateCPAStatistics extends PredicateCPAStatistics {

  private ExtendedPredicateRefiner refiner = null;
  
  public ABMPredicateCPAStatistics(ABMPredicateCPA pCpa) throws InvalidConfigurationException {
    super(pCpa);
  }

  @Override
  void addRefiner(PredicateRefiner pRef) {
    checkState(refiner == null);
    if (pRef instanceof ExtendedPredicateRefiner) {
      refiner = (ExtendedPredicateRefiner)pRef;  
    }
    super.addRefiner(pRef);
  }
  
  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    super.printStatistics(out, pResult, pReached);
    
    if (refiner != null) {
      out.println("SSA renaming:                        " + refiner.ssaRenamingTimer);
    }
  }
}
