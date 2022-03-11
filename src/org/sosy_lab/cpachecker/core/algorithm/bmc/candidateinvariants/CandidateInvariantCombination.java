// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class CandidateInvariantCombination {

  private interface Combination extends CandidateInvariant {

    Set<CandidateInvariant> getOperands();

    boolean isConjunction();

    @Override
    Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates);
  }

  private static class GenericCombination implements Combination {

    private final Set<CandidateInvariant> operands;

    private final boolean conjunction;

    private GenericCombination(
        ImmutableSet<? extends CandidateInvariant> pOperands, boolean pConjunction) {
      Preconditions.checkArgument(
          pOperands.size() > 1,
          "It makes no sense to use a CandidateInvariantCombination unless there are at least two"
              + " operands.");
      conjunction = pConjunction;
      operands = ImmutableSet.copyOf(pOperands);
    }

    @Override
    public BooleanFormula getFormula(
        FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
        throws CPATransferException, InterruptedException {
      BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
      BooleanFormula formula;
      if (isConjunction()) {
        formula = bfmgr.makeTrue();
        for (CandidateInvariant operand : operands) {
          formula = bfmgr.and(formula, operand.getFormula(pFMGR, pPFMGR, pContext));
        }
      } else {
        formula = bfmgr.makeFalse();
        for (CandidateInvariant operand : operands) {
          formula = bfmgr.or(formula, operand.getFormula(pFMGR, pPFMGR, pContext));
        }
      }
      return formula;
    }

    @Override
    public BooleanFormula getAssertion(
        Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
        throws CPATransferException, InterruptedException {
      BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
      BooleanFormula formula;
      if (isConjunction()) {
        formula = bfmgr.makeTrue();
        for (CandidateInvariant operand : operands) {
          formula = bfmgr.and(formula, operand.getAssertion(pReachedSet, pFMGR, pPFMGR));
        }
      } else {
        formula = bfmgr.makeFalse();
        for (CandidateInvariant operand : operands) {
          formula = bfmgr.or(formula, operand.getAssertion(pReachedSet, pFMGR, pPFMGR));
        }
      }
      return formula;
    }

    @Override
    public void assumeTruth(ReachedSet pReachedSet) {
      if (isConjunction()) {
        for (CandidateInvariant element : operands) {
          element.assumeTruth(pReachedSet);
        }
      }
    }

    @Override
    public int hashCode() {
      return (operands.hashCode() << 1) | (isConjunction() ? 1 : 0);
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof GenericCombination) {
        GenericCombination other = (GenericCombination) pOther;
        return conjunction == other.conjunction
            && operands.equals(((GenericCombination) pOther).operands);
      }
      return false;
    }

    @Override
    public String toString() {
      return operands.stream()
          .map(Object::toString)
          .collect(Collectors.joining(isConjunction() ? " and " : " or "));
    }

    @Override
    public boolean appliesTo(CFANode pLocation) {
      return Iterables.any(operands, e -> e.appliesTo(pLocation));
    }

    @Override
    public Set<CandidateInvariant> getOperands() {
      return operands;
    }

    @Override
    public boolean isConjunction() {
      return conjunction;
    }

    @Override
    public Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
      return FluentIterable.from(pStates)
          .filter(
              s ->
                  operands.stream()
                      .anyMatch(
                          op -> !Iterables.isEmpty(op.filterApplicable(Collections.singleton(s)))));
    }
  }

  private static class SingleLocationCombination extends SingleLocationFormulaInvariant
      implements Combination {

    private final Combination delegate;

    private SingleLocationCombination(
        Iterable<SingleLocationFormulaInvariant> pOperands, boolean pConjunction) {
      super(getSingleLocation(pOperands));
      delegate = new GenericCombination(ImmutableSet.copyOf(pOperands), pConjunction);
    }

    @Override
    public BooleanFormula getFormula(
        FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
        throws CPATransferException, InterruptedException {
      return delegate.getFormula(pFMGR, pPFMGR, pContext);
    }

    @Override
    public void assumeTruth(ReachedSet pReachedSet) {
      delegate.assumeTruth(pReachedSet);
    }

    @Override
    public Set<CandidateInvariant> getOperands() {
      return delegate.getOperands();
    }

    @Override
    public String toString() {
      return delegate.toString();
    }

    @Override
    public boolean isConjunction() {
      return delegate.isConjunction();
    }
  }

  public static CandidateInvariant conjunction(Iterable<? extends CandidateInvariant> pOperands) {
    return of(pOperands, true);
  }

  private static CandidateInvariant of(
      Iterable<? extends CandidateInvariant> pOperands, boolean pConjunction) {
    ImmutableSet<? extends CandidateInvariant> operands = ImmutableSet.copyOf(flatten(pOperands));
    if (operands.size() == 1) {
      return operands.iterator().next();
    }
    FluentIterable<SingleLocationFormulaInvariant> singleLocationOperands =
        FluentIterable.from(operands).filter(SingleLocationFormulaInvariant.class);
    if (singleLocationOperands.size() == operands.size()) {
      Set<CFANode> locations =
          singleLocationOperands.transform(SingleLocationFormulaInvariant::getLocation).toSet();
      if (locations.size() == 1) {
        // This admittedly hacky cast is safe here, because:
        // 1) We know all elements are of the desired type SingleLocationConjunction, and
        // 2) the set is immutable, so it will stay that way.
        // Thus, we don't need to create another copy.
        @SuppressWarnings("unchecked")
        SingleLocationFormulaInvariant result =
            singleLocationConjunction((Iterable<SingleLocationFormulaInvariant>) operands);
        return result;
      }
    }
    return new GenericCombination(operands, pConjunction);
  }

  public static SingleLocationFormulaInvariant singleLocationConjunction(
      SingleLocationFormulaInvariant... pOperands) {
    return singleLocationConjunction(Arrays.asList(pOperands));
  }

  public static SingleLocationFormulaInvariant singleLocationConjunction(
      Iterable<? extends SingleLocationFormulaInvariant> pOperands) {
    Set<SingleLocationFormulaInvariant> operands = ImmutableSet.copyOf(pOperands);
    if (operands.size() == 1) {
      return operands.iterator().next();
    }
    return new SingleLocationCombination(operands, true);
  }

  public static SingleLocationFormulaInvariant singleLocationDisjunction(
      SingleLocationFormulaInvariant... pOperands) {
    return singleLocationConjunction(Arrays.asList(pOperands));
  }

  public static SingleLocationFormulaInvariant singleLocationDisjunction(
      Iterable<? extends SingleLocationFormulaInvariant> pOperands) {
    Set<SingleLocationFormulaInvariant> operands = ImmutableSet.copyOf(pOperands);
    if (operands.size() == 1) {
      return operands.iterator().next();
    }
    return new SingleLocationCombination(operands, false);
  }

  private static Iterable<CandidateInvariant> flatten(
      Iterable<? extends CandidateInvariant> pOperands) {
    return FluentIterable.from(pOperands)
        .transformAndConcat(
            operand -> {
              if (operand instanceof Combination) {
                return flatten(((Combination) operand).getOperands());
              }
              return Collections.singleton(operand);
            });
  }

  public static Iterable<CandidateInvariant> getConjunctiveParts(
      CandidateInvariant pCandidateInvariant) {
    if (pCandidateInvariant instanceof Combination
        && ((Combination) pCandidateInvariant).isConjunction()) {
      return ((Combination) pCandidateInvariant).getOperands();
    }
    return Collections.singleton(pCandidateInvariant);
  }

  public static Iterable<CandidateInvariant> getDisjunctiveParts(
      Iterable<CandidateInvariant> pCandidateInvariants) {
    return FluentIterable.from(pCandidateInvariants)
        .transformAndConcat(CandidateInvariantCombination::getDisjunctiveParts);
  }

  public static Iterable<CandidateInvariant> getDisjunctiveParts(
      CandidateInvariant pCandidateInvariant) {
    if (pCandidateInvariant instanceof Combination
        && !((Combination) pCandidateInvariant).isConjunction()) {
      return ((Combination) pCandidateInvariant).getOperands();
    }
    return Collections.singleton(pCandidateInvariant);
  }

  public static Iterable<CandidateInvariant> getConjunctiveParts(
      Iterable<CandidateInvariant> pCandidateInvariants) {
    return FluentIterable.from(pCandidateInvariants)
        .transformAndConcat(CandidateInvariantCombination::getConjunctiveParts);
  }

  private static CFANode getSingleLocation(
      Iterable<SingleLocationFormulaInvariant> pSingleLocationFormulaInvariants) {
    Iterator<SingleLocationFormulaInvariant> it = pSingleLocationFormulaInvariants.iterator();
    if (it.hasNext()) {
      CFANode location = it.next().getLocation();
      if (it.hasNext()) {
        assert Iterators.all(it, inv -> inv.getLocation().equals(location))
            : "Expected all operands to apply to the same single location, but at least two are"
                + " different: "
                + pSingleLocationFormulaInvariants;
        return location;
      }
    }
    throw new IllegalArgumentException(
        "It makes no sense to use a CandidateInvariantConjunction unless there are at least two"
            + " operands.");
  }
}
