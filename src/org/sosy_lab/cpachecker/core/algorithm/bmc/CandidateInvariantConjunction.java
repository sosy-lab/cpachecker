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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class CandidateInvariantConjunction {

  private static interface Conjunction extends CandidateInvariant {

    Set<CandidateInvariant> getOperands();

  }

  private static class GenericConjunction implements Conjunction {

    private final Set<CandidateInvariant> operands;

    private GenericConjunction(ImmutableSet<? extends CandidateInvariant> pOperands) {
      Preconditions.checkArgument(
          pOperands.size() > 1,
          "It makes no sense to use a CandidateInvariantConjunction unless there are at least two operands.");
      this.operands = ImmutableSet.copyOf(pOperands);
    }

    @Override
    public BooleanFormula getFormula(
        FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
        throws CPATransferException, InterruptedException {
      BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
      BooleanFormula formula = bfmgr.makeTrue();
      for (CandidateInvariant operand : operands) {
        formula = bfmgr.and(formula, operand.getFormula(pFMGR, pPFMGR, pContext));
      }
      return formula;
    }

    @Override
    public BooleanFormula getAssertion(
        Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
        throws CPATransferException, InterruptedException {
      BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
      BooleanFormula formula = bfmgr.makeTrue();
      for (CandidateInvariant operand : operands) {
        formula = bfmgr.and(formula, operand.getAssertion(pReachedSet, pFMGR, pPFMGR));
      }
      return formula;
    }

    @Override
    public void assumeTruth(ReachedSet pReachedSet) {
      for (CandidateInvariant element : operands) {
        element.assumeTruth(pReachedSet);
      }
    }

    @Override
    public int hashCode() {
      return operands.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof GenericConjunction) {
        return operands.equals(((GenericConjunction) pObj).operands);
      }
      return false;
    }

    @Override
    public String toString() {
      return operands.toString();
    }

    @Override
    public boolean appliesTo(CFANode pLocation) {
      return Iterables.any(operands, e -> e.appliesTo(pLocation));
    }

    @Override
    public Set<CandidateInvariant> getOperands() {
      return operands;
    }
  }

  private static class SingleLocationConjunction extends SingleLocationFormulaInvariant
      implements Conjunction {

    private final Conjunction delegate;

    private SingleLocationConjunction(Iterable<SingleLocationFormulaInvariant> pOperands) {
      super(getSingleLocation(pOperands));
      delegate = new GenericConjunction(ImmutableSet.copyOf(pOperands));
    }

    @Override
    public BooleanFormula getFormula(
        FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
        throws CPATransferException, InterruptedException {
      return delegate.getFormula(pFMGR, pPFMGR, pContext);
    }

    @Override
    public BooleanFormula getAssertion(
        Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
        throws CPATransferException, InterruptedException {
      return delegate.getAssertion(pReachedSet, pFMGR, pPFMGR);
    }

    @Override
    public void assumeTruth(ReachedSet pReachedSet) {
      delegate.assumeTruth(pReachedSet);
    }

    @Override
    public Set<CandidateInvariant> getOperands() {
      return delegate.getOperands();
    }
  }

  public static CandidateInvariant of(Iterable<? extends CandidateInvariant> pOperands) {
    ImmutableSet<? extends CandidateInvariant> operands = ImmutableSet.copyOf(flatten(pOperands));
    if (operands.size() == 1) {
      return operands.iterator().next();
    }
    FluentIterable<SingleLocationFormulaInvariant> singleLocationOperands = FluentIterable.from(operands).filter(SingleLocationFormulaInvariant.class);
    if (singleLocationOperands.size() == operands.size()) {
      Set<CFANode> locations =
          singleLocationOperands
              .transform(SingleLocationFormulaInvariant::getLocation)
              .toSet();
      if (locations.size() == 1) {
        // This admittedly hacky cast is safe here, because:
        // 1) We know all elements are of the desired type SingleLocationConjunction, and
        // 2) the set is immutable, so it will stay that way.
        // Thus, we don't need to create another copy.
        @SuppressWarnings({"unchecked"})
        SingleLocationConjunction result =
            new SingleLocationConjunction((ImmutableSet<SingleLocationFormulaInvariant>) operands);
        return result;
      }
    }
    return new GenericConjunction(operands);
  }

  private static Iterable<CandidateInvariant> flatten(
      Iterable<? extends CandidateInvariant> pOperands) {
    return FluentIterable.from(pOperands)
        .transformAndConcat(
            operand -> {
              if (operand instanceof Conjunction) {
                return flatten(((Conjunction) operand).getOperands());
              }
              return Collections.singleton(operand);
            });
  }

  public static Iterable<CandidateInvariant> getConjunctiveParts(
      CandidateInvariant pCandidateInvariant) {
    if (pCandidateInvariant instanceof Conjunction) {
      return ((Conjunction) pCandidateInvariant).getOperands();
    }
    return Collections.singleton(pCandidateInvariant);
  }

  private static CFANode getSingleLocation(
      Iterable<SingleLocationFormulaInvariant> pSingleLocationFormulaInvariants) {
    Iterator<SingleLocationFormulaInvariant> it = pSingleLocationFormulaInvariants.iterator();
    if (it.hasNext()) {
      CFANode location = it.next().getLocation();
      if (it.hasNext()) {
        assert Iterators.all(it, inv -> inv.getLocation().equals(location))
            : "Expected all operands to apply to the same single location, but at least two are different: "
                + pSingleLocationFormulaInvariants;
        return location;
      }
    }
    throw new IllegalArgumentException(
        "It makes no sense to use a CandidateInvariantConjunction unless there are at least two operands.");
  }
}
