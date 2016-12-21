/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import java.util.Set;

import javax.annotation.Nullable;

public class CandidateInvariantConjunction implements CandidateInvariant {

  private final Iterable<? extends CandidateInvariant> elements;

  private CandidateInvariantConjunction(Iterable<? extends CandidateInvariant> pElements) {
    this.elements = Preconditions.checkNotNull(pElements);
  }

  @Override
  public BooleanFormula getFormula(FormulaManagerView pFMGR, PathFormulaManager pPFMGR,
      @Nullable PathFormula pContext) throws CPATransferException, InterruptedException {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BooleanFormula formula = bfmgr.makeTrue();
    for (CandidateInvariant element : elements) {
      formula = bfmgr.and(formula, element.getFormula(pFMGR, pPFMGR, pContext));
    }
    return formula;
  }

  @Override
  public BooleanFormula getAssertion(Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR, int pDefaultIndex)
      throws CPATransferException, InterruptedException {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BooleanFormula formula = bfmgr.makeTrue();
    for (CandidateInvariant element : elements) {
      formula = bfmgr.and(formula, element.getAssertion(pReachedSet, pFMGR, pPFMGR, pDefaultIndex));
    }
    return formula;
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    for (CandidateInvariant element : elements) {
      element.assumeTruth(pReachedSet);
    }
  }

  public Iterable<? extends CandidateInvariant> getElements() {
    return elements;
  }

  @Override
  public int hashCode() {
    return elements.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof CandidateInvariantConjunction) {
      return elements.equals(((CandidateInvariantConjunction) pObj).elements);
    }
    return false;
  }

  @Override
  public String toString() {
    return elements.toString();
  }

  public static CandidateInvariantConjunction of(Iterable<? extends CandidateInvariant> pElements) {
    if (areMatchingLocationInvariants(pElements)) {
      return new LocationCandidateInvariantConjunction(
          FluentIterable.from(pElements).filter(LocationFormulaInvariant.class));
    }
    return new CandidateInvariantConjunction(pElements);
  }

  private static boolean areMatchingLocationInvariants(
      Iterable<? extends CandidateInvariant> pElements) {
    FluentIterable<? extends CandidateInvariant> elements = FluentIterable.from(pElements);
    if (elements.isEmpty()) {
      return false;
    }
    return elements.allMatch(lfi -> lfi instanceof LocationFormulaInvariant);
  }

  private static class LocationCandidateInvariantConjunction extends CandidateInvariantConjunction
      implements LocationFormulaInvariant {

    private final FluentIterable<? extends LocationFormulaInvariant> elements;

    private LocationCandidateInvariantConjunction(
        FluentIterable<? extends LocationFormulaInvariant> pElements) {
      super(pElements);
      elements = pElements;
    }

    @Override
    public Set<CFANode> getLocations() {
      return elements.transformAndConcat(lfi -> lfi.getLocations()).toSet();
    }

    @Override
    public boolean equals(Object pObj) {
      // equals of superclass is fine here
      return super.equals(pObj);
    }

    @Override
    public int hashCode() {
      // hash code of superclass is fine here
      return super.hashCode();
    }

  }

}
