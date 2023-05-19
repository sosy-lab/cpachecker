// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.pdr;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class TotalTransitionRelation {

  private static final String LOCATION_VARIABLE_NAME = "__CPAchecker_location";

  private final Map<Integer, PartialTransitionRelation> totalTransitionRelation = new TreeMap<>();

  private final Set<CFANode> predecessorLocations;

  private final CFANode initialLocation;

  private final FormulaManagerView fmgr;

  public TotalTransitionRelation(
      FormulaManagerView pFmgr,
      CFANode pInitialLocation,
      Iterator<CFANode> pLoopHeadIterator,
      Function<CFANode, PartialTransitionRelation> pCreatePartialTransitionRelation) {
    fmgr = Objects.requireNonNull(pFmgr);
    initialLocation = Objects.requireNonNull(pInitialLocation);
    totalTransitionRelation.put(
        pInitialLocation.getNodeNumber(), pCreatePartialTransitionRelation.apply(pInitialLocation));
    while (pLoopHeadIterator.hasNext()) {
      CFANode predecessorLocation = pLoopHeadIterator.next();
      PartialTransitionRelation partialTransitionRelation =
          pCreatePartialTransitionRelation.apply(predecessorLocation);
      totalTransitionRelation.put(predecessorLocation.getNodeNumber(), partialTransitionRelation);
    }
    predecessorLocations =
        transformedImmutableSetCopy(
            totalTransitionRelation.values(), PartialTransitionRelation::getStartLocation);
  }

  public CFANode getInitialLocation() {
    return initialLocation;
  }

  public PartialTransitionRelation getInitiationRelation() {
    return totalTransitionRelation.get(initialLocation.getNodeNumber());
  }

  public Set<CFANode> getPredecessorLocations() {
    return predecessorLocations;
  }

  public FluentIterable<AbstractState> getPredecessorStates() {
    return FluentIterable.from(totalTransitionRelation.values())
        .transformAndConcat(
            partialTransitionRelation -> {
              ReachedSet reached = partialTransitionRelation.getReachedSet().getReachedSet();
              if (reached.size() <= 1) {
                return reached;
              }
              return Collections.singleton(reached.getFirstState());
            });
  }

  public AlgorithmStatus ensureK() throws InterruptedException, CPAException {
    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE;
    for (PartialTransitionRelation partialTransitionRelation : totalTransitionRelation.values()) {
      AlgorithmStatus partialStatus = partialTransitionRelation.ensureK();
      if (partialTransitionRelation.getStartLocation().equals(initialLocation)) {
        status = partialStatus;
      }
    }
    return status;
  }

  public BooleanFormula getTransitionFormula() {
    BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
    BooleanFormula transitionFormula = bfmgr.makeTrue();
    for (PartialTransitionRelation partialTransitionRelation : totalTransitionRelation.values()) {
      transitionFormula =
          bfmgr.and(
              transitionFormula,
              bfmgr.implication(
                  partialTransitionRelation.getStartLocationFormula(),
                  partialTransitionRelation.getFormula()));
    }
    return transitionFormula;
  }

  public BooleanFormula getPredecessorAssertion(CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException {
    return getPredecessorAssertions(Collections.singleton(pCandidateInvariant));
  }

  public BooleanFormula getPredecessorAssertions(
      Iterable<CandidateInvariant> pPredecessorAssertions)
      throws CPATransferException, InterruptedException {
    BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
    BooleanFormula assertions = bfmgr.makeTrue();
    for (PartialTransitionRelation partialTransitionRelation : totalTransitionRelation.values()) {
      assertions =
          bfmgr.and(
              assertions,
              partialTransitionRelation.getPredecessorAssertions(pPredecessorAssertions));
    }
    return assertions;
  }

  public BooleanFormula getSuccessorAssertion(CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException {
    BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
    BooleanFormula assertions = bfmgr.makeTrue();
    for (PartialTransitionRelation partialTransitionRelation : totalTransitionRelation.values()) {
      assertions =
          bfmgr.and(
              assertions, partialTransitionRelation.getSuccessorAssertion(pCandidateInvariant));
    }
    return assertions;
  }

  public Multimap<BooleanFormula, BooleanFormula> getSuccessorViolationAssertions(
      CandidateInvariant pCandidateInvariant) throws CPATransferException, InterruptedException {

    Multimap<BooleanFormula, BooleanFormula> successorViolationAssertions = HashMultimap.create();
    for (PartialTransitionRelation partialTransitionRelation : totalTransitionRelation.values()) {
      partialTransitionRelation.collectSuccessorViolationAssertions(
          pCandidateInvariant, successorViolationAssertions);
    }
    return successorViolationAssertions;
  }

  public PartialTransitionRelation getViolatedPartialTransition(
      List<ValueAssignment> pModelAssignments) {
    for (ValueAssignment valueAssignment : pModelAssignments) {
      String fullName = valueAssignment.getName();
      Pair<String, OptionalInt> varNameWithIndex = FormulaManagerView.parseName(fullName);
      String varName = varNameWithIndex.getFirst();
      OptionalInt index = varNameWithIndex.getSecond();
      if (index.isPresent() && index.orElseThrow() == 1 && varName.equals(LOCATION_VARIABLE_NAME)) {
        Object value = valueAssignment.getValue();
        if (value instanceof Number) {
          PartialTransitionRelation result =
              totalTransitionRelation.get(((Number) value).intValue());
          checkArgument(result != null, "Unknown location: %s", value);
          return result;
        }
      }
    }
    throw new IllegalArgumentException("No location information: " + pModelAssignments);
  }

  public Iterable<BooleanFormula> getPredecessorLocationFormulas() {
    return Iterables.transform(getPredecessorLocations(), l -> getUnprimedLocationFormula(fmgr, l));
  }

  public SymbolicCandiateInvariant getInitiationAssertion() {
    return SymbolicCandiateInvariant.makeSymbolicInvariant(
        predecessorLocations,
        getCandidateInvariantStatePredicate(),
        fmgr.uninstantiate(getInitiationRelation().getStartLocationFormula()),
        fmgr);
  }

  public Predicate<? super AbstractState> getCandidateInvariantStatePredicate() {
    return s ->
        FluentIterable.from(AbstractStates.extractLocations(s)).stream()
            .anyMatch(predecessorLocations::contains);
  }

  public static String getLocationVariableName() {
    return LOCATION_VARIABLE_NAME;
  }

  static BooleanFormula getUnprimedLocationFormula(FormulaManagerView pFmgr, CFANode pLocation) {
    IntegerFormulaManager ifmgr = pFmgr.getIntegerFormulaManager();
    IntegerFormula variable =
        pFmgr.makeVariable(ifmgr.getFormulaType(), getLocationVariableName(), 1);
    return ifmgr.equal(variable, ifmgr.makeNumber(pLocation.getNodeNumber()));
  }

  static BooleanFormula getPrimedLocationFormula(FormulaManagerView pFmgr, CFANode pLocation) {
    IntegerFormulaManager ifmgr = pFmgr.getIntegerFormulaManager();
    IntegerFormula variable =
        pFmgr.makeVariable(ifmgr.getFormulaType(), getLocationVariableName(), 2);
    return ifmgr.equal(variable, ifmgr.makeNumber(pLocation.getNodeNumber()));
  }

  static String getVariableName(FormulaManagerView pFmgr, Formula pVariable) {
    Set<String> variableNames = pFmgr.extractVariableNames(pVariable);
    checkArgument(variableNames.size() == 1, "Not a variable: %s", pVariable);
    return variableNames.iterator().next();
  }
}
