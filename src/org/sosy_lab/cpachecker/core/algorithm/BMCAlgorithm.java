/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.FILTER_ABSTRACTION_STATES;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
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

  private static final Predicate<AbstractState> IS_IN_LOOP = new Predicate<AbstractState>() {
    @Override
    public boolean apply(AbstractState pArg0) {
      LoopstackState loopState = extractStateByType(pArg0, LoopstackState.class);
      return loopState.getLoop() != null;
    }
  };

  private class BMCStatistics implements Statistics {

    private final Timer satCheck = new Timer();
    private final Timer errorPathCreation = new Timer();
    private final Timer assertionsCheck = new Timer();

    private final Timer inductionPreparation = new Timer();
    private final Timer inductionCheck = new Timer();
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
        out.println("  Time for invariant generation:     " + invariantGenerator.invariantGeneration);
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

  @Option(description="dump counterexample formula to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File dumpCounterexampleFormula = new File("counterexample.msat");

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

  private BooleanFormulaManager bfmgr;

  public BMCAlgorithm(Algorithm algorithm, ConfigurableProgramAnalysis pCpa,
                      Configuration config, LogManager logger,
                      ReachedSetFactory pReachedSetFactory, CFA pCfa)
                      throws InvalidConfigurationException, CPAException {
    config.inject(this);
    this.algorithm = algorithm;
    this.cpa = pCpa;
    this.logger = logger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCfa;

    invariantGenerator = new InvariantGenerator(config, logger, reachedSetFactory, cfa);

    PredicateCPA predCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for BMCAlgorithm");
    }
    fmgr = predCpa.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    solver = predCpa.getSolver();
  }

  @Override
  public boolean run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    if (induction) {
      CFANode initialLocation = extractLocation(pReachedSet.getFirstState());
      invariantGenerator.start(initialLocation);
    }

    try {
      logger.log(Level.INFO, "Creating formula for program");
      final boolean soundInner = algorithm.run(pReachedSet);

      if (from(pReachedSet)
          .skip(1) // first state of reached is always an abstraction state, so skip it
          .transform(toState(PredicateAbstractState.class))
          .anyMatch(FILTER_ABSTRACTION_STATES)) {

        logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
        return soundInner;
      }

      try (ProverEnvironment prover = solver.newProverEnvironment()) {

        // first check safety
        boolean safe = checkTargetStates(pReachedSet, prover);
        logger.log(Level.FINER, "Program is safe?:", safe);

        if (!safe) {
          createErrorPath(pReachedSet, prover);
        }

        prover.pop(); // remove program formula from solver stack

        // second check soundness
        boolean sound = false;

        // verify soundness, but don't bother if we are unsound anyway or we have found a bug
        if (soundInner && safe) {

          // check bounding assertions
          sound = checkBoundingAssertions(pReachedSet, prover);

          // try to prove program safety via induction
          if (induction) {
            sound = sound || checkWithInduction(prover);
          }
        }

        return sound && soundInner;
      }

    } finally {
      invariantGenerator.cancelAndWait();
    }
  }

  /**
   * This method tries to find a feasible path to (one of) the target state(s).
   * It does so by asking the solver for a satisfying assignment.
   */
  private void createErrorPath(final ReachedSet pReachedSet, final ProverEnvironment prover) throws CPATransferException {
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


      // replay error path for a more precise satisfying assignment
      BooleanFormula pathFormula = pmgr.makeFormulaForPath(targetPath.asEdgesList()).getFormula();
      prover.pop(); // remove program formula

      prover.push(pathFormula);

      if (prover.isUnsat()) {
        logger.log(Level.WARNING, "Inconsistent replayed error path!");
      } else {
        try {
          model = prover.getModel();
        } catch (SolverException e) {
          logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
          logger.logDebugException(e);
          return;
        }
      }

      // create and store CounterexampleInfo object
      CounterexampleInfo counterexample = CounterexampleInfo.feasible(targetPath, model);
      if (pathFormula != null) {
        counterexample.addFurtherInformation(pathFormula, dumpCounterexampleFormula);
      }

      ((ARGCPA)cpa).setCounterexample(counterexample);

    } finally {
      stats.errorPathCreation.stop();
    }
  }

  private boolean checkTargetStates(final ReachedSet pReachedSet, final ProverEnvironment prover) {
    List<AbstractState> targetStates = from(pReachedSet)
                                            .filter(IS_TARGET_STATE)
                                            .toImmutableList();

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

  private boolean checkBoundingAssertions(final ReachedSet pReachedSet, final ProverEnvironment prover) {
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

  private boolean checkWithInduction(final ProverEnvironment prover) throws CPAException, InterruptedException {
    if (!cfa.getLoopStructure().isPresent()) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, loop structure of program could not be determined.");
      return false;
    }
    Multimap<String, Loop> loops = cfa.getLoopStructure().get();

    // Induction is currently only possible if there is a single loop.
    // This check can be weakend in the future,
    // e.g. it is ok if there is only a single loop on each path.
    if (loops.size() > 1) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, program has too many loops");
      return false;
    }

    if (loops.isEmpty()) {
      // induction is unnecessary, program has no loops
      return true;
    }

    stats.inductionPreparation.start();

    Loop loop = Iterables.getOnlyElement(loops.values());

    // function edges do not count as incoming/outgoing edges
    FluentIterable<CFAEdge> incomingEdges = from(loop.getIncomingEdges())
                                                 .filter(not(instanceOf(CFunctionReturnEdge.class)));
    FluentIterable<CFAEdge> outgoingEdges = from(loop.getOutgoingEdges())
                                                 .filter(not(instanceOf(CFunctionCallEdge.class)));

    if (incomingEdges.size() > 1) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many incoming edges", incomingEdges);
      return false;
    }

    if (loop.getLoopHeads().size() > 1) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many loop heads");
      return false;
    }

    CFANode loopHead = Iterables.getOnlyElement(loop.getLoopHeads());

    // check that the loop head is unambigious
    assert loopHead.equals(Iterables.getOnlyElement(incomingEdges).getSuccessor());

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
    // the outgoing edge, if it didn't take it in the iteration before.
    // We create three formulas:
    // A is the assumption from the continuation edge in the previous iteration
    // B is the formula for the loop body in the current iteration up to the cut point
    // C is the assumption from the continuation edge in the current iteration
    //   Note that this is the negation of the assumption from the exit edge.
    // Then we try to prove that the formula (A & B) => C holds.
    // This implies that control flow cannot take the exit edge.

    // The conjunction (A & B) is created by running the CPAAlgorithm starting
    // at the cut point and letting it run until the end of the current iteration
    // (i.e. let it finish the iteration it starts in and complete one more iteration).
    // Then we get the abstract state at the cut point in the last iteration
    // and take its path formula, which is exactly (A & B).
    // C is created manually. It is important to re-use the SSAMap from (A & B)
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


    // Create initial reached set
    ReachedSet reached = reachedSetFactory.create();
    reached.add(cpa.getInitialState(loopHead), cpa.getInitialPrecision(loopHead));

    // Run algorithm in order to create formula (A & B)

    logger.log(Level.INFO, "Running algorithm to create induction hypothesis");
    algorithm.run(reached);

    Multimap<CFANode, AbstractState> reachedPerLocation = Multimaps.index(reached, EXTRACT_LOCATION);

    // live view of reached set with only the states in the loop
    FluentIterable<AbstractState> loopStates = from(reached).filter(IS_IN_LOOP);

    assert !loopStates.isEmpty();
    if (loopStates.anyMatch(IS_TARGET_STATE)) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, target state is contained in the loop");
      return false;
    }

    // get global invariants
    BooleanFormula invariants = extractInvariantsAt(loopHead, invariantGenerator.get());
    invariants = fmgr.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));

    // Create formulas
    BooleanFormula inductions = bfmgr.makeBoolean(true);

    for (CFAEdge outgoingEdge : outgoingEdges) {
      // filter out exit edges that do not lead to a target state, we don't care about them
      {
        CFANode exitLocation = outgoingEdge.getSuccessor();
        Iterable<AbstractState> exitStates = reachedPerLocation.get(exitLocation);
        ARGState lastExitState = (ARGState)Iterables.getLast(exitStates);

        // the states reachable from the exit edge
        Set<ARGState> outOfLoopStates = lastExitState.getSubgraph();
        if (!from(outOfLoopStates).anyMatch(IS_TARGET_STATE)) {
          // no target state reachable
          continue;
        }
      }
      stats.inductionCutPoints++;
      logger.log(Level.FINEST, "Considering exit edge", outgoingEdge);

      CFANode cutPoint = outgoingEdge.getPredecessor();
      Iterable<AbstractState> cutPointStates = reachedPerLocation.get(cutPoint);
      AbstractState lastcutPointState = Iterables.getLast(cutPointStates);

      // Create (A & B)
      PathFormula pathFormulaAB = extractStateByType(lastcutPointState, PredicateAbstractState.class).getPathFormula();
      BooleanFormula formulaAB = bfmgr.and(invariants, pathFormulaAB.getFormula());

      // Create C
      PathFormula empty = pmgr.makeEmptyPathFormula(pathFormulaAB); // empty has correct SSAMap
      PathFormula pathFormulaC = pmgr.makeAnd(empty, outgoingEdge);
      // we need to negate it, because we used the outgoing edge, not the continuation edge
      BooleanFormula formulaC = bfmgr.not(pathFormulaC.getFormula());

      // Crate (A & B) => C
      BooleanFormula f = bfmgr.or(bfmgr.not(formulaAB), formulaC);

      inductions = bfmgr.and(inductions, f);
    }

    // now prove that (A & B) => C is a tautology by checking if the negation is unsatisfiable

    inductions = bfmgr.not(inductions);

    stats.inductionPreparation.stop();

    logger.log(Level.INFO, "Starting induction check...");

    stats.inductionCheck.start();
    prover.push(inductions);
    boolean sound = prover.isUnsat();
    prover.pop();
    stats.inductionCheck.stop();

    if (!sound && logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
    }

    logger.log(Level.FINER, "Soundness after induction check:", sound);
    return sound;
  }

  private BooleanFormula extractInvariantsAt(CFANode loc, ReachedSet reached) {
    if (reached.isEmpty()) {
      return bfmgr.makeBoolean(true); // invariant generation was disabled
    }

    BooleanFormula invariant =bfmgr.makeBoolean(false);

    for (AbstractState locState : AbstractStates.filterLocation(reached, loc)) {
      BooleanFormula f = AbstractStates.extractReportedFormulas(fmgr, locState);
      logger.log(Level.ALL, "Invariant:", f);

      invariant = bfmgr.or(invariant, f);
    }
    return invariant;
  }


  /**
   * Class that encapsulates invariant generation.
   * Supports synchronous and asynchronous execution.
   */
  @Options(prefix="bmc")
  private static class InvariantGenerator {

    @Option(name="invariantGenerationConfigFile",
            description="configuration file for invariant generation")
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    private File configFile;

    @Option(description="generate invariants for induction in parallel to the analysis")
    private boolean parallelInvariantGeneration = false;

    private final Timer invariantGeneration = new Timer();

    private final LogManager logger;
    private final Algorithm invariantAlgorithm;
    private final ReachedSet reached;

    private CFANode initialLocation = null;

    private ExecutorService executor = null;
    private Future<ReachedSet> invariantGenerationFuture = null;

    public InvariantGenerator(Configuration config, LogManager pLogger, ReachedSetFactory reachedSetFactory, CFA cfa) throws InvalidConfigurationException, CPAException {
      config.inject(this);
      logger = pLogger;

      if (configFile != null) {
        Configuration invariantConfig;
        try {
          invariantConfig = Configuration.builder()
                                .loadFromFile(configFile)
                                .build();
        } catch (IOException e) {
          throw new InvalidConfigurationException("could not read configuration file for invariant generation: " + e.getMessage(), e);
        }

        ConfigurableProgramAnalysis invariantCPAs = new CPABuilder(invariantConfig, logger, reachedSetFactory).buildCPAs(cfa);
        invariantAlgorithm = new CPAAlgorithm(invariantCPAs, logger, invariantConfig);

        reached = new ReachedSetFactory(invariantConfig, logger).create();
        reached.add(invariantCPAs.getInitialState(initialLocation), invariantCPAs.getInitialPrecision(initialLocation));

      } else {
        // invariant generation is disabled
        invariantAlgorithm = null;
        reached = new ReachedSetFactory(config, logger).create(); // create reached set that will stay empty
      }
    }

    public void start(CFANode pInitialLocation) {
      checkState(initialLocation == null);
      initialLocation = pInitialLocation;

      if (!reached.hasWaitingState()) {
        // invariant generation disabled
        return;
      }

      if (parallelInvariantGeneration) {

        executor = Executors.newSingleThreadExecutor();
        invariantGenerationFuture = executor.submit(new Callable<ReachedSet>() {
              @Override
              public ReachedSet call() throws Exception {
                return findInvariants();
              }
            });
        executor.shutdown();
      }
    }

    public void cancelAndWait() {
      if (invariantGenerationFuture != null) {
        invariantGenerationFuture.cancel(true);
        Concurrency.waitForTermination(executor);
      }
    }

    public ReachedSet get() throws CPAException, InterruptedException {
      if (invariantGenerationFuture == null) {
        return findInvariants();

      } else {
        try {
          return invariantGenerationFuture.get();

        } catch (ExecutionException e) {
          Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
          throw new UnexpectedCheckedException("invariant generation", e.getCause());
        }
      }
    }

    private ReachedSet findInvariants() throws CPAException, InterruptedException {
      checkState(initialLocation != null);

      if (!reached.hasWaitingState()) {
        // invariant generation disabled
        return reached;
      }

      invariantGeneration.start();
      logger.log(Level.INFO, "Finding invariants");

      try {
        assert invariantAlgorithm != null;
        invariantAlgorithm.run(reached);

        return reached;

      } finally {
        invariantGeneration.stop();
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }
}
