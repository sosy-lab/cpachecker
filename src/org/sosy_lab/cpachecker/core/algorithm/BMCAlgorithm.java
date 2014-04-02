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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
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
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
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

  private BooleanFormulaManagerView bfmgr;

  public BMCAlgorithm(Algorithm algorithm, ConfigurableProgramAnalysis pCpa,
                      Configuration config, LogManager logger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCfa)
                      throws InvalidConfigurationException, CPAException {
    config.inject(this);
    this.algorithm = algorithm;
    this.cpa = pCpa;
    this.logger = logger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCfa;

    if (induction && useInvariantsForInduction) {
      invariantGenerator = new CPAInvariantGenerator(config, logger, reachedSetFactory, pShutdownNotifier, cfa);
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

    this.shutdownNotifier = pShutdownNotifier;
    conditionCPAs = CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class).toList();
  }

  @Override
  public boolean run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    CFANode initialLocation = extractLocation(pReachedSet.getFirstState());
    invariantGenerator.start(initialLocation);

    try {
      logger.log(Level.INFO, "Creating formula for program");
      boolean soundInner;


      try (ProverEnvironment prover = solver.newProverEnvironmentWithModelGeneration();
          KInductionProver kInductionProver = new KInductionProver()) {

        do {
          shutdownNotifier.shutdownIfNecessary();
          soundInner = unroll(pReachedSet);
          if (from(pReachedSet)
              .skip(1) // first state of reached is always an abstraction state, so skip it
              .transform(toState(PredicateAbstractState.class))
              .anyMatch(FILTER_ABSTRACTION_STATES)) {

            logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
            return soundInner;
          }

          // first check safety
          boolean safe = checkTargetStates(pReachedSet, prover);
          logger.log(Level.FINER, "Program is safe?:", safe);

          if (!safe) {
            createErrorPath(pReachedSet, prover);
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
            sound = checkBoundingAssertions(pReachedSet, prover);

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
    }
  }

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
        logger.log(Level.WARNING, "Could not create error path because of missing branching informating");
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

  private class KInductionProver implements AutoCloseable {

    private ProverEnvironment prover = null;

    private final Boolean trivialResult;

    private final Iterable<CFAEdge> incomingEdges;

    private final Iterable<CFAEdge> outgoingEdges;

    private final ReachedSet reachedSet;

    private final Loop loop;

    private UnmodifiableReachedSet invariantsReachedSet;

    public KInductionProver() {
      List<CFAEdge> incomingEdges = null;
      List<CFAEdge> outgoingEdges = null;
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
              }).toList();

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
          reachedSet.add(cpa.getInitialState(loopHead), cpa.getInitialPrecision(loopHead));
          stats.inductionPreparation.stop();
        }
      }
      this.incomingEdges = trivialResult == null ? incomingEdges : null;
      this.outgoingEdges = trivialResult == null ? outgoingEdges : null;
      this.reachedSet = reachedSet;
      this.loop = loop;
    }

    private boolean isTrivial() {
      return this.trivialResult != null;
    }

    private boolean getTrivialResult() {
      Preconditions.checkState(isTrivial(), "The proof is non-trivial.");
      return trivialResult;
    }

    private Iterable<CFAEdge> getIncomingEdges() {
      Preconditions.checkState(!isTrivial(), "No incoming edges computed, because the proof is trivial.");
      assert incomingEdges != null;
      return incomingEdges;
    }

    private Iterable<CFAEdge> getOutgoingEdges() {
      Preconditions.checkState(!isTrivial(), "No outgoing edges computed, because the proof is trivial.");
      assert outgoingEdges != null;
      return outgoingEdges;
    }

    private ReachedSet getCurrentReachedSet() {
      Preconditions.checkState(!isTrivial(), "No reached set created, because the proof is trivial.");
      assert reachedSet != null;
      return reachedSet;
    }

    private Loop getLoop() {
      Preconditions.checkState(!isTrivial(), "No loop computed, because the proof is trivial.");
      assert loop != null;
      return loop;
    }

    private boolean isProverInitialized() {
      return prover != null;
    }

    private ProverEnvironment getProver() throws CPAException, InterruptedException {
      // get global invariants
      CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
      UnmodifiableReachedSet currentInvariantsReachedSet = invariantGenerator.get();
      if (currentInvariantsReachedSet != invariantsReachedSet) {
        invariantsReachedSet = currentInvariantsReachedSet;
        BooleanFormula invariants = extractInvariantsAt(currentInvariantsReachedSet, loopHead);
        if (isProverInitialized()) {
          prover.pop();
        } else {
          prover = solver.newProverEnvironmentWithModelGeneration();
        }
        invariants = fmgr.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));
        prover.push(invariants);
      }
      assert isProverInitialized();
      return prover;
    }

    @Override
    public void close() {
      if (isProverInitialized()) {
        prover.pop();
        prover.close();
      }
    }

    private BooleanFormula extractInvariantsAt(UnmodifiableReachedSet pReachedSet, CFANode pLocation) throws CPAException, InterruptedException {
      BooleanFormula invariant = bfmgr.makeBoolean(false);

      if (pReachedSet.isEmpty()) {
        return bfmgr.makeBoolean(true); // no invariants available
      }

      for (AbstractState locState : AbstractStates.filterLocation(pReachedSet, pLocation)) {
        BooleanFormula f = AbstractStates.extractReportedFormulas(fmgr, locState);
        logger.log(Level.ALL, "Invariant:", f);

        invariant = bfmgr.or(invariant, f);
      }
      return invariant;
    }

    public final boolean check() throws CPAException, InterruptedException {
      if (isTrivial()) {
        return getTrivialResult();
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
      prover.push(inductions);
      boolean sound = prover.isUnsat();

      if (!sound && logger.wouldBeLogged(Level.ALL)) {
        logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
      }
      prover.pop();
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
      final Iterable<CFAEdge> cutPointEdges;
      {
        Iterable<CFAEdge> relevantOutgoingEdges = FluentIterable.from(getOutgoingEdges()).filter(new Predicate<CFAEdge>() {@Override
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
        cutPointEdges = Iterables.concat(relevantOutgoingEdges, relevantInsideEdges);
      }
      return cutPointEdges;
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
   * @throws PredicatedAnalysisPropertyViolationException
   * @throws CPAException
   * @throws InterruptedException
   */
  private boolean unroll(ReachedSet pReachedSet) throws PredicatedAnalysisPropertyViolationException, CPAException, InterruptedException {
    adjustReachedSet(pReachedSet);
    return algorithm.run(pReachedSet);
  }

  private void adjustReachedSet(ReachedSet pReachedSet) {
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
      pReachedSet.add(cpa.getInitialState(initialLocation), cpa.getInitialPrecision(initialLocation));
    }
  }
}
