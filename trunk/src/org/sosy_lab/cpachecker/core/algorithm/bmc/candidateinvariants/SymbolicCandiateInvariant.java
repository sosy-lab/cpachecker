// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CounterexampleToInductivity;
import org.sosy_lab.cpachecker.core.algorithm.bmc.ModelValue;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;

public class SymbolicCandiateInvariant implements CandidateInvariant {

  private final LoadingCache<FormulaManagerView, BooleanFormula> cachedFormulas;

  private final Set<CFANode> applicableLocations;

  private final String invariant;

  private final Predicate<? super AbstractState> stateFilter;

  private final Supplier<String> textualRepresentation;

  /** Is the invariant known to be the boolean constant 'false' */
  private boolean isDefinitelyBooleanFalse = false;

  private SymbolicCandiateInvariant(
      Iterable<CFANode> pApplicableLocations,
      Predicate<? super AbstractState> pStateFilter,
      String pInvariant,
      Supplier<String> pTextualRepresentation) {
    applicableLocations = ImmutableSet.copyOf(pApplicableLocations);
    cachedFormulas =
        CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(CacheLoader.from(fmgr -> fmgr.parse(pInvariant)));
    invariant = pInvariant;
    stateFilter = Objects.requireNonNull(pStateFilter);
    textualRepresentation = Objects.requireNonNull(pTextualRepresentation);
  }

  @Override
  public final BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws CPATransferException, InterruptedException {
    Iterable<AbstractState> locationStates = filterApplicable(pReachedSet);
    return BMCHelper.assertAt(locationStates, this, pFMGR, pPFMGR);
  }

  @Override
  public Iterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return AbstractStates.filterLocations(pStates, applicableLocations).filter(stateFilter::test);
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFmgr, PathFormulaManager pPfmgr, PathFormula pContext)
      throws InterruptedException {
    return getPlainFormula(pFmgr);
  }

  @Override
  public String toString() {
    return textualRepresentation.get();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof SymbolicCandiateInvariant) {
      SymbolicCandiateInvariant other = (SymbolicCandiateInvariant) pOther;
      return stateFilter.equals(other.stateFilter)
          && invariant.equals(other.invariant)
          && applicableLocations.equals(other.applicableLocations);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(stateFilter, invariant, applicableLocations);
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    if (isDefinitelyBooleanFalse) {
      Iterable<AbstractState> targetStates = ImmutableList.copyOf(filterApplicable(pReachedSet));
      pReachedSet.removeAll(targetStates);
      for (ARGState s : from(targetStates).filter(ARGState.class)) {
        s.removeFromARG();
      }
    }
  }

  @Override
  public boolean appliesTo(CFANode pLocation) {
    return applicableLocations.contains(pLocation);
  }

  public Set<CFANode> getApplicableLocations() {
    return applicableLocations;
  }

  public Predicate<? super AbstractState> getStateFilter() {
    return stateFilter;
  }

  public BooleanFormula getPlainFormula(FormulaManagerView pFmgr) throws InterruptedException {
    if (isDefinitelyBooleanFalse) {
      return pFmgr.getBooleanFormulaManager().makeFalse();
    }

    BooleanFormula formula;
    try {
      formula = cachedFormulas.get(pFmgr);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause != null) {
        Throwables.propagateIfPossible(cause, InterruptedException.class);
        throw new UncheckedExecutionException(cause);
      }
      throw new UncheckedExecutionException(e);
    }

    if (!isDefinitelyBooleanFalse && pFmgr.getBooleanFormulaManager().isFalse(formula)) {
      isDefinitelyBooleanFalse = true;
    }

    return formula;
  }

  public SymbolicCandiateInvariant negate(FormulaManagerView pFmgr) throws InterruptedException {
    return makeSymbolicInvariant(
        applicableLocations,
        stateFilter,
        pFmgr.getBooleanFormulaManager().not(getPlainFormula(pFmgr)),
        pFmgr);
  }

  public Iterable<SymbolicCandiateInvariant> splitLiterals(
      FormulaManagerView pFMGR, boolean pSplitNumeralEqualities) throws InterruptedException {
    BooleanFormula formula = getPlainFormula(pFMGR);
    Iterable<BooleanFormula> literals =
        getConjunctionOperands(pFMGR, formula, pSplitNumeralEqualities);
    return Iterables.transform(
        literals, f -> makeSymbolicInvariant(applicableLocations, stateFilter, f, pFMGR));
  }

  public static BlockedCounterexampleToInductivity blockCti(
      Set<CFANode> pApplicableLocations,
      CounterexampleToInductivity pCtiToBlock,
      FormulaManagerView pOriginalFormulaManager) {
    return blockCti(
        pApplicableLocations, Predicates.alwaysTrue(), pCtiToBlock, pOriginalFormulaManager);
  }

  public static BlockedCounterexampleToInductivity blockCti(
      Set<CFANode> pApplicableLocations,
      Predicate<? super AbstractState> pStateFilter,
      CounterexampleToInductivity pCtiToBlock,
      FormulaManagerView pOriginalFormulaManager) {
    return new BlockedCounterexampleToInductivity(
        pApplicableLocations, pStateFilter, pCtiToBlock, pOriginalFormulaManager);
  }

  public static SymbolicCandiateInvariant makeSymbolicInvariant(
      Set<CFANode> pApplicableLocations,
      Predicate<? super AbstractState> pStateFilter,
      BooleanFormula pInvariant,
      FormulaManagerView pOriginalFormulaManager) {
    return new SymbolicCandiateInvariant(
        pApplicableLocations,
        pStateFilter,
        pOriginalFormulaManager.dumpFormula(pInvariant).toString(),
        pInvariant::toString);
  }

  public static Iterable<BooleanFormula> getConjunctionOperands(
      FormulaManagerView pFMGR, BooleanFormula pFormula, boolean pSplitNumeralEqualities) {
    Iterable<BooleanFormula> operands = getConjunctionOperands(pFMGR, pFormula);
    if (!pSplitNumeralEqualities) {
      return operands;
    }
    return FluentIterable.from(operands).transformAndConcat(pFMGR::splitNumeralEqualityIfPossible);
  }

  private static Iterable<BooleanFormula> getConjunctionOperands(
      FormulaManagerView pFMGR, BooleanFormula pFormula) {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    return bfmgr.visit(
        pFormula,
        new DefaultBooleanFormulaVisitor<Iterable<BooleanFormula>>() {

          @Override
          protected Iterable<BooleanFormula> visitDefault() {
            return pFMGR.splitNumeralEqualityIfPossible(pFormula);
          }

          @Override
          public Iterable<BooleanFormula> visitAnd(List<BooleanFormula> pArg0) {
            return FluentIterable.from(pArg0)
                .transformAndConcat(operand -> getConjunctionOperands(pFMGR, operand));
          }

          @Override
          public Iterable<BooleanFormula> visitNot(BooleanFormula pArg0) {
            FluentIterable<BooleanFormula> disjunctionOperands =
                FluentIterable.from(getDisjunctionOperands(pFMGR, pArg0));
            if (disjunctionOperands.skip(1).isEmpty()) {
              return Collections.singleton(bfmgr.not(disjunctionOperands.iterator().next()));
            }
            return disjunctionOperands.transformAndConcat(
                innerOp -> getConjunctionOperands(pFMGR, bfmgr.not(innerOp)));
          }
        });
  }

  private static Iterable<BooleanFormula> getDisjunctionOperands(
      FormulaManagerView pFMGR, BooleanFormula pFormula) {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    return bfmgr.visit(
        pFormula,
        new DefaultBooleanFormulaVisitor<Iterable<BooleanFormula>>() {

          @Override
          protected Iterable<BooleanFormula> visitDefault() {
            return Collections.singleton(pFormula);
          }

          @Override
          public Iterable<BooleanFormula> visitOr(List<BooleanFormula> pArg0) {
            return FluentIterable.from(pArg0)
                .transformAndConcat(operand -> getDisjunctionOperands(pFMGR, operand));
          }

          @Override
          public Iterable<BooleanFormula> visitNot(BooleanFormula pArg0) {
            FluentIterable<BooleanFormula> conjunctionOperands =
                FluentIterable.from(getConjunctionOperands(pFMGR, pArg0));
            if (conjunctionOperands.skip(1).isEmpty()) {
              return Collections.singleton(bfmgr.not(conjunctionOperands.iterator().next()));
            }
            return conjunctionOperands.transformAndConcat(
                innerOp -> getDisjunctionOperands(pFMGR, bfmgr.not(innerOp)));
          }
        });
  }

  public static class BlockedCounterexampleToInductivity extends SymbolicCandiateInvariant {

    private final CounterexampleToInductivity blockedCti;

    private BlockedCounterexampleToInductivity(
        Set<CFANode> pApplicableLocations,
        Predicate<? super AbstractState> pStateFilter,
        CounterexampleToInductivity pCtiToBlock,
        FormulaManagerView pFmgr) {
      super(
          pApplicableLocations,
          pStateFilter,
          pFmgr
              .dumpFormula(pFmgr.getBooleanFormulaManager().not(pCtiToBlock.getFormula(pFmgr)))
              .toString(),
          () -> "");
      blockedCti = Objects.requireNonNull(pCtiToBlock);
    }

    @Override
    public BooleanFormula getFormula(
        FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext) {
      BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
      BooleanFormula model = bfmgr.makeFalse();
      for (Map.Entry<String, ModelValue> valueAssignment : blockedCti.getAssignments().entrySet()) {
        String variableName = valueAssignment.getKey();
        ModelValue v = valueAssignment.getValue();
        assert variableName.equals(v.getVariableName());
        model = bfmgr.or(model, bfmgr.not(v.toAssignment(pFMGR)));
      }
      return model;
    }

    public CounterexampleToInductivity getCti() {
      return blockedCti;
    }

    @Override
    public String toString() {
      return String.format("!%s", blockedCti.getAssignments().values());
    }

    @Override
    public boolean equals(Object pOther) {
      return super.equals(pOther);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

  public static Multimap<Set<AbstractState>, SymbolicCandiateInvariant> indexByApplicableStates(
      Iterable<SymbolicCandiateInvariant> pCandidateInvariants, Iterable<AbstractState> pStates) {
    if (Iterables.isEmpty(pCandidateInvariants)) {
      return ImmutableListMultimap.of();
    }
    ImmutableListMultimap.Builder<Set<AbstractState>, SymbolicCandiateInvariant> builder =
        ImmutableListMultimap.builder();

    Iterator<SymbolicCandiateInvariant> candidateIterator = pCandidateInvariants.iterator();
    SymbolicCandiateInvariant first = candidateIterator.next();
    Set<AbstractState> firstApplicableStates = Sets.newHashSet(first.filterApplicable(pStates));
    builder.put(firstApplicableStates, first);
    while (candidateIterator.hasNext()) {
      SymbolicCandiateInvariant current = candidateIterator.next();
      if (current.stateFilter.equals(first.stateFilter)
          && current.applicableLocations.equals(first.applicableLocations)) {
        builder.put(firstApplicableStates, current);
      } else {
        builder.put(Sets.newHashSet(current.filterApplicable(pStates)), current);
      }
    }

    return builder.build();
  }
}
