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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public abstract class AbstractLocationFormulaInvariant implements LocationFormulaInvariant {

  private final Set<CFANode> locations;

  public AbstractLocationFormulaInvariant(CFANode pLocation) {
    Preconditions.checkNotNull(pLocation);
    this.locations = Collections.singleton(pLocation);
  }

  public AbstractLocationFormulaInvariant(Set<? extends CFANode> pLocations) {
    Preconditions.checkNotNull(pLocations);
    this.locations = ImmutableSet.copyOf(pLocations);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.algorithm.bmc.LocationInvariant#getLocations()
   */
  @Override
  public Set<CFANode> getLocations() {
    return locations;
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet,
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      int pDefaultIndex)
      throws CPATransferException, InterruptedException {
    Iterable<AbstractState> locationStates = AbstractStates.filterLocations(pReachedSet, locations);
    FluentIterable<BooleanFormula> assertions =
        FluentIterable.from(BMCHelper.assertAt(locationStates, this, pFMGR, pPFMGR, pDefaultIndex));
    return pFMGR.getBooleanFormulaManager().and(assertions.toList());
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Do nothing
  }

  public static LocationFormulaInvariant makeBooleanInvariant(CFANode pLocation, final boolean pValue) {
    return new AbstractLocationFormulaInvariant(pLocation) {

      @Override
      public BooleanFormula getFormula(
          FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
          throws CPATransferException, InterruptedException {
        return pFMGR.getBooleanFormulaManager().makeBoolean(pValue);
      }
    };
  }

  public static AbstractLocationFormulaInvariant makeLocationInvariant(
      final CFANode pLocation, final String pInvariant) {
    return new AbstractLocationFormulaInvariant(pLocation) {

      /**
       * Is the invariant known to be the boolean constant 'false'
       */
      private boolean isDefinitelyBooleanFalse = false;
      private final Map<FormulaManagerView, BooleanFormula> cachedFormulas = new HashMap<>();

      @Override
      public BooleanFormula getFormula(
          FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
          throws CPATransferException, InterruptedException {
        BooleanFormula formula;

        if (cachedFormulas.containsKey(pFMGR)) {
          formula = cachedFormulas.get(pFMGR);
        } else {
          formula = pFMGR.parse(pInvariant);
          cachedFormulas.put(pFMGR, formula);
        }

        if (!isDefinitelyBooleanFalse && pFMGR.getBooleanFormulaManager().isFalse(formula)) {
          isDefinitelyBooleanFalse = true;
        }
        return formula;
      }

      @Override
      public String toString() {
        return pInvariant;
      }

      @Override
      public void assumeTruth(ReachedSet pReachedSet) {
        if (isDefinitelyBooleanFalse) {
          Iterable<AbstractState> targetStates =
              ImmutableList.copyOf(AbstractStates.filterLocation(pReachedSet, pLocation));
          pReachedSet.removeAll(targetStates);
          for (ARGState s : from(targetStates).filter(ARGState.class)) {
            s.removeFromARG();
          }
        }
      }
    };
  }
}
