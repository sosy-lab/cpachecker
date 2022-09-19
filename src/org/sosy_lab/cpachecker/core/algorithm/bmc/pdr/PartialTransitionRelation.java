// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.pdr;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CounterexampleToInductivity;
import org.sosy_lab.cpachecker.core.algorithm.bmc.ModelValue;
import org.sosy_lab.cpachecker.core.algorithm.bmc.UnrolledReachedSet;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariantCombination;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.input.InputState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

class PartialTransitionRelation implements Comparable<PartialTransitionRelation> {

  private final CFANode startLocation;

  private final LogManager logger;

  private final UnrolledReachedSet reachedSet;

  private final Set<CFANode> loopHeads;

  private final FormulaManagerView fmgr;

  private final BooleanFormulaManager bfmgr;

  private final PathFormulaManager pmgr;

  private @Nullable ImmutableSet<AbstractState> currentEndStates = null;
  private @Nullable ImmutableMap<String, Formula> currentVariables = null;

  private int lastK = -1;

  public PartialTransitionRelation(
      CFANode pStartLocation,
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      FormulaManagerView pFmgr,
      PathFormulaManager pPmgr,
      LogManager pLogger,
      ReachedSet pReachedSet,
      Set<CFANode> pLoopHeads) {
    startLocation = Objects.requireNonNull(pStartLocation);
    logger = Objects.requireNonNull(pLogger);
    loopHeads = Objects.requireNonNull(pLoopHeads);
    reachedSet = new UnrolledReachedSet(pAlgorithm, pCpa, pLoopHeads, pReachedSet, this::ensureK);
    fmgr = Objects.requireNonNull(pFmgr);
    bfmgr = pFmgr.getBooleanFormulaManager();
    pmgr = Objects.requireNonNull(pPmgr);
  }

  public int getDesiredK() {
    return reachedSet.getDesiredK();
  }

  public void setDesiredK(int pK) {
    if (pK < reachedSet.getCurrentMaxK()) {
      throw new IllegalArgumentException(
          "The length of this transition relation is "
              + reachedSet.getCurrentMaxK()
              + " and cannot be decreased.");
    }
    reachedSet.setDesiredK(pK);
  }

  public AlgorithmStatus ensureK() throws InterruptedException, CPAException {
    if (reachedSet.getDesiredK() > lastK) {
      currentEndStates = null;
      currentVariables = null;
    }
    return reachedSet.ensureK();
  }

  public CFANode getStartLocation() {
    return startLocation;
  }

  public ImmutableSet<AbstractState> getEndStates() {
    int desiredK = getDesiredK();
    if (currentEndStates != null && lastK == desiredK) {
      return currentEndStates;
    }
    currentEndStates =
        filterIterationsUpTo(reachedSet.getReachedSet(), desiredK)
            .filter(
                state ->
                    BMCHelper.isEndState(state)
                        || (BMCHelper.hasMatchingLocation(state, loopHeads)
                            && !Iterables.isEmpty(
                                filterIteration(Collections.singleton(state), desiredK))))
            .toSet();
    currentVariables = null;
    lastK = desiredK;
    return currentEndStates;
  }

  public boolean transitionsTo(PartialTransitionRelation pOther) {
    return Iterables.contains(
        AbstractStates.extractLocations(getEndStates()), pOther.getStartLocation());
  }

  @Override
  public int compareTo(PartialTransitionRelation pOther) {
    return startLocation.compareTo(pOther.startLocation);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof PartialTransitionRelation) {
      PartialTransitionRelation other = (PartialTransitionRelation) pOther;
      return startLocation.equals(other.startLocation)
          && bfmgr.equals(other.bfmgr)
          && fmgr.equals(other.fmgr)
          && pmgr.equals(other.pmgr)
          && reachedSet.equals(other.reachedSet)
          && logger.equals(other.logger)
          && loopHeads.equals(other.loopHeads);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startLocation, bfmgr, fmgr, pmgr, reachedSet, logger, loopHeads);
  }

  private AlgorithmStatus ensureK(
      Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, ReachedSet pReachedSet)
      throws InterruptedException, CPAException {
    if (pReachedSet.size() < 1) {
      AbstractState initialState =
          pCpa.getInitialState(startLocation, StateSpacePartition.getDefaultPartition());
      Precision precision =
          pCpa.getInitialPrecision(startLocation, StateSpacePartition.getDefaultPartition());
      pReachedSet.add(initialState, precision);
      if (pReachedSet.isEmpty()) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
    }
    return BMCHelper.unroll(logger, pReachedSet, pAlgorithm, pCpa);
  }

  public UnrolledReachedSet getReachedSet() {
    return reachedSet;
  }

  private FluentIterable<AbstractState> filterIteration(
      Iterable<AbstractState> pStates, int pIteration) {
    return filterIterationsBetween(pStates, pIteration, pIteration);
  }

  private FluentIterable<AbstractState> filterIterationsUpTo(
      Iterable<AbstractState> pStates, int pIteration) {
    return filterIterationsBetween(pStates, 0, pIteration);
  }

  private FluentIterable<AbstractState> filterIterationsBetween(
      Iterable<AbstractState> pStates, int pMinIt, int pMaxIt) {
    if (pMinIt > pMaxIt) {
      throw new IllegalArgumentException(
          String.format("Minimum (%d) not lower than maximum (%d)", pMinIt, pMaxIt));
    }
    checkArgument(pMinIt >= 0, "Minimum must not be lower than 0 but is %s", pMinIt);
    int min = pMinIt;
    int max = pMaxIt;
    Set<CFANode> startLocations = loopHeads;
    if (startLocation.getNumEnteringEdges() == 0 && startLocation instanceof FunctionEntryNode) {
      startLocations = Sets.union(startLocations, Collections.singleton(startLocation));
      --min;
      --max;
      if (max < 0) {
        ReachedSet reached = reachedSet.getReachedSet();
        if (reached.isEmpty()) {
          return FluentIterable.of();
        }
        return FluentIterable.of(reached.getFirstState());
      } else {
        min = Math.max(0, min);
        max = Math.max(0, max);
      }
    }
    return BMCHelper.filterIterationsBetween(pStates, min, max, startLocations);
  }

  public ImmutableMap<String, Formula> getVariables() {
    int desiredK = getDesiredK();
    if (currentVariables != null && lastK == desiredK) {
      return currentVariables;
    }
    currentEndStates = null;
    Set<CFANode> relevantLocations = Sets.union(loopHeads, Collections.singleton(startLocation));
    Iterable<AbstractState> relevantStates =
        filterIterationsUpTo(
            AbstractStates.filterLocations(reachedSet.getReachedSet(), relevantLocations),
            desiredK);
    currentVariables =
        AbstractStates.projectToType(relevantStates, PredicateAbstractState.class).stream()
            .map(PartialTransitionRelation::getPathFormula)
            .flatMap(
                pathFormula -> {
                  SSAMap ssaMap = pathFormula.getSsa();
                  return ssaMap.allVariables().stream()
                      .filter(name -> !name.startsWith("*"))
                      .map(
                          name -> {
                            return pmgr.makeFormulaForUninstantiatedVariable(
                                name,
                                ssaMap.getType(name),
                                pathFormula.getPointerTargetSet(),
                                false);
                          });
                })
            .distinct()
            .collect(
                ImmutableMap.toImmutableMap(
                    f -> fmgr.extractVariableNames(f).iterator().next(), Function.identity()));
    lastK = desiredK;
    return currentVariables;
  }

  public BooleanFormula getPredecessorAssertions(
      Iterable<CandidateInvariant> pPredecessorAssertions)
      throws CPATransferException, InterruptedException {
    return getStateAssertions(
        pPredecessorAssertions, states -> filterIterationsUpTo(states, getDesiredK() - 1), 1);
  }

  public BooleanFormula getSuccessorAssertion(CandidateInvariant pSuccessorAssertion)
      throws CPATransferException, InterruptedException {
    return getSuccessorAssertions(Collections.singleton(pSuccessorAssertion));
  }

  public BooleanFormula getSuccessorAssertions(Iterable<CandidateInvariant> pSuccessorAssertions)
      throws CPATransferException, InterruptedException {
    return getStateAssertions(
        pSuccessorAssertions, states -> filterIteration(states, getDesiredK()), 2);
  }

  private BooleanFormula getStateAssertions(
      Iterable<CandidateInvariant> pAssertions,
      UnaryOperator<Iterable<AbstractState>> pStateFilter,
      int pDefaultIndex)
      throws CPATransferException, InterruptedException {
    BooleanFormula assertions = bfmgr.makeBoolean(true);
    for (CandidateInvariant assertion : pAssertions) {
      for (CandidateInvariant conjunctivePart :
          CandidateInvariantCombination.getConjunctiveParts(assertion)) {
        assertions =
            bfmgr.and(assertions, getStateAssertion(conjunctivePart, pStateFilter, pDefaultIndex));
      }
    }
    return assertions;
  }

  private BooleanFormula getStateAssertion(
      CandidateInvariant pAssertion,
      UnaryOperator<Iterable<AbstractState>> pStateFilter,
      int pDefaultIndex)
      throws CPATransferException, InterruptedException {
    ReachedSet reached = reachedSet.getReachedSet();
    Set<AbstractState> states =
        filterIterationsUpTo(
                pStateFilter.apply(pAssertion.filterApplicable(reached)), getDesiredK())
            .toSet();

    BooleanFormula stateAssertionFormula = bfmgr.makeTrue();

    for (AbstractState state : states) {
      BooleanFormula stateFormula = getStateFormula(state);

      // Use index 2 for successor locations
      BooleanFormula invariantFormula = instantiateAt(state, pAssertion, pDefaultIndex);

      stateAssertionFormula =
          bfmgr.and(stateAssertionFormula, bfmgr.implication(stateFormula, invariantFormula));
    }
    return stateAssertionFormula;
  }

  private BooleanFormula getStateFormula(AbstractState state) {
    BooleanFormula startLocationFormula = getStartLocationFormula();
    BooleanFormula stateFormula = getPathFormula(state).getFormula();
    stateFormula = bfmgr.and(startLocationFormula, stateFormula);
    if (getEndStates().contains(state)) {
      BooleanFormula endLocationFormula = bfmgr.makeFalse();
      for (CFANode endLocation : AbstractStates.extractLocations(state)) {
        endLocationFormula =
            bfmgr.or(
                endLocationFormula,
                TotalTransitionRelation.getPrimedLocationFormula(fmgr, endLocation));
      }
      stateFormula = bfmgr.and(stateFormula, endLocationFormula);
    }
    return stateFormula;
  }

  void collectSuccessorViolationAssertions(
      CandidateInvariant pCandidateInvariant, Multimap<BooleanFormula, BooleanFormula> pCollection)
      throws CPATransferException, InterruptedException {
    ReachedSet reached = reachedSet.getReachedSet();
    Iterable<AbstractState> assertionStates =
        filterIteration(pCandidateInvariant.filterApplicable(reached), getDesiredK());

    for (AbstractState state : assertionStates) {
      BooleanFormula stateFormula = getStateFormula(state);

      // Use index 2 for successor locations
      BooleanFormula invariantFormula = instantiateAt(state, pCandidateInvariant, 2);
      pCollection.put(stateFormula, bfmgr.not(invariantFormula));
    }
  }

  private BooleanFormula instantiateAt(
      AbstractState pState, CandidateInvariant pCandidateInvariant, int pDefaultIndex)
      throws CPATransferException, InterruptedException {
    PathFormula pathFormula = getPathFormula(pState);
    SSAMap ssaMap =
        pathFormula.getSsa().withDefault(pDefaultIndex); // Use index 2 for successor locations
    pathFormula = pathFormula.withContext(ssaMap, pathFormula.getPointerTargetSet());
    BooleanFormula uninstantiatedFormula = pCandidateInvariant.getFormula(fmgr, pmgr, pathFormula);
    return fmgr.instantiate(uninstantiatedFormula, ssaMap);
  }

  public BooleanFormula getStartLocationFormula() {
    return TotalTransitionRelation.getUnprimedLocationFormula(fmgr, startLocation);
  }

  public BooleanFormula getFormula() {
    ImmutableSet<AbstractState> endStates = getEndStates();
    ReachedSet reached = reachedSet.getReachedSet();
    if (reached.isEmpty() || endStates.isEmpty()) {
      return bfmgr.makeFalse();
    }

    // Create the transition formula for each end state and disjoin them
    BooleanFormula transition = bfmgr.makeFalse();
    for (AbstractState endState : endStates) {
      // Build the state formula
      BooleanFormula endStateFormula = getStateFormula(endState);
      transition = bfmgr.or(transition, endStateFormula);
    }
    return transition;
  }

  public CtiWithInputs getCtiWithInputs(List<ValueAssignment> pModelAssignments) {
    Map<String, CType> types = new HashMap<>();
    Multimap<String, Integer> inputs =
        extractInputs(
            filterIterationsUpTo(getReachedSet().getReachedSet(), getDesiredK() + 1), types);
    ImmutableMap<String, Formula> variables = getVariables();

    PersistentMap<String, ModelValue> model = PathCopyingPersistentTreeMap.of();

    for (ValueAssignment valueAssignment : pModelAssignments) {
      if (!valueAssignment.isFunction()) {
        String fullName = valueAssignment.getName();
        Pair<String, OptionalInt> pair = FormulaManagerView.parseName(fullName);
        String actualName = pair.getFirst();
        OptionalInt index = pair.getSecond();
        Object value = valueAssignment.getValue();
        if (index.isPresent()
            && index.orElseThrow() == 1
            && value instanceof Number
            && (actualName.equals(TotalTransitionRelation.getLocationVariableName())
                || (variables.containsKey(actualName)
                    && !inputs.get(actualName).contains(index.orElseThrow())))) {
          BooleanFormula assignment = fmgr.uninstantiate(valueAssignment.getAssignmentAsFormula());
          model = model.putAndCopy(actualName, new ModelValue(actualName, assignment, fmgr));
        }
      }
    }
    CounterexampleToInductivity cti = new CounterexampleToInductivity(startLocation, model);

    return new CtiWithInputs(
        cti,
        getInputAssignments(fmgr, variables, getReachedSet().getReachedSet(), pModelAssignments));
  }

  private static BooleanFormula getInputAssignments(
      FormulaManagerView pFmgr,
      Map<String, Formula> pVariables,
      Iterable<AbstractState> pReached,
      Iterable<ValueAssignment> pModelAssignments) {
    BooleanFormulaManager bfmgr = pFmgr.getBooleanFormulaManager();
    BooleanFormula inputAssignments = bfmgr.makeTrue();
    Map<String, CType> types = new HashMap<>();
    Multimap<String, Integer> inputs = extractInputs(pReached, types);
    if (inputs.isEmpty()) {
      return inputAssignments;
    }

    for (ValueAssignment valueAssignment : pModelAssignments) {
      if (!valueAssignment.isFunction()) {
        String fullName = valueAssignment.getName();
        Pair<String, OptionalInt> pair = FormulaManagerView.parseName(fullName);
        String actualName = pair.getFirst();
        OptionalInt index = pair.getSecond();

        // We consider as inputs
        // a) those that have an SSA index and are contained in our list of input variables and
        // b) those that have no SSA index and are not known as actual variables,
        // such as __ADDRESS_OF:
        if ((index.isPresent() && inputs.get(actualName).contains(index.orElseThrow()))
            || (!index.isPresent() && !pVariables.containsKey(actualName))) {
          inputAssignments = bfmgr.and(inputAssignments, valueAssignment.getAssignmentAsFormula());
        }
      }
    }
    return inputAssignments;
  }

  private static Multimap<String, Integer> extractInputs(
      Iterable<AbstractState> pReached, Map<String, CType> types) {
    Multimap<String, Integer> inputs = LinkedHashMultimap.create();
    for (AbstractState s : pReached) {
      InputState is = AbstractStates.extractStateByType(s, InputState.class);
      if (is != null) {
        PredicateAbstractState pas =
            AbstractStates.extractStateByType(s, PredicateAbstractState.class);
        SSAMap ssaMap = pas.getPathFormula().getSsa();
        for (String input : is.getInputs()) {
          if (ssaMap.containsVariable(input)) {
            inputs.put(input, ssaMap.getIndex(input) - 1);
            types.put(input, ssaMap.getType(input));
          }
        }
        for (String varName : ssaMap.allVariables()) {
          types.put(varName, ssaMap.getType(varName));
        }
      }
    }
    return inputs;
  }

  private static PathFormula getPathFormula(AbstractState pPas) {
    PredicateAbstractState pas =
        AbstractStates.extractStateByType(pPas, PredicateAbstractState.class);
    return getPathFormula(pas);
  }

  private static PathFormula getPathFormula(PredicateAbstractState pPas) {
    if (pPas.isAbstractionState()) {
      return pPas.getAbstractionFormula().getBlockFormula();
    }
    return pPas.getPathFormula();
  }

  static class CtiWithInputs {

    private final CounterexampleToInductivity cti;

    private final BooleanFormula inputs;

    public CtiWithInputs(CounterexampleToInductivity pCti, BooleanFormula pInputs) {
      cti = Objects.requireNonNull(pCti);
      inputs = Objects.requireNonNull(pInputs);
    }

    @Override
    public String toString() {
      return cti + " with inputs " + inputs;
    }

    public CounterexampleToInductivity getCti() {
      return cti;
    }

    public BooleanFormula getInputs() {
      return inputs;
    }
  }
}
