/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.FILTER_ABSTRACTION_STATES;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.edgeexclusion.EdgeExclusionPrecision;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@Options(prefix="bmc")
public class BMCAlgorithm implements Algorithm, StatisticsProvider {

  private static final Predicate<AbstractState> IS_STOP_STATE =
    Predicates.compose(new Predicate<AssumptionStorageState>() {
                             @Override
                             public boolean apply(AssumptionStorageState pArg0) {
                               return (pArg0 != null) && pArg0.isStop();
                             }
                           },
                       AbstractStates.toState(AssumptionStorageState.class));

  private static final Function<? super AbstractState, ? extends Iterable<? extends CFAEdge>> ENTERING_EDGES = new Function<AbstractState, Iterable<CFAEdge>>() {

    @Override
    @Nullable
    public Iterable<CFAEdge> apply(@Nullable AbstractState pArg0) {
      CFANode location;
      if (pArg0 == null || (location = extractLocation(pArg0)) == null) {
        return Collections.<CFAEdge>emptyList();
      }
      return CFAUtils.enteringEdges(location);
    }

    };


  private class BMCStatistics implements Statistics {

    private final Timer satCheck = new Timer();
    private final Timer errorPathCreation = new Timer();
    private final Timer assertionsCheck = new Timer();

    private final Timer inductionPreparation = new Timer();
    private final Timer inductionCheck = new Timer();
    private Timer invariantGeneration;
    private int inductionCutPoints = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      if (satCheck.getNumberOfIntervals() > 0) {
        out.println("Time for final sat check:            " + satCheck);
      }
      if (errorPathCreation.getNumberOfIntervals() > 0) {
        out.println("Time for error path creation:        " + errorPathCreation);
      }
      if (assertionsCheck.getNumberOfIntervals() > 0) {
        out.println("Time for bounding assertions check:  " + assertionsCheck);
      }
      if (inductionCheck.getNumberOfIntervals() > 0) {
        out.println("Number of cut points for induction:  " + inductionCutPoints);
        out.println("Time for induction formula creation: " + inductionPreparation);
        if (invariantGeneration.getNumberOfIntervals() > 0) {
          out.println("  Time for invariant generation:     " + invariantGeneration);
        }
        out.println("Time for induction check:            " + inductionCheck);
      }
    }

    @Override
    public String getName() {
      return "BMC algorithm";
    }
  }

  @Option(description = "If BMC did not find a bug, check whether "
      + "the bounding did actually remove parts of the state space "
      + "(this is similar to CBMC's unwinding assertions).")
  private boolean boundingAssertions = true;

  @Option(description="Check reachability of target states after analysis "
      + "(classical BMC). The alternative is to check the reachability "
      + "as soon as the target states are discovered, which is done if "
      + "cpa.predicate.targetStateSatCheck=true.")
  private boolean checkTargetStates = true;

  @Option(description="try using induction to verify programs with loops")
  private boolean induction = false;

  @Option(description="Generate invariants and add them to the induction hypothesis.")
  private boolean useInvariantsForInduction = false;

  @Option(description="dump counterexample formula to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpCounterexampleFormula = Paths.get("ErrorPath.%d.smt2");

  private final BMCStatistics stats = new BMCStatistics();
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;

  private final InvariantGenerator invariantGenerator;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final Solver solver;

  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

  private final ShutdownNotifier shutdownNotifier;

  private final List<? extends AdjustableConditionCPA> conditionCPAs;

  private final Iterable<CFAEdge> ignorableEdges;

  private final BooleanFormulaManagerView bfmgr;

  public BMCAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCfa)
                      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);
    algorithm = pAlgorithm;
    cpa = pCpa;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCfa;

    if (induction && useInvariantsForInduction) {
      invariantGenerator = new CPAInvariantGenerator(pConfig, pLogger, reachedSetFactory, pShutdownNotifier, cfa);
    } else {
      invariantGenerator = new DoNothingInvariantGenerator(reachedSetFactory);
    }
    stats.invariantGeneration = invariantGenerator.getTimeOfExecution();

    PredicateCPA predCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for BMCAlgorithm");
    }
    fmgr = predCpa.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    solver = predCpa.getSolver();
    shutdownNotifier = pShutdownNotifier;
    conditionCPAs = CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class).toList();

    ignorableEdges = induction ? getIgnorableEdges(cfa) : Collections.<CFAEdge>emptySet();
  }

  @Override
  public boolean run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    final ReachedSet reachedSet;
    if (Iterables.isEmpty(ignorableEdges)) {
      reachedSet = pReachedSet;
    } else {
      reachedSet = reachedSetFactory.create();
      ReachedSetUtils.addReachedStatesToOtherReached(pReachedSet, reachedSet);
      for (AbstractState waitingState : pReachedSet.getWaitlist()) {
        Precision precision = pReachedSet.getPrecision(waitingState);
        precision = excludeIgnorableEdges(precision);
        reachedSet.remove(waitingState);
        reachedSet.add(waitingState, precision);
      }
    }


    CFANode initialLocation = extractLocation(reachedSet.getFirstState());

    invariantGenerator.start(initialLocation);

    try {
      logger.log(Level.INFO, "Creating formula for program");
      boolean soundInner;


      try (ProverEnvironment prover = solver.newProverEnvironmentWithModelGeneration();
          KInductionProver kInductionProver = induction ? new KInductionProver() : null) {

        do {
          shutdownNotifier.shutdownIfNecessary();
          soundInner = unroll(reachedSet);
          if (from(reachedSet)
              .skip(1) // first state of reached is always an abstraction state, so skip it
              .transform(toState(PredicateAbstractState.class))
              .anyMatch(FILTER_ABSTRACTION_STATES)) {

            logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
            return soundInner;
          }

          // first check safety
          boolean safe = checkTargetStates(reachedSet, prover);
          logger.log(Level.FINER, "Program is safe?:", safe);

          if (!safe) {
            createErrorPath(reachedSet, prover);
          }

          prover.pop(); // remove program formula from solver stack

          if (!safe) {
            return soundInner;
          }

          // second check soundness
          boolean sound = false;

          // verify soundness, but don't bother if we are unsound anyway or we have found a bug
          if (soundInner && safe) {

            // check bounding assertions
            sound = checkBoundingAssertions(reachedSet, prover);

            // try to prove program safety via induction
            if (induction) {
              sound = sound || kInductionProver.check();
            }
            if (sound) {
              return true;
            }
          }
        }
        while (soundInner && adjustConditions());
      }

      return false;

    } finally {
      invariantGenerator.cancel();
      if (reachedSet != pReachedSet) {
        pReachedSet.clear();
        ReachedSetUtils.addReachedStatesToOtherReached(reachedSet, pReachedSet);
      }
    }
  }

  /**
   * Adjusts the conditions of the CPAs which support the adjusting of
   * conditions.
   *
   * @return {@code true} if all CPAs supporting the feature agreed on
   * adjusting their conditions, {@code false} if one of the CPAs does not
   * support any further adjustment of conditions.
   */
  private boolean adjustConditions() {
    for (AdjustableConditionCPA condCpa : conditionCPAs) {
      if (!condCpa.adjustPrecision()) {
        // this cpa said "do not continue"
        logger.log(Level.INFO, "Terminating because of", condCpa.getClass().getSimpleName());
        return false;
      }
    }
    return !conditionCPAs.isEmpty();
  }

  /**
   * This method tries to find a feasible path to (one of) the target state(s).
   * It does so by asking the solver for a satisfying assignment.
   * @throws InterruptedException
   */
  private void createErrorPath(final ReachedSet pReachedSet, final ProverEnvironment prover) throws CPATransferException, InterruptedException {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARGCPA");
      return;
    }

    stats.errorPathCreation.start();
    try {
      logger.log(Level.INFO, "Error found, creating error path");

      Iterable<ARGState> arg = from(pReachedSet).filter(ARGState.class);

      // get the branchingFormula
      // this formula contains predicates for all branches we took
      // this way we can figure out which branches make a feasible path
      BooleanFormula branchingFormula = pmgr.buildBranchingFormula(arg);

      if (bfmgr.isTrue(branchingFormula)) {
        logger.log(Level.WARNING, "Could not create error path because of missing branching information!");
        return;
      }

      Model model;

      // add formula to solver environment
      prover.push(branchingFormula);
      try {

        // need to ask solver for satisfiability again,
        // otherwise model doesn't contain new predicates
        boolean stillSatisfiable = !prover.isUnsat();

        if (!stillSatisfiable) {
          // should not occur
          logger.log(Level.WARNING, "Could not create error path information because of inconsistent branching information!");
          return;
        }

        try {
          model = prover.getModel();
        } catch (SolverException e) {
          logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
          logger.logDebugException(e);
          return;
        }
      } finally {
        prover.pop(); // remove branchingFormula
      }


      // get precise error path
      Map<Integer, Boolean> branchingInformation = pmgr.getBranchingPredicateValuesFromModel(model);
      ARGState root = (ARGState)pReachedSet.getFirstState();

      ARGPath targetPath;
      try {
        targetPath = ARGUtils.getPathFromBranchingInformation(root, pReachedSet.asCollection(), branchingInformation);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not create error path");
        return;
      }

      // create and store CounterexampleInfo object
      CounterexampleInfo counterexample;

      // replay error path for a more precise satisfying assignment
      PathChecker pathChecker = new PathChecker(logger, pmgr, solver);
      try {
        CounterexampleTraceInfo info = pathChecker.checkPath(targetPath.asEdgesList());

        if (info.isSpurious()) {
          logger.log(Level.WARNING, "Inconsistent replayed error path!");
          counterexample = CounterexampleInfo.feasible(targetPath, model);

        } else {
          counterexample = CounterexampleInfo.feasible(targetPath, info.getModel());

          counterexample.addFurtherInformation(fmgr.dumpFormula(bfmgr.and(info.getCounterExampleFormulas())),
              dumpCounterexampleFormula);
        }

      } catch (CPATransferException e) {
        // path is now suddenly a problem
        logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
        counterexample = CounterexampleInfo.feasible(targetPath, model);
      }

      ((ARGCPA)cpa).addCounterexample(targetPath.getLast().getFirst(), counterexample);

    } finally {
      stats.errorPathCreation.stop();
    }
  }

  /**
   * Checks the reachability of the target states contained in the given
   * reached set by performing a satisfiability check with the given prover.
   *
   * @param pReachedSet the reached set containing the target states.
   * @param prover the prover to be used.
   *
   * @return {@code true} if no target states are reachable, {@code false}
   * otherwise.
   *
   * @throws InterruptedException if the satisfiability check was interrupted.
   */
  private boolean checkTargetStates(final ReachedSet pReachedSet, final ProverEnvironment prover) throws InterruptedException {
    List<AbstractState> targetStates = from(pReachedSet)
                                            .filter(IS_TARGET_STATE)
                                            .toList();

    if (checkTargetStates) {
      logger.log(Level.FINER, "Found", targetStates.size(), "potential target states");

      // create formula
      BooleanFormula program = createFormulaFor(targetStates);

      logger.log(Level.INFO, "Starting satisfiability check...");
      stats.satCheck.start();
      prover.push(program);
      boolean safe = prover.isUnsat();
      // leave program formula on solver stack
      stats.satCheck.stop();

      if (safe) {
        pReachedSet.removeAll(targetStates);
      }
      return safe;

    } else {
      // fast check for trivial cases
      return targetStates.isEmpty();
    }
  }

  /**
   * Checks if the bounded unrolling completely unrolled all reachable loop
   * iterations by performing a satisfiablity check on the formulas encoding
   * the reachability of the states where the bounded model check stopped due
   * to reaching the bound.
   *
   * If this is is the case, then the bounded model check is guaranteed to be
   * sound.
   *
   * @param pReachedSet the reached set containing the frontier of the bounded
   * model check, i.e. where the bounded model check stopped.
   * @param prover the prover to be used to prove that the stop states are
   * unreachable.
   *
   * @return {@code true} if the bounded model check covered all reachable
   * states and was thus sound, {@code false} otherwise.
   *
   * @throws InterruptedException if the satisfiability check is interrupted.
   */
  private boolean checkBoundingAssertions(final ReachedSet pReachedSet, final ProverEnvironment prover) throws InterruptedException {
    FluentIterable<AbstractState> stopStates = from(pReachedSet)
                                                    .filter(IS_STOP_STATE);

    if (boundingAssertions) {
      // create formula for unwinding assertions
      BooleanFormula assertions = createFormulaFor(stopStates);

      logger.log(Level.INFO, "Starting assertions check...");

      stats.assertionsCheck.start();
      prover.push(assertions);
      boolean sound = prover.isUnsat();
      prover.pop();
      stats.assertionsCheck.stop();

      logger.log(Level.FINER, "Soundness after assertion checks:", sound);
      return sound;

    } else {
      // fast check for trivial cases
      return stopStates.isEmpty();
    }
  }

  /**
   * Create a disjunctive formula of all the path formulas in the supplied iterable.
   */
  private BooleanFormula createFormulaFor(Iterable<AbstractState> states) {
    BooleanFormula f = bfmgr.makeBoolean(false);

    for (PredicateAbstractState e : AbstractStates.projectToType(states, PredicateAbstractState.class)) {
      f = bfmgr.or(f, e.getPathFormula().getFormula());
    }

    return f;
  }


  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  /**
   * Instances of this class are used to prove the safety of a program by
   * applying an inductive approach based on k-induction.
   */
  private class KInductionProver implements AutoCloseable {

    private ProverEnvironment prover = null;

    private final Boolean trivialResult;

    private final Iterable<CFAEdge> incomingEdges;

    private final Iterable<CFAEdge> outgoingEdges;

    private final ReachedSet reachedSet;

    private final Loop loop;

    private UnmodifiableReachedSet invariantsReachedSet;

    private BooleanFormula currentInvariants = bfmgr.makeBoolean(true);

    private int stackDepth = 0;

    private Set<CFANode> targetLocations = null;

    /**
     * Creates an instance of the KInductionProver.
     */
    public KInductionProver() {
      List<CFAEdge> incomingEdges = null;
      FluentIterable<CFAEdge> outgoingEdges = null;
      ReachedSet reachedSet = null;
      Loop loop = null;
      if (!cfa.getLoopStructure().isPresent()) {
        logger.log(Level.WARNING, "Could not use induction for proving program safety, loop structure of program could not be determined.");
        trivialResult = false;
      } else {
        Multimap<String, Loop> loops = cfa.getLoopStructure().get();

        // Induction is currently only possible if there is a single loop.
        // This check can be weakened in the future,
        // e.g. it is ok if there is only a single loop on each path.
        if (loops.size() > 1) {
          logger.log(Level.WARNING, "Could not use induction for proving program safety, program has too many loops");
          invariantGenerator.cancel();
          trivialResult = false;
        } else if (loops.isEmpty()) {
          // induction is unnecessary, program has no loops
          invariantGenerator.cancel();
          trivialResult = true;
        } else {
          stats.inductionPreparation.start();

          loop = Iterables.getOnlyElement(loops.values());
          // function edges do not count as incoming/outgoing edges
          incomingEdges = from(loop.getIncomingEdges()).filter(not(instanceOf(CFunctionReturnEdge.class))).toList();
          outgoingEdges = from(loop.getOutgoingEdges())
              .filter(new Predicate<CFAEdge>() {
                @Override
                public boolean apply(CFAEdge pInput) {
                  if (!(pInput instanceof CFunctionCallEdge)) {
                    return true;
                  }
                  CFANode nodeAfterFunction = ((CFunctionCallEdge) pInput).getSummaryEdge().getSuccessor();
                  if (nodeAfterFunction.getNumEnteringEdges() == 0) {
                    // This is a function call without the chance to return
                    // to the node after the function (a non-terminating function).
                    // This is an exception where the edge counts as an outgoing edge.
                    return true;
                  }
                  return false;
                }
              });

          if (incomingEdges.size() > 1) {
            logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many incoming edges", incomingEdges);
            trivialResult = false;
          } else if (loop.getLoopHeads().size() > 1) {
            logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many loop heads");
            trivialResult = false;
          } else {
            trivialResult = null;
          }
          reachedSet = reachedSetFactory.create();
          CFANode loopHead = Iterables.getOnlyElement(loop.getLoopHeads());
          Precision precision = cpa.getInitialPrecision(loopHead);
          if (trivialResult == null) {
            precision = excludeIgnorableEdges(precision);
          }
          reachedSet.add(cpa.getInitialState(loopHead), precision);
          stats.inductionPreparation.stop();
        }
      }
      this.incomingEdges = trivialResult == null ? incomingEdges : null;
      this.outgoingEdges = trivialResult == null ? outgoingEdges : null;
      this.reachedSet = reachedSet;
      this.loop = loop;
    }

    /**
     * Checks if the result of the k-induction check has been determined to
     * be trivial by the constructor.
     *
     * @return {@code true} if the constructor was able to determine a constant
     * result for the k-induction check, {@code false} otherwise.
     */
    private boolean isTrivial() {
      return this.trivialResult != null;
    }

    /**
     * If available, gets the constant result of the k-induction check as
     * determined by the constructor. Do not call this function if there is no
     * such trivial constant result. This can be checked by calling
     * {@link isTrivial}.
     *
     * @return the trivial constant result of the k-induction check.
     */
    private boolean getTrivialResult() {
      Preconditions.checkState(isTrivial(), "The proof is non-trivial.");
      return trivialResult;
    }

    /**
     * Gets the edges incoming into the single loop of the program to be
     * checked. These incoming edges are only available if no trivial constant
     * result for the k-induction check was determined by the constructor, as
     * can be checked by calling {@link isTrivial}.
     *
     * @return the edges incoming into the single loop of the program.
     */
    private Iterable<CFAEdge> getIncomingEdges() {
      Preconditions.checkState(!isTrivial(), "No incoming edges computed, because the proof is trivial.");
      assert incomingEdges != null;
      return incomingEdges;
    }

    /**
     * Gets the edges going out of the single loop of the program to be
     * checked. These outgoing edges are only available if no trivial constant
     * result for the k-induction check was determined by the constructor, as
     * can be checked by calling {@link isTrivial}.
     *
     * @return the edges going out of the single loop of the program.
     */
    private Iterable<CFAEdge> getOutgoingEdges() {
      Preconditions.checkState(!isTrivial(), "No outgoing edges computed, because the proof is trivial.");
      assert outgoingEdges != null;
      return outgoingEdges;
    }

    /**
     * Gets the current reached set describing the loop iterations unrolled for
     * the inductive step. The reached set is only available if no trivial
     * constant result for the k-induction check was determined by the
     * constructor, as can be checked by calling {@link isTrivial}.
     *
     * @return the current reached set describing the loop iterations unrolled
     * for the inductive step.
     */
    private ReachedSet getCurrentReachedSet() {
      Preconditions.checkState(!isTrivial(), "No reached set created, because the proof is trivial.");
      assert reachedSet != null;
      return reachedSet;
    }

    /**
     * Gets the single loop of the program. This loop is only available if no
     * trivial constant result for the k-induction check was determined by the
     * constructor, as can be checked by calling {@link isTrivial}.
     *
     * @return the single loop of the program.
     */
    private Loop getLoop() {
      Preconditions.checkState(!isTrivial(), "No loop computed, because the proof is trivial.");
      assert loop != null;
      return loop;
    }

    /**
     * Checks if the prover is already initialized.
     *
     * @return {@code true} if the prover is initialized, {@code false}
     * otherwise.
     */
    private boolean isProverInitialized() {
      return prover != null;
    }

    /**
     * Gets the prover environment to be used within the KInductionProver.
     *
     * This prover may be preinitialized with additional supporting invariants.
     * The presence of these invariants, including pushing them onto and
     * popping them off of the prover stack, is taken care of automatically.
     *
     * @return the prover environment to be used within the KInductionProver.
     *
     * @throws CPAException if the supporting invariant generation encountered
     * an exception.
     * @throws InterruptedException if the supporting invariant generation is
     * interrupted.
     */
    private ProverEnvironment getProver() throws CPAException, InterruptedException {
      UnmodifiableReachedSet currentInvariantsReachedSet = invariantGenerator.get();
      if (currentInvariantsReachedSet != invariantsReachedSet) {
        CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
        invariantsReachedSet = currentInvariantsReachedSet;
        // get global invariants
        BooleanFormula invariants = getCurrentInvariants();
        injectInvariants(currentInvariantsReachedSet, loopHead);
        if (isProverInitialized()) {
          pop();
        } else {
          prover = solver.newProverEnvironmentWithModelGeneration();
        }
        invariants = fmgr.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));
        push(invariants);
      }
      assert isProverInitialized();
      return prover;
    }

    /**
     * Gets the most current invariants generated by the invariant generator.
     *
     * @return the most current invariants generated by the invariant generator.
     *
     * @throws CPAException if the invariant generation encountered an exception.
     * @throws InterruptedException if the invariant generation is interrupted.
     */
    private BooleanFormula getCurrentInvariants() throws CPAException, InterruptedException {
      if (!bfmgr.isFalse(currentInvariants)) {
        UnmodifiableReachedSet currentInvariantsReachedSet = invariantGenerator.get();
        if (currentInvariantsReachedSet != invariantsReachedSet) {
          CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
          currentInvariants = extractInvariantsAt(currentInvariantsReachedSet, loopHead);
        }
      }
      return currentInvariants;
    }

    /**
     * Attempts to inject the generated invariants into the bounded analysis
     * CPAs to improve their performance.
     *
     * Currently, this is only supported for the InvariantsCPA. If the
     * InvariantsCPA is not activated for both the bounded analysis as well as
     * the invariant generation, this function does nothing.
     *
     * @param pReachedSet the invariant generation reached set.
     * @param pLocation the location for which to extract and re-inject the
     * invariants.
     */
    private void injectInvariants(UnmodifiableReachedSet pReachedSet, CFANode pLocation) {
      InvariantsCPA invariantsCPA = CPAs.retrieveCPA(cpa, InvariantsCPA.class);
      if (invariantsCPA == null) {
        return;
      }
      InvariantsState invariant = null;
      for (AbstractState locState : AbstractStates.filterLocation(pReachedSet, pLocation)) {
        InvariantsState disjunctivePart = AbstractStates.extractStateByType(locState, InvariantsState.class);
        if (disjunctivePart != null) {
          if (invariant == null) {
            invariant = disjunctivePart;
          } else {
            invariant = invariant.join(disjunctivePart, true);
          }
        } else {
          return;
        }
      }
      if (invariant != null) {
        invariantsCPA.injectInvariant(pLocation, invariant);
      }
    }

    @Override
    public void close() {
      if (isProverInitialized()) {
        while (stackDepth-- > 0) {
          prover.pop();
        }
        prover.close();
      }
    }

    /**
     * Pops the last formula from the prover stack.
     */
    private void pop() {
      Preconditions.checkState(isProverInitialized());
      Preconditions.checkState(stackDepth > 0);
      prover.pop();
      --stackDepth;
    }

    /**
     * Pushes the given formula to the prover stack.
     *
     * @param pFormula the formula to be pushed.
     */
    private void push(BooleanFormula pFormula) {
      Preconditions.checkState(isProverInitialized());
      prover.push(pFormula);
      ++stackDepth;
    }

    /**
     * Extracts the generated invariants for the given location from the
     * given reached set produced by the invariant generator.
     *
     * @param pReachedSet the reached set produced by the invariant generator.
     * @param pLocation the location to extract the invariants for.
     *
     * @return the extracted invariants as a boolean formula.
     */
    private BooleanFormula extractInvariantsAt(UnmodifiableReachedSet pReachedSet, CFANode pLocation) {

      if (pReachedSet.isEmpty()) {
        return bfmgr.makeBoolean(true); // no invariants available
      }

      // Check if the invariant generation was able to prove correctness for the program
      if (targetLocations != null && AbstractStates.filterLocations(pReachedSet, targetLocations).isEmpty()) {
        logger.log(Level.INFO, "Invariant generation found no target states.");
        invariantGenerator.cancel();
        return bfmgr.makeBoolean(false);
      }

      BooleanFormula invariant = bfmgr.makeBoolean(false);

      for (AbstractState locState : AbstractStates.filterLocation(pReachedSet, pLocation)) {
        BooleanFormula f = AbstractStates.extractReportedFormulas(fmgr, locState);
        logger.log(Level.ALL, "Invariant:", f);

        invariant = bfmgr.or(invariant, f);
      }
      return invariant;
    }

    /**
     * Attempts to perform the inductive check.
     *
     * @return <code>true</code> if k-induction successfully proved the
     * correctness, <code>false</code> if the attempt was inconclusive.
     *
     * @throws CPAException if the bounded analysis constructing the step case
     * encountered an exception.
     * @throws InterruptedException if the bounded analysis constructing the
     * step case was interrupted.
     */
    public final boolean check() throws CPAException, InterruptedException {
      // Early return if there is a trivial result for the inductive approach
      if (isTrivial()) {
        return getTrivialResult();
      }

      // Early return if the invariant generation proved the program correct
      if (bfmgr.isFalse(getCurrentInvariants())) {
        return true;
      }

      stats.inductionPreparation.start();
      final Loop loop = getLoop();

      final CFANode loopHead = Iterables.getOnlyElement(loop.getLoopHeads());

      // check that the loop head is unambiguous
      assert loopHead.equals(Iterables.getOnlyElement(getIncomingEdges()).getSuccessor());


      // Proving program safety with induction consists of two parts:
      // 1) Prove all paths safe that go only one iteration through the loop.
      //    This is part of the classic bounded model checking done above,
      //    so we don't care about this here.
      // 2) Assume that one loop iteration is safe and prove that the next one is safe, too.

      // Suppose that the loop has a single outgoing edge,
      // which leads to the error location. This edge is always an CAssumeEdge,
      // and it has a "sibling" which is an inner edge of the loop and leads to
      // the next iteration. We call the latter the continuation edge.
      // The common predecessor node of these two edges will be called cut point.
      // Now we want to show that the control flow of the program will never take
      // the outgoing edge and reach the error location,
      // if it didn't take it in the iteration before.
      // We create three formulas:
      // A is the assumption from the continuation edge in the previous iteration
      // B is the formula for the loop body in the current iteration up to the cut point
      // C is the negation of the formula for the path from the cut point to the error location
      // Then we try to prove that the formula (A & B) => C holds.
      // This implies that control flow cannot reach the error location.

      // The conjunction (A & B) is created by running the CPAAlgorithm starting
      // at the cut point and letting it run until the end of the current iteration
      // (i.e. let it finish the iteration it starts in and complete one more iteration).
      // Then we get the abstract state at the cut point in the last iteration
      // and take its path formula, which is exactly (A & B).
      // C is created by letting the CPAAlgorithm run starting at the cut point,
      // taking the outgoing edge (and not staying within the loop).
      // Then we can take the disjunction of the path formulas at all reached
      // target states. Two things are important here:
      // CPAAlgorithm needs to take only the path out of the loop,
      // and the path formula needs to be created with the SSAMap from (A & B)
      // in order to get the indices right.

      // Everything above is easily extended to k-induction with k >= 1
      // and to loops that have several outgoing edges (and therefore several
      // cut points).
      // For k-induction, just let the algorithm run a few iterations. Of course
      // the formula for the induction basis needs to contain the same number of
      // iterations. This is ensured because we use the same algorithm and the
      // same CPAs to create the formulas in both cases, so they'll run the same
      // number of iterations in both cases.
      // For several exiting edges, we add each cut-point to the initial reached
      // set, so that A will contain the assumptions from all continuation edges,
      // and we'll create several (A & B) and C formulas, one for each cut point.


      // Create initial reached set:
      // Run algorithm in order to create formula (A & B)
      logger.log(Level.INFO, "Running algorithm to create induction hypothesis");
      ReachedSet reached = getCurrentReachedSet();
      unroll(reached);

      final Multimap<CFANode, AbstractState> reachedPerLocation = Multimaps.index(reached, EXTRACT_LOCATION);

      // live view of reached set with only the states in the loop
      FluentIterable<AbstractState> loopStates = from(reached).filter(new Predicate<AbstractState>() {
        @Override
        public boolean apply(AbstractState pArg0) {
          LoopstackState loopState = extractStateByType(pArg0, LoopstackState.class);
          return loop.equals(loopState.getLoop());
        }
      });

      assert !loopStates.isEmpty();

      // Create formula
      BooleanFormula inductions = bfmgr.makeBoolean(true);

      targetLocations = from(reached).filter(IS_TARGET_STATE).transform(AbstractStates.EXTRACT_LOCATION).toSet();

      final Iterable<CFAEdge> cutPointEdges = getCutPointEdges(reachedPerLocation, loopStates);

      int inductionCutPoints = 0;

      for (CFAEdge cutPointEdge : cutPointEdges) {

        inductionCutPoints++;
        logger.log(Level.FINEST, "Considering cut point edge", cutPointEdge);

        CFANode cutPoint = cutPointEdge.getPredecessor();
        Iterable<AbstractState> cutPointStates = reachedPerLocation.get(cutPoint);

        // Create (A & B)
        PathFormula pathFormulaAB = extractLastIterationPath(cutPointStates);
        BooleanFormula formulaAB = pathFormulaAB.getFormula();

        BooleanFormula formulaC;
        { // Create C
          // We want to continue exploration from lastCutPointState,
          // but with an empty path formula. However, the SSAMap needs to be the one
          // from lastCutPointState, so we can use a fresh initial state.
          PathFormula empty = pmgr.makeEmptyPathFormula(pathFormulaAB); // empty has correct SSAMap
          AbstractState freshCutPointState = cpa.getInitialState(cutPoint);

          // Modify the path formula but save the previous one to restore it later
          PredicateAbstractState predicateAbstractState = extractStateByType(freshCutPointState, PredicateAbstractState.class);
          PathFormula savedPathFormula = predicateAbstractState.getPathFormula();
          predicateAbstractState.setPathFormula(empty);

          /*
           * Ensure that only the path leaving the loop is left by preparing
           * the precision to exclude all other edges leaving the cut point
           */
          Precision freshCutPointPrecision = cpa.getInitialPrecision(cutPoint);
          freshCutPointPrecision = excludeIgnorableEdges(freshCutPointPrecision);
          freshCutPointPrecision = excludeLoopEdges(freshCutPointPrecision, cutPointEdge);

          // Create path formulas by running CPAAlgorithm
          reached = reachedSetFactory.create();
          reached.add(freshCutPointState, freshCutPointPrecision);
          algorithm.run(reached);

          Iterable<AbstractState> targetStates = from(reached).filter(IS_TARGET_STATE);
          formulaC = bfmgr.not(createFormulaFor(targetStates));
          reached.clear();

          // Reset predicate path formula
          predicateAbstractState.setPathFormula(savedPathFormula);
        }

        // Create (A & B) => C
        BooleanFormula f = bfmgr.or(bfmgr.not(formulaAB), formulaC);

        inductions = bfmgr.and(inductions, f);
      }

      stats.inductionCutPoints = inductionCutPoints;

      // now prove that (A & B) => C is a tautology by checking if the negation is unsatisfiable

      inductions = bfmgr.not(inductions);

      ProverEnvironment prover = getProver();

      stats.inductionPreparation.stop();

      logger.log(Level.INFO, "Starting induction check...");

      stats.inductionCheck.start();
      push(inductions);
      boolean sound = prover.isUnsat();

      if (!sound && logger.wouldBeLogged(Level.ALL)) {
        logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
      }
      pop();
      stats.inductionCheck.stop();

      logger.log(Level.FINER, "Soundness after induction check:", sound);
      return sound;
    }

    /**
     * Ensures that only the path leaving the loop is analyzed by preparing the
     * precision to exclude all other edges leaving the cut point.
     *
     * @param pFreshCutPointPrecision the current precision at the cut point.
     * @param pCutPointEdge the edge leaving the loop at the cut point.
     *
     * @return the new precision at the cut point.
     *
     * @throws CPAException if the EdgeExclusionCPA is not present.
     */
    private Precision excludeLoopEdges(Precision pFreshCutPointPrecision, CFAEdge pCutPointEdge) throws CPAException {
      List<CFAEdge> loopEdges = CFAUtils.leavingEdges(pCutPointEdge.getPredecessor())
          .filter(not(equalTo(pCutPointEdge))).toList();

      if (loopEdges.isEmpty()) {
        return pFreshCutPointPrecision;
      }

      EdgeExclusionPrecision oldPrecision = Precisions.extractPrecisionByType(pFreshCutPointPrecision, EdgeExclusionPrecision.class);
      if (oldPrecision == null) {
        throw new CPAException("Bounded model checking with induction requires an instance of the EdgeExclusionCPA. Please restart with the EdgeExclusionCPA.");
      }
      EdgeExclusionPrecision newPrecision = oldPrecision.excludeMoreEdges(loopEdges);
      if (newPrecision == oldPrecision) {
        return pFreshCutPointPrecision;
      }
      pFreshCutPointPrecision = Precisions.replaceByType(pFreshCutPointPrecision, newPrecision, EdgeExclusionPrecision.class);
      return pFreshCutPointPrecision;
    }

    /**
     * Extracts the path formula to the cut point states representing the last iteration.
     *
     * @param cutPointStates the cut point states.
     * @return the path formula to the cut point states representing the last iteration.
     *
     * @throws CPAException if no loopstack information is present.
     */
    private PathFormula extractLastIterationPath(Iterable<AbstractState> cutPointStates) throws CPAException, InterruptedException {
      int highestIteration = -1;
      PathFormula pathFormula = null;
      for (AbstractState cutPointState : cutPointStates) {
        LoopstackState loopstackState = extractStateByType(cutPointState, LoopstackState.class);
        if (loopstackState == null) {
          throw new CPAException("BMC without LoopstackCPA is not supported. Please rerun with an instance of the LoopstackCPA.");
        }
        if (!loopstackState.mustDumpAssumptionForAvoidance()) {
          int iteration = loopstackState.getIteration();
          if (iteration > highestIteration || pathFormula == null) {
            highestIteration = iteration;
            pathFormula = extractStateByType(cutPointState, PredicateAbstractState.class).getPathFormula();
          } else if (iteration == highestIteration) {
            assert pathFormula != null;
            pathFormula = pmgr.makeOr(pathFormula, extractStateByType(cutPointState, PredicateAbstractState.class).getPathFormula());
          }
        }
      }
      Preconditions.checkArgument(pathFormula != null, "cutPointStates must not be empty.");
      return pathFormula;
    }

    /**
     * Consider all edges that lead from the loop to a target location. This is
     * the combination of all edges leaving the loop leading to a target
     * location and all edges leading to target locations within the loop, but
     * not any target locations preceding the loop.
     *
     * @param pReachedPerLocation the reached set mapped to the reached locations.
     * @param pLoopStates the loop states.
     * @return all edges that lead from the loop to a target location.
     */
    private Iterable<CFAEdge> getCutPointEdges(final Multimap<CFANode, AbstractState> pReachedPerLocation,
        Iterable<AbstractState> pLoopStates) {
      Iterable<CFAEdge> relevantOutgoingEdges = FluentIterable.from(getOutgoingEdges()).filter(new Predicate<CFAEdge>() {

        @Override
        public boolean apply(@Nullable CFAEdge pEdge) {
          if (pEdge == null) {
            return false;
          }
          // filter out exit edges that do not lead to a target state, we don't care about them
          CFANode exitLocation = pEdge.getSuccessor();
          Collection<AbstractState> exitStates = pReachedPerLocation.get(exitLocation);
          if (exitStates.isEmpty()) {
            return false;
          }
          ARGState lastExitState = (ARGState) Iterables.getLast(exitStates);

          // the states reachable from the exit edge
          Set<ARGState> outOfLoopStates = lastExitState.getSubgraph();
          if (!from(outOfLoopStates).anyMatch(IS_TARGET_STATE)) {
            return false;
          }
          return true;
        }

      });
      Iterable<CFAEdge> relevantInsideEdges = FluentIterable.from(pLoopStates).filter(IS_TARGET_STATE).transformAndConcat(ENTERING_EDGES).toSet();
      return Iterables.concat(relevantOutgoingEdges, relevantInsideEdges);
    }

  }

  /**
   * Unrolls the given reached set using the algorithm provided to this
   * instance of the bounded model checking algorithm.
   *
   * @param pReachedSet the reached set to unroll.
   *
   * @return {@code true} if the unrolling was sound, {@code false} otherwise.
   *
   * @throws CPAException if an exception occurred during unrolling the reached
   * set.
   * @throws InterruptedException if the unrolling is interrupted.
   */
  private boolean unroll(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    return unroll(pReachedSet, Collections.<CFAEdge>emptySet());
  }

  /**
   * Unrolls the given reached set using the algorithm provided to this
   * instance of the bounded model checking algorithm.
   *
   * @param pReachedSet the reached set to unroll.
   * @param pExcludedEdges edges that are excluded in the current edge
   * exclusion precision and should stay excluded.
   *
   * @return {@code true} if the unrolling was sound, {@code false} otherwise.
   *
   * @throws CPAException
   * @throws InterruptedException
   */
  private boolean unroll(ReachedSet pReachedSet, Iterable<CFAEdge> pExcludedEdges) throws CPAException, InterruptedException {
    adjustReachedSet(pReachedSet, pExcludedEdges);
    return algorithm.run(pReachedSet);
  }

  /**
   * Adjusts the given reached set so that the involved adjustable condition
   * CPAs are able to operate properly without being negatively influenced by
   * states generated earlier under different conditions while trying to
   * retain as many states as possible.
   *
   * @param pReachedSet the reached set to be adjusted.
   * @param pExcludedEdges the edges that were excluded and should also be
   * excluded in the future in case the reached set needs to be cleared and
   * reinitialized.
   */
  private void adjustReachedSet(ReachedSet pReachedSet, Iterable<CFAEdge> pExcludedEdges) {
    Preconditions.checkArgument(!pReachedSet.isEmpty());
    CFANode initialLocation = extractLocation(pReachedSet.getFirstState());
    for (AdjustableConditionCPA conditionCPA : conditionCPAs) {
      if (conditionCPA instanceof ReachedSetAdjustingCPA) {
        ((ReachedSetAdjustingCPA) conditionCPA).adjustReachedSet(pReachedSet);
      } else {
        pReachedSet.clear();
        logger.log(Level.WARNING, "Completely clearing the reached set after condition adjustment due to " + conditionCPA.getClass()
            + ". This may drastically impede the efficiency of iterative deepening. Implement ReachedSetAdjustingCPA to avoid this problem.");
        break;
      }
    }
    if (pReachedSet.isEmpty()) {
      Precision precision = cpa.getInitialPrecision(initialLocation);
      precision = excludeIgnorableEdges(precision);
      precision = excludeEdges(precision, pExcludedEdges);
      pReachedSet.add(cpa.getInitialState(initialLocation), precision);
    }
  }

  /**
   * Excludes the collected ignorable edges from the given precision.
   *
   * @param pPrecision the precision to exclude the edges from.
   * @return the new precision.
   */
  private Precision excludeIgnorableEdges(Precision pPrecision) {
    return excludeEdges(pPrecision, ignorableEdges);
  }

  /**
   * Excludes the given edges from the given precision if the EdgeExclusionCPA
   * is activated to allow for such edge exclusions.
   *
   * @param pPrecision the precision to exclude the edges from.
   * @param pEdgesToIgnore the edges to be excluded.
   * @return the new precision.
   */
  private Precision excludeEdges(Precision pPrecision, Iterable<CFAEdge> pEdgesToIgnore) {
    EdgeExclusionPrecision oldPrecision = Precisions.extractPrecisionByType(pPrecision, EdgeExclusionPrecision.class);
    if (oldPrecision != null) {
      EdgeExclusionPrecision newPrecision = oldPrecision.excludeMoreEdges(pEdgesToIgnore);
      return Precisions.replaceByType(pPrecision, newPrecision, EdgeExclusionPrecision.class);
    }
    return pPrecision;
  }

  /**
   * Consider a variable v assigned at a location l within a single loop L.
   * If the next occurrence of v is at an assume edge e and all paths starting
   * at e either modify no variables but v before looping back to l or leave
   * the loop L without ever again referring to v, then the induction algorithm
   * may treat the edge e as non-existent.
   *
   * Reason: The paths starting at e do not change the safety property of the
   * loop.
   *
   * Advantage: This optimization makes induction possible for loops with a
   * non-deterministically loop-assigned switch variables where the default
   * case does not contain any logic. Such code is often generated by driver
   * environments.
   *
   * @param pCFA the control flow automaton.
   *
   * @return the control flow edges ignorable for induction according to the
   * reasoning described above.
   */
  private static Iterable<CFAEdge> getIgnorableEdges(CFA pCFA) {
    // Check if the required preconditions are met
    if (!pCFA.getVarClassification().isPresent()
        || !pCFA.getLoopStructure().isPresent()) {
      return Collections.emptySet();
    }
    ImmutableMultimap<String, Loop> loopStructure = pCFA.getLoopStructure().get();
    if (loopStructure.isEmpty() || loopStructure.values().size() > 2) {
      return Collections.emptySet();
    }
    Loop loop = Iterables.getOnlyElement(loopStructure.values());
    if (loop.getLoopHeads().size() != 1) {
      return Collections.emptySet();
    }

    CFANode loopHead = Iterables.getOnlyElement(loop.getLoopHeads());
    Set<CFANode> loopNodes = loop.getLoopNodes();
    VariableClassification variableClassification = pCFA.getVarClassification().get();

    // Compute all potential assignment edges within the loop
    Deque<CFANode> waitlist = new ArrayDeque<>();
    Set<CFANode> visited = new HashSet<>();
    waitlist.offer(loopHead);
    Set<CFAEdge> potentialAssignmentEdges = new HashSet<>();
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (visited.add(current)) {
        for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(current)) {
          if (loopNodes.contains(leavingEdge.getSuccessor())) {
            if (leavingEdge.getEdgeType() == CFAEdgeType.DeclarationEdge
                || leavingEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
              potentialAssignmentEdges.add(leavingEdge);
            }
            waitlist.offer(leavingEdge.getSuccessor());
          }
        }
      }
    }
    waitlist.clear();
    visited.clear();

    // Extract all candidate assignments
    Map<CFAEdge, String> candidateAssignments = new HashMap<>();
    for (CFAEdge edge : potentialAssignmentEdges) {
      if (edge instanceof AStatementEdge) {
        IAStatement statement = ((AStatementEdge) edge).getStatement();
        final IALeftHandSide leftHandSide;
        if (statement instanceof AExpressionAssignmentStatement) {
          AExpressionAssignmentStatement assignmentStatement = (AExpressionAssignmentStatement) statement;
          leftHandSide = assignmentStatement.getLeftHandSide();
        } else if (statement instanceof AFunctionCallAssignmentStatement) {
          AFunctionCallAssignmentStatement assignmentStatement = (AFunctionCallAssignmentStatement) statement;
          leftHandSide = assignmentStatement.getLeftHandSide();
        } else {
          leftHandSide = null;
        }
        if (leftHandSide instanceof AIdExpression) {
          String variableName = ((AIdExpression) leftHandSide).getDeclaration().getQualifiedName();
          if (!variableClassification.getAddressedVariables().contains(variableName)) {
            candidateAssignments.put(edge, variableName);
          }
        }
      }
    }

    // Filter for all edges that actually may be ignored for induction
    Set<CFAEdge> ignorableEdges = new HashSet<>();

    for (Map.Entry<CFAEdge, String> entry : candidateAssignments.entrySet()) {
      assert waitlist.isEmpty();
      assert visited.isEmpty();

      CFAEdge candidateAssignmentEdge = entry.getKey();
      String variable = entry.getValue();

      waitlist.offer(candidateAssignmentEdge.getSuccessor());

      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        if (visited.add(current)) {
          for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current)) {
            CFANode successor = leavingEdge.getSuccessor();
            if (loopNodes.contains(successor)) {
              boolean variableIsInvolved = getInvolvedVariables(leavingEdge, variableClassification).contains(variable);
              boolean isAssumeEdge = leavingEdge.getEdgeType() == CFAEdgeType.AssumeEdge;
              if (!variableIsInvolved || isAssumeEdge) {
                waitlist.add(successor);
              }
              if (variableIsInvolved && isAssumeEdge && isIgnorable(leavingEdge, candidateAssignmentEdge, ignorableEdges, variableClassification, loop, variable)) {
                ignorableEdges.add(leavingEdge);
              }
            }
          }
        }
      }

      waitlist.clear();
      visited.clear();
    }

    return ignorableEdges;
  }

  /**
   * Checks if the given assume edge succeeding the given assignment edge is
   * ignorable. See {@link getIgnorableEdges} for details on why such an edge
   * may be deemed ignorable.
   *
   * This is a helper function only meant to be called by
   * {@link getIgnorableEdges}.
   *
   * @param pAssumeEdge the assume edge containing an assumption about the
   * given variable {@code pVariable}.
   * @param pAssignmentEdge the assignment edge preceding the assume edge
   * {@code pAssumeEdge} and assigning a value to the variable
   * {@code pVariable}.
   * @param pIgnorableEdges the edges already found to be ignorable. This set
   * is not modified by this function.
   * @param pVariableClassification the variable classification information
   * about the control flow automaton.
   * @param pLoop the loop containing the given edges.
   * @param pVariable the variable assigned to by the assignment edge
   * {@code pAssignmentEdge}.
   *
   * @return {@code true} if the edge may be ignored by k-induction,
   * {@code false} if it should not be ignored.
   */
  private static boolean isIgnorable(CFAEdge pAssumeEdge, CFAEdge pAssignmentEdge, Set<CFAEdge> pIgnorableEdges,
      VariableClassification pVariableClassification, Loop pLoop, String pVariable) {
    Preconditions.checkArgument(pLoop.getLoopHeads().size() == 1);
    if (pIgnorableEdges.contains(pAssumeEdge)) {
      return true;
    }

    CFANode loopHead = Iterables.getOnlyElement(pLoop.getLoopHeads());

    Deque<CFANode> waitlist = new ArrayDeque<>();
    Deque<Boolean> loopHeadReachedWaitlist = new ArrayDeque<>();
    Set<CFANode> visited = new HashSet<>();
    waitlist.offer(pAssumeEdge.getSuccessor());
    loopHeadReachedWaitlist.offer(false);

    boolean assignmentReached = false;
    boolean assumptionReached = false;

    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      boolean loopHeadReached = loopHeadReachedWaitlist.poll();
      if (visited.add(current)) {
        for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current)) {
          CFANode successor = leavingEdge.getSuccessor();
          boolean loopHeadReachedLocal = loopHeadReached || current.equals(loopHead);
          if (current.equals(pAssignmentEdge.getPredecessor())) {
            assignmentReached = true;
          } else if (current.equals(pAssumeEdge.getPredecessor())) {
            assumptionReached = true;
          } else {
            boolean isInLoop = pLoop.getLoopNodes().contains(successor);
            boolean isBeforeLoopHead = isInLoop && !loopHeadReachedLocal;
            Iterable<String> involvedVariables = getInvolvedVariables(leavingEdge, pVariableClassification);
            involvedVariables = from(involvedVariables).filter(not(in(pVariableClassification.getIrrelevantVariables())));
            if (isBeforeLoopHead && !isFreeOfSideEffects(leavingEdge)
                || !isBeforeLoopHead && (Iterables.contains(involvedVariables, pVariable) && !(Iterables.all(involvedVariables, equalTo(pVariable)) && leavingEdge.getEdgeType() == CFAEdgeType.DeclarationEdge))) {
              return false;
            }
          }
          if (!assignmentReached || !assumptionReached) {
            waitlist.add(successor);
            loopHeadReachedWaitlist.offer(loopHeadReachedLocal);
          }
        }
        if (current.getNumLeavingEdges() == 0 && !loopHeadReached) {
          return false;
        }
      }
    }

    return assignmentReached && assumptionReached;
  }

  /**
   * Checks if the given CFA edge is free of side effects.
   *
   * @param pEdge the edge to be checked.
   *
   * @return {@code true} if the edge is considered to be free of side effects,
   * {@code false} if it might cause side effects.
   */
  private static boolean isFreeOfSideEffects(CFAEdge pEdge) {
    if (pEdge == null
        || pEdge.getEdgeType() != CFAEdgeType.StatementEdge
        && pEdge.getEdgeType() != CFAEdgeType.DeclarationEdge
        && pEdge.getEdgeType() != CFAEdgeType.MultiEdge) {
      return true;
    }
    if (pEdge instanceof AStatementEdge) {
      IAStatement statement = ((AStatementEdge) pEdge).getStatement();
      if (statement instanceof AExpressionAssignmentStatement
          || statement instanceof AFunctionCallAssignmentStatement) {
        return false;
      }
      return true;
    }
    if (pEdge instanceof MultiEdge) {
      for (CFAEdge edge : (MultiEdge) pEdge)  {
        if (!isFreeOfSideEffects(edge)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Gets the variables involved in the given edge.
   *
   * @param pCfaEdge the edge to be analyzed.
   * @param pVariableClassification the variable classification.
   *
   * @return the variables involved in the given edge.
   */
  private static Set<String> getInvolvedVariables(CFAEdge pCfaEdge, VariableClassification pVariableClassification) {
    switch (pCfaEdge.getEdgeType()) {
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) pCfaEdge;
      IAExpression expression = assumeEdge.getExpression();
      return getInvolvedVariables(expression, pVariableClassification);
    }
    case MultiEdge: {
      MultiEdge multiEdge = (MultiEdge) pCfaEdge;
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (CFAEdge edge : multiEdge) {
        builder.addAll(getInvolvedVariables(edge, pVariableClassification));
      }
      return builder.build();
    }
    case DeclarationEdge: {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pCfaEdge;
      IADeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof AVariableDeclaration) {
        AVariableDeclaration variableDeclaration = (AVariableDeclaration) declaration;
        String declaredVariable = variableDeclaration.getQualifiedName();
        IAInitializer initializer = variableDeclaration.getInitializer();
        if (initializer == null) {
          return Collections.singleton(declaredVariable);
        }
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add(declaredVariable);
        if (initializer instanceof AInitializerExpression) {
          builder.addAll(getInvolvedVariables(((AInitializerExpression) initializer).getExpression(), pVariableClassification));
        } else if (initializer instanceof CInitializer) {
          builder.addAll(getInvolvedVariables((CInitializer) initializer, pVariableClassification));
        } else if (initializer instanceof JArrayInitializer) {
          for (IAExpression expression : ((JArrayInitializer) initializer).getInitializerExpressions()) {
            builder.addAll(getInvolvedVariables(expression, pVariableClassification));
          }
        }
        return builder.build();
      } else {
        return Collections.emptySet();
      }
    }
    case FunctionCallEdge: {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) pCfaEdge;
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (IAExpression argument : functionCallEdge.getArguments()) {
        builder.addAll(getInvolvedVariables(argument, pVariableClassification));
      }
      for (IAExpression parameter : functionCallEdge.getSummaryEdge().getExpression().getFunctionCallExpression().getParameterExpressions()) {
        builder.addAll(getInvolvedVariables(parameter, pVariableClassification));
      }
      return builder.build();
    }
    case ReturnStatementEdge: {
      AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) pCfaEdge;
      return getInvolvedVariables(returnStatementEdge.getExpression(), pVariableClassification);
    }
    case StatementEdge: {
      AStatementEdge statementEdge = (AStatementEdge) pCfaEdge;
      IAStatement statement = statementEdge.getStatement();
      if (statement instanceof AExpressionAssignmentStatement) {
        AExpressionAssignmentStatement expressionAssignmentStatement = (AExpressionAssignmentStatement) statement;
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.addAll(getInvolvedVariables(expressionAssignmentStatement.getLeftHandSide(), pVariableClassification));
        builder.addAll(getInvolvedVariables(expressionAssignmentStatement.getRightHandSide(), pVariableClassification));
        return builder.build();
      } else if (statement instanceof AExpressionStatement) {
        return getInvolvedVariables(((AExpressionStatement) statement).getExpression(), pVariableClassification);
      } else if (statement instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement functionCallAssignmentStatement = (AFunctionCallAssignmentStatement) statement;
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.addAll(getInvolvedVariables(functionCallAssignmentStatement.getLeftHandSide(), pVariableClassification));
        for (IAExpression expression : functionCallAssignmentStatement.getFunctionCallExpression().getParameterExpressions()) {
          builder.addAll(getInvolvedVariables(expression, pVariableClassification));
        }
        return builder.build();
      } else if (statement instanceof AFunctionCallStatement) {
        AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (IAExpression expression : functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
          builder.addAll(getInvolvedVariables(expression, pVariableClassification));
        }
        return builder.build();
      } else {
        return Collections.emptySet();
      }
    }
    case BlankEdge:
    case CallToReturnEdge:
    case FunctionReturnEdge:
    default:
      return Collections.emptySet();
    }
  }

  /**
   * Gets the variables involved in the given CInitializer.
   *
   * @param pCInitializer the CInitializer to be analyzed.
   * @param pVariableClassification the variable classification.
   *
   * @return the variables involved in the given CInitializer.
   */
  private static Set<String> getInvolvedVariables(CInitializer pCInitializer, VariableClassification pVariableClassification) {
    if (pCInitializer instanceof CDesignatedInitializer) {
      return getInvolvedVariables(((CDesignatedInitializer) pCInitializer).getRightHandSide(), pVariableClassification);
    } else if (pCInitializer instanceof CInitializerExpression) {
      return getInvolvedVariables(((CInitializerExpression) pCInitializer).getExpression(), pVariableClassification);
    } else if (pCInitializer instanceof CInitializerList) {
      CInitializerList initializerList = (CInitializerList) pCInitializer;
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (CInitializer initializer : initializerList.getInitializers()) {
        builder.addAll(getInvolvedVariables(initializer, pVariableClassification));
      }
      return builder.build();
    }
    return Collections.emptySet();
  }

  /**
   * Gets the variables involved in the given expression.
   *
   * @param pExpression the expression to be analyzed.
   * @param pVariableClassification the variable classification.
   *
   * @return the variables involved in the given expression.
   */
  private static Set<String> getInvolvedVariables(IAExpression pExpression, VariableClassification pVariableClassification) {
    if (pExpression == null) {
      return Collections.emptySet();
    } if (pExpression instanceof CExpression) {
      return pVariableClassification.getVariablesOfExpression((CExpression) pExpression);
    } else {
      throw new UnsupportedOperationException("VariableClassification only supports C expressions");
    }
  }

}
