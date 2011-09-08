/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
  void addRefiner(AbstractInterpolationBasedRefiner pRef) {
    checkState(refiner == null);
    if (pRef instanceof ExtendedPredicateRefiner) {
      refiner = (ExtendedPredicateRefiner)pRef;
    }
    super.addRefiner(pRef);
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    super.printStatistics(out, pResult, pReached);

    out.println();
    out.println("Reduce elements:            " + ABMPredicateReducer.reduceTimer);
    out.println("Expand elements:            " + ABMPredicateReducer.expandTimer);
    out.println("Extract predicates:         " + ABMPredicateReducer.extractTimer);

    if (refiner != null) {
      out.println("SSA renaming:               " + refiner.ssaRenamingTimer);
    }
  }
}
