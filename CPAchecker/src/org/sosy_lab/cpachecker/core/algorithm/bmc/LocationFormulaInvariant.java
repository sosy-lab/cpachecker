/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;


public abstract class LocationFormulaInvariant implements CandidateInvariant {

  private final Set<CFANode> locations;

  public LocationFormulaInvariant(CFANode pLocation) {
    Preconditions.checkNotNull(pLocation);
    this.locations = Collections.singleton(pLocation);
  }

  public LocationFormulaInvariant(Set<CFANode> pLocations) {
    Preconditions.checkNotNull(pLocations);
    this.locations = ImmutableSet.copyOf(pLocations);
  }

  @Override
  public BooleanFormula getAssertion(Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException {
    Iterable<AbstractState> locationStates = AbstractStates.filterLocations(pReachedSet, locations);
    FluentIterable<BooleanFormula> assertions = FluentIterable.from(
        BMCHelper.assertAt(locationStates, getFormula(pFMGR, pPFMGR), pFMGR));
    return pFMGR.getBooleanFormulaManager().and(assertions.toList());
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Do nothing
  }

  @Override
  public void attemptInjection(InvariantGenerator pInvariantGenerator) throws UnrecognizedCodeException {
    // Do nothing
  }

  public static LocationFormulaInvariant makeBooleanInvariant(CFANode pLocation, boolean pValue) {
    return new LocationFormulaInvariant(pLocation) {

      @Override
      public BooleanFormula getFormula(FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException,
          InterruptedException {
        return pFMGR.getBooleanFormulaManager().makeBoolean(true);
      }
    };
  }

  public Set<CFANode> getLocations() {
    return locations;
  }

}
