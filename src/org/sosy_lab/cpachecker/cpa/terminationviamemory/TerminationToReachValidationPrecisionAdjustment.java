// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.CURR_KEYWORD;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.TRANS_INV_KEYWORD;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.ExpressionTreeLocationTransitionInvariant;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Precision adjustment for the validation of termination witnesses. In addition to the transition
 * invariants found by {@link TerminationToReachPrecisionAdjustment}, it also uses the transition
 * invariants from the witness if they are inductive. If one of them is not inductive, it is checked
 * on the path to the loop head instead. If it does not hold there, the analysis stops without a
 * result, since the witness cannot be confirmed.
 */
public class TerminationToReachValidationPrecisionAdjustment
    extends TerminationToReachPrecisionAdjustment {

  private final PathFormulaManager pthfmgr;
  private final ImmutableSet<ExpressionTreeLocationInvariant> candidateInvariants;
  // Whether the transition invariant of the witness is not inductive at the state of the current
  // call of prec, so that it has to be checked on the path to that state
  private boolean isWitnessTransitionInvariantNotInductive = false;
  // A transition invariant of the witness that does not hold on some path cannot be confirmed
  private boolean isWitnessTransitionInvariantViolated = false;

  public TerminationToReachValidationPrecisionAdjustment(
      Solver pSolver,
      LogManager plogger,
      CFA pCFA,
      BooleanFormulaManagerView pBfmgr,
      FormulaManagerView pFmgr,
      PathFormulaManager pPthfmgr,
      InterpolationManager pItpMgr,
      Configuration pConfiguration,
      ImmutableSet<Loop> pAllLoops,
      ImmutableSet<ExpressionTreeLocationInvariant> pCandidateInvariants)
      throws InvalidConfigurationException, InterruptedException {
    super(
        pSolver,
        plogger,
        pCFA,
        pBfmgr,
        pFmgr,
        pItpMgr,
        pConfiguration,
        pAllLoops,
        collectSupportingInvariants(pCandidateInvariants, pFmgr, pPthfmgr, plogger));
    pthfmgr = pPthfmgr;
    candidateInvariants = pCandidateInvariants;
  }

  /**
   * Conjoins the (supporting) invariants from the witness at each location. Their variables are
   * renamed to the variables of the state where the iteration starts. The witness is only used if
   * these invariants were proven to hold, see {@link TerminationToReachCPA}.
   */
  private static ImmutableMap<CFANode, BooleanFormula> collectSupportingInvariants(
      ImmutableSet<ExpressionTreeLocationInvariant> pCandidateInvariants,
      FormulaManagerView pFmgr,
      PathFormulaManager pPthfmgr,
      LogManager pLogger)
      throws InterruptedException {
    Map<CFANode, BooleanFormula> invariantsAtLocations = new HashMap<>();
    for (ExpressionTreeLocationInvariant invariant : pCandidateInvariants) {
      if (invariant instanceof ExpressionTreeLocationTransitionInvariant
          || invariant.asExpressionTree().equals(ExpressionTrees.getTrue())) {
        continue;
      }
      try {
        invariantsAtLocations.merge(
            invariant.getLocation(),
            invariant.getFormula(pFmgr, pPthfmgr, pPthfmgr.makeEmptyPathFormula()),
            pFmgr.getBooleanFormulaManager()::and);
      } catch (CPATransferException e) {
        // Ignoring an invariant only weakens the checks
        pLogger.logDebugException(e, "Could not convert the supporting invariant " + invariant);
      }
    }
    return ImmutableMap.copyOf(
        Maps.transformValues(
            invariantsAtLocations,
            invariant ->
                pFmgr.substitute(
                    invariant,
                    ImmutableMap.copyOf(
                        Maps.asMap(
                            ImmutableSet.copyOf(pFmgr.extractVariables(invariant).values()),
                            variable ->
                                pFmgr.makeVariable(
                                    pFmgr.getFormulaType(variable),
                                    pFmgr.uninstantiate(variable).toString() + CURR_KEYWORD))))));
  }

  /**
   * Stops the analysis once a transition invariant of the witness does not hold on the path to a
   * loop head. Otherwise, the program could be proven terminating with an invalid witness. A
   * transition invariant that is inductive at a loop head holds on the path to it if it held at the
   * previous visit, so only the transition invariants that are not inductive are checked on the
   * path.
   */
  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState)
      throws CPAException, InterruptedException {
    isWitnessTransitionInvariantNotInductive = false;
    Optional<PrecisionAdjustmentResult> result =
        super.prec(state, precision, states, stateProjection, fullState);
    if (!isWitnessTransitionInvariantViolated
        && isWitnessTransitionInvariantNotInductive
        && result.isPresent()
        && !holdsWitnessTransitionInvariantOnPath((TerminationToReachState) state, fullState)) {
      logger.logf(
          Level.INFO,
          "The transition invariant of the witness at %s does not hold on the path to it, so the"
              + " witness cannot be confirmed.",
          AbstractStates.extractLocation(fullState));
      isWitnessTransitionInvariantViolated = true;
    }
    return isWitnessTransitionInvariantViolated
        ? result.map(adjusted -> adjusted.withAction(Action.BREAK))
        : result;
  }

  /**
   * Checks that the transition invariant of the witness holds between the values at each earlier
   * visit of the loop head and the current values, on the path to the current state. This follows
   * the meaning of {@code \at(x, AnyPrev)}, i.e., the value of {@code x} in any previous state of
   * the path at that location.
   *
   * @return whether the transition invariant holds, i.e., false if it is violated or the check
   *     fails
   */
  private boolean holdsWitnessTransitionInvariantOnPath(
      TerminationToReachState pState, AbstractState pFullState) throws InterruptedException {
    LocationState locationState =
        AbstractStates.extractStateByType(pFullState, LocationState.class);
    CallstackState callstackState =
        AbstractStates.extractStateByType(pFullState, CallstackState.class);
    ProgramCounter programCounter = new ProgramCounter(locationState, callstackState);
    PathFormula iteration = pState.getPathFormulasForIteration().get(programCounter);
    BooleanFormula path =
        bfmgr.and(
            pState.getPathFormulasForPrefix().orElseThrow().getFormula(), iteration.getFormula());
    BooleanFormula witnessInvariant =
        conjoinWitnessTransitionInvariants(AbstractStates.extractLocation(locationState));
    // The values at the current visit have the last key
    int currentVisit = pState.getNumberOfIterationsAtLoopHead(programCounter) - 1;
    try {
      for (Entry<Integer, ImmutableSet<Formula>> visit :
          pState.getStoredValues().get(programCounter).entrySet()) {
        if (visit.getKey() >= currentVisit) {
          continue;
        }
        Optional<BooleanFormula> invariantAtVisit =
            instantiateBetween(witnessInvariant, visit.getValue(), iteration.getSsa());
        if (invariantAtVisit.isPresent()
            && !solver.isUnsat(bfmgr.and(path, bfmgr.not(invariantAtVisit.orElseThrow())))) {
          return false;
        }
      }
    } catch (SolverException e) {
      logger.logDebugException(e, "Could not check the transition invariant of the witness");
      return false;
    }
    return true;
  }

  /**
   * Instantiates the previous values of the transition invariant with the values at an earlier
   * visit and its other variables with the current values.
   *
   * @return the instantiated formula, or nothing if a variable has no value
   */
  private Optional<BooleanFormula> instantiateBetween(
      BooleanFormula pTransitionInvariant,
      ImmutableSet<Formula> pValuesAtVisit,
      SSAMap pCurrentIndices) {
    ImmutableMap.Builder<String, Formula> valuesAtVisitBuilder = ImmutableMap.builder();
    for (Formula value : pValuesAtVisit) {
      valuesAtVisitBuilder.put(fmgr.uninstantiate(value).toString(), value);
    }
    ImmutableMap<String, Formula> valuesAtVisit = valuesAtVisitBuilder.buildKeepingLast();
    ImmutableMap.Builder<Formula, Formula> substitution = ImmutableMap.builder();
    for (Formula variable : fmgr.extractVariables(pTransitionInvariant).values()) {
      Formula variableName = fmgr.uninstantiate(variable);
      String name = variableName.toString();
      Formula value;
      if (name.contains(TRANS_INV_KEYWORD)) {
        value = valuesAtVisit.get(TransitionInvariantUtils.removeKeyWordAfterTransInv(name));
      } else {
        value =
            pCurrentIndices.containsVariable(name)
                ? fmgr.instantiate(variableName, pCurrentIndices)
                : null;
      }
      if (value == null) {
        return Optional.empty();
      }
      substitution.put(variable, value);
    }
    return Optional.of(fmgr.substitute(pTransitionInvariant, substitution.buildOrThrow()));
  }

  /**
   * Collects the inductive transition invariants as {@link
   * TerminationToReachPrecisionAdjustment#collectInductiveTransitionInvariants} does, and adds the
   * transition invariant from the witness as well, if it is inductive.
   */
  @Override
  protected ImmutableSet.Builder<PartitionedRelationFormula> collectInductiveTransitionInvariants(
      TerminationToReachState terminationState,
      PartitionedRelationFormula iterationFormula,
      CFANode location)
      throws InterruptedException {
    ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionInvariants =
        super.collectInductiveTransitionInvariants(terminationState, iterationFormula, location);

    // Add the predicates from the witness
    PartitionedRelationFormula invariantFromWitness =
        new PartitionedRelationFormula(collectCandidateTransitionInvariants(location), fmgr);
    if (isInductiveTransitionInvariant(invariantFromWitness, iterationFormula, location)) {
      builderTransitionInvariants.add(invariantFromWitness);
    } else {
      logger.logf(
          Level.FINE,
          "The transition invariant of the witness at %s is not inductive, it is checked on the"
              + " path.",
          location);
      isWitnessTransitionInvariantNotInductive = true;
    }
    return builderTransitionInvariants;
  }

  /**
   * Returns no transition invariants. The inductive transition invariants may contain the one from
   * the witness, which is not irreflexive by construction. Without a fix-point, their conjunction
   * did not pass the check for a lasso in the overapproximation, so a state with them could be
   * covered before the unrolling finds a lasso.
   */
  @Override
  protected ImmutableSet<PartitionedRelationFormula> getTransitionInvariantsWithoutFixPoint(
      ImmutableSet<PartitionedRelationFormula> pInductiveTransitionInvariants) {
    return ImmutableSet.of();
  }

  /**
   * The transition invariants from the witness that are inductive are known before computing new
   * transition invariants. If their conjunction excludes a lasso, the fix-point is reached.
   */
  @Override
  protected Optional<PrecisionAdjustmentResult> checkFixPointWithKnownTransitionInvariants(
      ImmutableList<BooleanFormula> sameStateFormulas,
      PartitionedRelationFormula iterationFormula,
      PathFormula prefixPathFormula,
      CFANode location,
      TerminationToReachState terminationState,
      ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionPredicates,
      ImmutableSet.Builder<PartitionedRelationFormula> builderTransitionInvariants,
      PrecisionAdjustmentResult result)
      throws InterruptedException {
    ImmutableSet<PartitionedRelationFormula> knownTransitionInvariants =
        builderTransitionInvariants.build();
    if (knownTransitionInvariants.isEmpty()) {
      return Optional.empty();
    }
    // Each of the transition invariants is inductive, so their conjunction is inductive as well
    PartitionedRelationFormula conjunction =
        new PartitionedRelationFormula(
            bfmgr.and(
                transformedImmutableListCopy(
                    knownTransitionInvariants, PartitionedRelationFormula::getFormula)),
            fmgr);
    try {
      if (findNonterminatingLoop(
              sameStateFormulas,
              true,
              Optional.of(conjunction),
              iterationFormula,
              prefixPathFormula,
              location)
          .isPresent()) {
        return Optional.empty();
      }
    } catch (SolverException e) {
      logger.logDebugException(e);
      return Optional.empty();
    }
    return checkFixPoint(
        true,
        conjunction,
        iterationFormula,
        location,
        terminationState,
        builderTransitionPredicates,
        builderTransitionInvariants,
        result);
  }

  /**
   * Conjoins all transition invariants from the witness at the given location into one formula. The
   * variables of the formula that do not belong to the previous state are renamed to variables of
   * the current state.
   */
  private BooleanFormula collectCandidateTransitionInvariants(CFANode pLocation)
      throws InterruptedException {
    BooleanFormula candidateTransitionInvariant = conjoinWitnessTransitionInvariants(pLocation);
    candidateTransitionInvariant =
        fmgr.substitute(
            candidateTransitionInvariant,
            ImmutableMap.copyOf(
                Maps.asMap(
                    fmgr.extractVariables(candidateTransitionInvariant).values().stream()
                        .filter(variable -> !variable.toString().contains(TRANS_INV_KEYWORD))
                        .collect(ImmutableSet.toImmutableSet()),
                    variable ->
                        fmgr.makeVariable(
                            fmgr.getFormulaType(variable),
                            TransitionInvariantUtils.removeKeyWordAfterTransInv(
                                    fmgr.uninstantiate(variable).toString())
                                + CURR_KEYWORD))));
    return candidateTransitionInvariant;
  }

  /**
   * Conjoins all transition invariants from the witness at the given location into one formula, as
   * they are parsed.
   */
  private BooleanFormula conjoinWitnessTransitionInvariants(CFANode pLocation)
      throws InterruptedException {
    BooleanFormula conjunction = bfmgr.makeTrue();
    for (ExpressionTreeLocationInvariant invariant : candidateInvariants) {
      if (!(invariant instanceof ExpressionTreeLocationTransitionInvariant)) {
        continue;
      }

      if (invariant.getLocation().equals(pLocation)) {
        BooleanFormula invariantFormula;
        try {
          if (invariant.asExpressionTree().equals(ExpressionTrees.getTrue())) {
            invariantFormula = bfmgr.makeTrue();
          } else {
            invariantFormula = invariant.getFormula(fmgr, pthfmgr, pthfmgr.makeEmptyPathFormula());
          }
        } catch (CPATransferException e) {
          logger.logUserException(Level.WARNING, e, "It was not possible to parse the invariant.");
          invariantFormula = bfmgr.makeTrue();
        }
        conjunction = bfmgr.and(conjunction, invariantFormula);
      }
    }
    return conjunction;
  }
}
