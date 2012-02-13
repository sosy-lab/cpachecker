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
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.*;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.FILTER_ABSTRACTION_ELEMENTS;
import static org.sosy_lab.cpachecker.util.AbstractElements.*;
import static org.sosy_lab.cpachecker.util.assumptions.ReportingUtils.extractReportedFormulas;

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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@Options(prefix="bmc")
public class BMCAlgorithm implements Algorithm, StatisticsProvider {

  private static final Function<AbstractElement, PredicateAbstractElement> EXTRACT_PREDICATE_ELEMENT
      = AbstractElements.extractElementByTypeFunction(PredicateAbstractElement.class);

  private static final Predicate<AbstractElement> IS_STOP_ELEMENT =
    Predicates.compose(new Predicate<AssumptionStorageElement>() {
                             @Override
                             public boolean apply(AssumptionStorageElement pArg0) {
                               return (pArg0 != null) && pArg0.isStop();
                             }
                           },
                       AbstractElements.extractElementByTypeFunction(AssumptionStorageElement.class));

  private static final Predicate<AbstractElement> IS_IN_LOOP = new Predicate<AbstractElement>() {
    @Override
    public boolean apply(AbstractElement pArg0) {
      LoopstackElement loopElement = extractElementByType(pArg0, LoopstackElement.class);
      return loopElement.getLoop() != null;
    }
  };

  private static <T> boolean none(Iterable<T> iterable, Predicate<? super T> predicate) {
    return !any(iterable, predicate);
  }

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

  private final FormulaManager fmgr;
  private final PathFormulaManager pmgr;
  private final TheoremProver prover;

  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

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
    pmgr = predCpa.getPathFormulaManager();
    prover = predCpa.getTheoremProver();
  }

  @Override
  public boolean run(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    if (induction) {
      CFANode initialLocation = extractLocation(pReachedSet.getFirstElement());
      invariantGenerator.start(initialLocation);
    }

    try {
      logger.log(Level.INFO, "Creating formula for program");
      final boolean soundInner = algorithm.run(pReachedSet);

      if (any(transform(skip(pReachedSet, 1), EXTRACT_PREDICATE_ELEMENT), FILTER_ABSTRACTION_ELEMENTS)) {
        // first element of reached is always an abstraction element, so skip it
        logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
        return soundInner;
      }

      prover.init();
      try {

        // first check safety
        boolean safe = checkTargetStates(pReachedSet);
        logger.log(Level.FINER, "Program is safe?:", safe);

        if (!safe) {
          createErrorPath(pReachedSet);
        }

        prover.pop(); // remove program formula from solver stack

        // second check soundness
        boolean sound = false;

        // verify soundness, but don't bother if we are unsound anyway or we have found a bug
        if (soundInner && safe) {

          // check bounding assertions
          sound = checkBoundingAssertions(pReachedSet);

          // try to prove program safety via induction
          if (induction) {
            sound = sound || checkWithInduction();
          }
        }

        return sound && soundInner;

      } finally {
        prover.reset();
      }

    } finally {
      invariantGenerator.cancelAndWait();
    }
  }

  /**
   * This method tries to find a feasible path to (one of) the target element(s).
   * It does so by asking the solver for a satisfying assignment.
   */
  private void createErrorPath(final ReachedSet pReachedSet) throws CPATransferException {
    if (!(cpa instanceof ARTCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARTCPA");
      return;
    }

    stats.errorPathCreation.start();
    try {
      logger.log(Level.INFO, "Error found, creating error path");

      Iterable<ARTElement> art = Iterables.filter(pReachedSet.getReached(), ARTElement.class);

      // get the branchingFormula
      // this formula contains predicates for all branches we took
      // this way we can figure out which branches make a feasible path
      Formula branchingFormula = pmgr.buildBranchingFormula(art);

      if (branchingFormula.isTrue()) {
        logger.log(Level.WARNING, "Could not create error path because of missing branching informating");
        return;
      }

      // add formula to solver environment
      prover.push(branchingFormula);

      // need to ask solver for satisfiability again,
      // otherwise model doesn't contain new predicates
      boolean stillSatisfiable = !prover.isUnsat();

      if (!stillSatisfiable) {
        // should not occur
        logger.log(Level.WARNING, "Could not create error path information because of inconsistent branching information!");
        return;
      }

      Model model = prover.getModel();
      prover.pop(); // remove branchingFormula


      // get precise error path
      Map<Integer, Boolean> branchingInformation = pmgr.getBranchingPredicateValuesFromModel(model);
      ARTElement root = (ARTElement)pReachedSet.getFirstElement();

      Path targetPath;
      try {
        targetPath = ARTUtils.getPathFromBranchingInformation(root, pReachedSet.getReached(), branchingInformation);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not create error path");
        return;
      }


      // replay error path for a more precise satisfying assignment
      Formula pathFormula = pmgr.makeFormulaForPath(targetPath.asEdgesList()).getFormula();
      prover.pop(); // remove program formula

      prover.push(pathFormula);

      if (prover.isUnsat()) {
        logger.log(Level.WARNING, "Inconsistent replayed error path!");
      } else {
        model = prover.getModel();
      }

      // create and store CounterexampleInfo object
      CounterexampleInfo counterexample = CounterexampleInfo.feasible(targetPath, model);
      if (pathFormula != null) {
        counterexample.addFurtherInformation(pathFormula, dumpCounterexampleFormula);
      }

      ((ARTCPA)cpa).setCounterexample(counterexample);

    } finally {
      stats.errorPathCreation.stop();
    }
  }

  private boolean checkTargetStates(final ReachedSet pReachedSet) {
    if (checkTargetStates) {

      List<AbstractElement> targetElements = Lists.newArrayList(AbstractElements.filterTargetElements(pReachedSet));
      logger.log(Level.FINER, "Found", targetElements.size(), "potential target elements");

      // create formula
      Formula program = createFormulaFor(targetElements);

      logger.log(Level.INFO, "Starting satisfiability check...");
      stats.satCheck.start();
      prover.push(program);
      boolean safe = prover.isUnsat();
      // leave program formula on solver stack
      stats.satCheck.stop();

      if (safe) {
        pReachedSet.removeAll(targetElements);
      }
      return safe;

    } else {
      // fast check for trivial cases
      return none(pReachedSet, IS_TARGET_ELEMENT);
    }
  }

  private boolean checkBoundingAssertions(final ReachedSet pReachedSet) {
    if (boundingAssertions) {
      // create formula for unwinding assertions
      Iterable<AbstractElement> stopElements = filter(pReachedSet, IS_STOP_ELEMENT);
      Formula assertions = createFormulaFor(stopElements);

      logger.log(Level.INFO, "Starting assertions check...");

      stats.assertionsCheck.start();
      boolean sound = prover.isUnsat(assertions);
      stats.assertionsCheck.stop();

      logger.log(Level.FINER, "Soundness after assertion checks:", sound);
      return sound;

    } else {
      // fast check for trivial cases
      return none(pReachedSet, IS_STOP_ELEMENT);
    }
  }

  /**
   * Create a disjunctive formula of all the path formulas in the supplied iterable.
   */
  private Formula createFormulaFor(Iterable<AbstractElement> elements) {
    Formula f = fmgr.makeFalse();

    for (PredicateAbstractElement e : transform(elements, EXTRACT_PREDICATE_ELEMENT)) {
      assert e != null : "PredicateCPA exists but did not produce elements!";

      f = fmgr.makeOr(f, e.getPathFormula().getFormula());
    }

    return f;
  }

  private boolean checkWithInduction() throws CPAException, InterruptedException {
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
    Iterable<CFAEdge> incomingEdges = Iterables.filter(loop.getIncomingEdges(),
                                                       Predicates.not(instanceOf(FunctionReturnEdge.class)));
    Iterable<CFAEdge> outgoingEdges = Iterables.filter(loop.getOutgoingEdges(),
                                                       Predicates.not(instanceOf(FunctionCallEdge.class)));

    if (Iterables.size(incomingEdges) > 1) {
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
    // which leads to the error location. This edge is always an AssumeEdge,
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
    reached.add(cpa.getInitialElement(loopHead), cpa.getInitialPrecision(loopHead));

    // Run algorithm in order to create formula (A & B)

    logger.log(Level.INFO, "Running algorithm to create induction hypothesis");
    algorithm.run(reached);

    Multimap<CFANode, AbstractElement> reachedPerLocation = Multimaps.index(reached, AbstractElements.EXTRACT_LOCATION);

    // live view of reached set with only the elements in the loop
    Iterable<AbstractElement> loopStates = Iterables.filter(reached, IS_IN_LOOP);

    assert !Iterables.isEmpty(loopStates);
    if (Iterables.any(loopStates, IS_TARGET_ELEMENT)) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, target state is contained in the loop");
      return false;
    }

    // get global invariants
    Formula invariants = extractInvariantsAt(loopHead, invariantGenerator.get());
    invariants = fmgr.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));

    // Create formulas
    Formula inductions = fmgr.makeTrue();

    for (CFAEdge outgoingEdge : outgoingEdges) {
      // filter out exit edges that do not lead to a target state, we don't care about them
      {
        CFANode exitLocation = outgoingEdge.getSuccessor();
        Iterable<AbstractElement> exitStates = reachedPerLocation.get(exitLocation);
        ARTElement lastExitState = (ARTElement)Iterables.getLast(exitStates);

        // the states reachable from the exit edge
        Set<ARTElement> outOfLoopStates = lastExitState.getSubtree();
        if (Iterables.isEmpty(filterTargetElements(outOfLoopStates))) {
          // no target state reachable
          continue;
        }
      }
      stats.inductionCutPoints++;
      logger.log(Level.FINEST, "Considering exit edge", outgoingEdge);

      CFANode cutPoint = outgoingEdge.getPredecessor();
      Iterable<AbstractElement> cutPointStates = reachedPerLocation.get(cutPoint);
      AbstractElement lastcutPointState = Iterables.getLast(cutPointStates);

      // Create (A & B)
      PathFormula pathFormulaAB = extractElementByType(lastcutPointState, PredicateAbstractElement.class).getPathFormula();
      Formula formulaAB = fmgr.makeAnd(invariants, pathFormulaAB.getFormula());
      assert (!prover.isUnsat(formulaAB));

      // Create C
      PathFormula empty = pmgr.makeEmptyPathFormula(pathFormulaAB); // empty has correct SSAMap
      PathFormula pathFormulaC = pmgr.makeAnd(empty, outgoingEdge);
      // we need to negate it, because we used the outgoing edge, not the continuation edge
      Formula formulaC = fmgr.makeNot(pathFormulaC.getFormula());

      // Crate (A & B) => C
      Formula f = fmgr.makeOr(fmgr.makeNot(formulaAB), formulaC);

      inductions = fmgr.makeAnd(inductions, f);
    }

    // now prove that (A & B) => C is a tautology by checking if the negation is unsatisfiable

    inductions = fmgr.makeNot(inductions);

    stats.inductionPreparation.stop();

    logger.log(Level.INFO, "Starting induction check...");

    stats.inductionCheck.start();
    boolean sound = prover.isUnsat(inductions);
    stats.inductionCheck.stop();

    if (!sound && logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
    }

    logger.log(Level.FINER, "Soundness after induction check:", sound);
    return sound;
  }

  private Formula extractInvariantsAt(CFANode loc, ReachedSet reached) {
    if (reached.isEmpty()) {
      return fmgr.makeTrue(); // invariant generation was disabled
    }

    Formula invariant = fmgr.makeFalse();

    for (AbstractElement locState : AbstractElements.filterLocation(reached, loc)) {
      Formula f = extractReportedFormulas(fmgr, locState);
      logger.log(Level.ALL, "Invariant:", f);

      invariant = fmgr.makeOr(invariant, f);
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
    private final ConfigurableProgramAnalysis invariantCPAs;
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

        invariantCPAs = new CPABuilder(invariantConfig, logger, reachedSetFactory).buildCPAs(cfa);
        reached = new ReachedSetFactory(invariantConfig, logger).create();

      } else {
        // invariant generation is disabled
        invariantCPAs = null;
        reached = new ReachedSetFactory(config, logger).create(); // create reached set that will stay empty
      }
    }

    public void start(CFANode pInitialLocation) {
      checkState(initialLocation == null);
      initialLocation = pInitialLocation;

      if (invariantCPAs == null) {
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

      if (invariantCPAs == null) {
        // invariant generation disabled
        return reached;
      }

      invariantGeneration.start();
      logger.log(Level.INFO, "Finding invariants");

      try {
        reached.add(invariantCPAs.getInitialElement(initialLocation), invariantCPAs.getInitialPrecision(initialLocation));

        Algorithm invariantAlgorithm = new CPAAlgorithm(invariantCPAs, logger);

        invariantAlgorithm.run(reached);

        return reached;

      } finally {
        invariantGeneration.start();
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
