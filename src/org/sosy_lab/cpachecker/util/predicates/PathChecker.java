// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getAllStatesOnPathsTo;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.overflow.OverflowState;
import org.sosy_lab.cpachecker.cpa.predicate.BAMBlockFormulaStrategy;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/** This class can check feasibility of a simple path using an SMT solver. */
@Options(prefix = "counterexample.export", deprecatedPrefix = "cpa.predicate")
public class PathChecker implements Statistics {

  @Option(
      secure = true,
      name = "formula",
      deprecatedName = "dumpCounterexampleFormula",
      description =
          "where to dump the counterexample formula in case a specification violation is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleFormula =
      PathTemplate.ofFormatString("Counterexample.%d.smt2");

  @Option(
      secure = true,
      name = "model",
      deprecatedName = "dumpCounterexampleModel",
      description =
          "where to dump the counterexample model in case a specification violation is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleModel =
      PathTemplate.ofFormatString("Counterexample.%d.assignment.txt");

  @Option(
      secure = true,
      description =
          "An imprecise counterexample of the Predicate CPA is usually a bug,"
              + " but expected in some configurations. Should it be treated as a bug or accepted?")
  private boolean allowImpreciseCounterexamples = false;

  @Option(
      secure = true,
      description =
          "Always use imprecise counterexamples of the predicate analysis. If this option is set to"
              + " true, counterexamples generated by the predicate analysis will be exported as-is."
              + " This means that no information like variable assignments will be added and"
              + " imprecise or potentially wrong program paths will be exported as counterexample.")
  private boolean alwaysUseImpreciseCounterexamples = false;

  @Option(
      secure = true,
      description = "Reuse the last __VERIFIER_nondet_ variable assignments of last iteration")
  private boolean withReuse = false;



  private final LogManager logger;
  private final PathFormulaManager pmgr;
  private final Solver solver;
  private final AssignmentToPathAllocator assignmentToPathAllocator;
  private List<ValueAssignment> inputModelCache = new ArrayList<ValueAssignment>();
  private List<ValueAssignment> inputModelCacheForReuse = new ArrayList<ValueAssignment>();
  private int countNondetVars = 0;
  private int countReuses = 0;
  private int countReusedVAs = 0;
  private int countFailedReuses = 0;
  private int countFullReuses = 0;
  private int pfSize = 0;

  public PathChecker(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel,
      PathFormulaManager pPmgr,
      Solver pSolver)
      throws InvalidConfigurationException {
    this(
        pConfig,
        pLogger,
        pPmgr,
        pSolver,
        new AssignmentToPathAllocator(pConfig, pShutdownNotifier, pLogger, pMachineModel));
  }

  public PathChecker(
      Configuration pConfig,
      LogManager pLogger,
      PathFormulaManager pPmgr,
      Solver pSolver,
      AssignmentToPathAllocator pAssignmentToPathAllocator)
      throws InvalidConfigurationException {
    logger = pLogger;
    pmgr = pPmgr;
    solver = pSolver;
    assignmentToPathAllocator = pAssignmentToPathAllocator;
    pConfig.inject(this);
  }

  /**
   * Create a {@link CounterexampleInfo} object for a given counterexample. The path will be checked
   * again with an SMT solver to extract a model that is as precise and simple as possible. We
   * assume that one additional SMT query will not cause too much overhead. If counterexample does
   * not contain precise path information or the double-check fails, the method may crash or return
   * an imprecise result is (depending on configuration).
   *
   * @param counterexample The representation of the counterexample (must not be spurious).
   * @param fallbackPath A potentially imprecise/wrong path that should be used as fallback if the
   *     precise path does not exist / is contradicting.
   * @return a {@link CounterexampleInfo} instance
   */
  public CounterexampleInfo handleFeasibleCounterexample(
      final CounterexampleTraceInfo counterexample, final ARGPath fallbackPath)
      throws InterruptedException {

    if (alwaysUseImpreciseCounterexamples) {
      return createImpreciseCounterexample(fallbackPath, counterexample);
    }

    checkArgument(!counterexample.isSpurious());

    final ARGPath precisePath;
    if (counterexample.getPrecisePath() == null) {
      // This happens if we fail to produce a precise path earlier, for example because the solver
      // does not give us a model.

      if (hasBranching(fallbackPath)) {
        // No branches/merges in path, it is precise anyway.
        precisePath = fallbackPath;
      } else {
        logger.log(Level.WARNING, "No information about ARG branches available!");
        return createImpreciseCounterexample(fallbackPath, counterexample);
      }

    } else {
      precisePath = counterexample.getPrecisePath();
    }


    if (withReuse) {
      return createCounterexampleWithReuse(precisePath, counterexample);
    } else {
      return createCounterexample(precisePath, counterexample);
    }
  }

  /** Determine whether the given path is the only possible path to its last state in the ARG. */
  private boolean hasBranching(ARGPath path) {
    Set<ARGState> elementsOnPath = getAllStatesOnPathsTo(path.getLastState());
    if (elementsOnPath.size() >= path.size()) {
      return true;
    }

    // Check whether the path contains branching in the form A->B; B->C; A->C;
    for (ARGState state : elementsOnPath) {
      if (from(state.getChildren()).filter(elementsOnPath::contains).size() > 1) {
        return true;
      }
    }

    return false;
  }

  private CounterexampleInfo createCounterexampleWithReuse(
      final ARGPath precisePath, final CounterexampleTraceInfo pInfo) throws InterruptedException {

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      Pair<PathFormula, List<SSAMap>> replayedPath = createPrecisePathFormula(precisePath);
      List<SSAMap> ssaMaps = replayedPath.getSecond();
      BooleanFormula pathFormula = replayedPath.getFirstNotNull().getFormula();
      LinkedHashSet<BooleanFormula> previousVariableAssignments;
      prover.push(pathFormula);
      boolean fullReuse = true;
      if (pfSize < pathFormula.toString().length()) {
        // erst alle assigments pushen oder immer nur eins? // vlt alle, wenn unsat letzte raus ?
        // aber
        // dass probiert dann wieder
        for (ValueAssignment assignment : inputModelCacheForReuse) {
          countReusedVAs += 1;
          prover.push(
              assignment
                  .getAssignmentAsFormula()); // mehr als 1 va pushen vlt alles was sich in letztem
        }

        if (prover.isUnsat()) {
          fullReuse = false;
          countFailedReuses += 1;
          for (ValueAssignment assignment : inputModelCacheForReuse) {
            prover.pop();
            countReusedVAs -= 1;
          }
          if (prover.isUnsat()) { // TODO kann das bei testcomp vlt weg?
            logger.log(
                Level.WARNING,
                "Inconsistent replayed error path! No variable values will be available.");
            return createImpreciseCounterexample(precisePath, pInfo);
          }
        } else {
          countReuses += 1;
        }
      } else {
        if (prover.isUnsat()) { // TODO kann das bei testcomp vlt weg?
          logger.log(
              Level.WARNING,
              "Inconsistent replayed error path! No variable values will be available.");
          return createImpreciseCounterexample(precisePath, pInfo);
        }
      }

      ImmutableList<ValueAssignment> model = getModel(prover);
      List<ValueAssignment> modelHelper = new ArrayList<>();
      pfSize = pathFormula.toString().length();
      inputModelCacheForReuse.clear();
      for (ValueAssignment va : model) {
        if (va.getName().startsWith("__VERIFIER_nondet_")) {
          if (inputModelCache.contains(va)) {
            inputModelCacheForReuse.add(va);
          } else {
            fullReuse = false;
          }
        }
        modelHelper.add(va);
      }

      inputModelCache.clear();
      countNondetVars = modelHelper.size();
      inputModelCache.addAll(modelHelper);

      CFAPathWithAssumptions pathWithAssignments =
          assignmentToPathAllocator.allocateAssignmentsToPath(precisePath, model, ssaMaps);

      CounterexampleInfo cex = CounterexampleInfo.feasiblePrecise(precisePath, pathWithAssignments);
      addCounterexampleFormula(ImmutableList.of(pathFormula), cex);
      addCounterexampleModel(model, cex);

      if (fullReuse) {
        cex.fullReuse = true;
        countFullReuses += 1;
      }

      return cex;

    } catch (SolverException | CPATransferException e) {
      // path is now suddenly a problem
      logger.logUserException(
          Level.WARNING, e, "Could not replay error path! No variable values will be available.");
      return createImpreciseCounterexample(precisePath, pInfo);
    }
  }

  /**
   * Create a {@link CounterexampleInfo} object for a given counterexample. The path will be checked
   * again with an SMT solver to extract a model that is as precise and simple as possible. We
   * assume that one additional SMT query will not cause too much overhead. If the double-check
   * fails, an imprecise result is returned.
   *
   * @param precisePath The precise ARGPath that represents the counterexample.
   * @param pInfo More information about the counterexample (as fallback).
   * @return a {@link CounterexampleInfo} instance
   */
  private CounterexampleInfo createCounterexample(
      final ARGPath precisePath, final CounterexampleTraceInfo pInfo) throws InterruptedException {

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      Pair<PathFormula, List<SSAMap>> replayedPath = createPrecisePathFormula(precisePath);
      List<SSAMap> ssaMaps = replayedPath.getSecond();
      BooleanFormula pathFormula = replayedPath.getFirstNotNull().getFormula();

      prover.push(pathFormula);

      if (prover.isUnsat()) {
        logger.log(
            Level.WARNING,
            "Inconsistent replayed error path! No variable values will be available.");
        return createImpreciseCounterexample(precisePath, pInfo);
      }

      ImmutableList<ValueAssignment> model = getModel(prover);
      CFAPathWithAssumptions pathWithAssignments =
          assignmentToPathAllocator.allocateAssignmentsToPath(precisePath, model, ssaMaps);

      CounterexampleInfo cex = CounterexampleInfo.feasiblePrecise(precisePath, pathWithAssignments);
      addCounterexampleFormula(ImmutableList.of(pathFormula), cex);
      addCounterexampleModel(model, cex);
      return cex;

    } catch (SolverException | CPATransferException e) {
      // path is now suddenly a problem
      logger.logUserException(
          Level.WARNING, e, "Could not replay error path! No variable values will be available.");
      return createImpreciseCounterexample(precisePath, pInfo);
    }
  }

  /**
   * Create a {@link CounterexampleInfo} object for a given counterexample. Use this method if a
   * precise {@link ARGPath} for the counterexample could not be constructed.
   *
   * @param imprecisePath Some ARGPath that is related to the counterexample.
   * @param pInfo More information about the counterexample
   * @return a {@link CounterexampleInfo} instance
   */
  private CounterexampleInfo createImpreciseCounterexample(
      final ARGPath imprecisePath, final CounterexampleTraceInfo pInfo) {
    if (!allowImpreciseCounterexamples) {
      throw new AssertionError(
          "Found imprecise counterexample in PredicateCPA. "
              + "If this is expected for this configuration "
              + "(e.g., because of UF-based heap encoding), "
              + "set counterexample.export.allowImpreciseCounterexamples=true. "
              + "Otherwise please report this as a bug.");
    }
    CounterexampleInfo cex = CounterexampleInfo.feasibleImprecise(imprecisePath);
    if (!alwaysUseImpreciseCounterexamples) {
      addCounterexampleFormula(pInfo.getCounterExampleFormulas(), cex);
    }
    return cex;
  }

  private void addCounterexampleModel(
      ImmutableList<ValueAssignment> model, CounterexampleInfo counterexample) {
    counterexample.addFurtherInformation(
        new AbstractAppender() {
          @Override
          public void appendTo(Appendable out) throws IOException {
            ImmutableList<String> lines =
                ImmutableList.sortedCopyOf(Lists.transform(model, Object::toString));
            Joiner.on('\n').appendTo(out, lines);
          }
        },
        dumpCounterexampleModel);
  }

  private void addCounterexampleFormula(
      List<BooleanFormula> cexFormulas, CounterexampleInfo counterexample) {
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();

    BooleanFormula f = bfmgr.and(cexFormulas);
    if (!bfmgr.isTrue(f)) {
      counterexample.addFurtherInformation(fmgr.dumpFormula(f), dumpCounterexampleFormula);
    }
  }

  /**
   * Calculate the precise PathFormula and SSAMaps for the given path. Multi-edges will be resolved.
   * The resulting list of SSAMaps need not be the same size as the given path.
   *
   * <p>If the path traverses recursive function calls, the path formula updates the SSAMaps.
   *
   * @param pPath calculate the precise list of SSAMaps for this path.
   * @return the PathFormula and the precise list of SSAMaps for the given path.
   */
  private Pair<PathFormula, List<SSAMap>> createPrecisePathFormula(ARGPath pPath)
      throws CPATransferException, InterruptedException {

    List<SSAMap> ssaMaps = new ArrayList<>(pPath.size());

    PathFormula pathFormula = pmgr.makeEmptyPathFormula();

    PathIterator pathIt = pPath.fullPathIterator();

    // for recursion we need to update SSA-indices after returning from a function call,
    // in non-recursive cases this should not change anything.
    Deque<PathFormula> callstack = new ArrayDeque<>();

    while (pathIt.hasNext()) {
      if (pathIt.isPositionWithState()) {
        pathFormula = addAssumptions(pathFormula, pathIt.getAbstractState());
      }
      CFAEdge edge = pathIt.getOutgoingEdge();
      pathIt.advance();

      if (!pathIt.hasNext() && pathIt.isPositionWithState()) {
        pathFormula = addAssumptions(pathFormula, pathIt.getAbstractState());
      }

      // for recursion
      if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        callstack.push(pathFormula);
      }

      // for recursion
      if (!callstack.isEmpty() && edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        pathFormula =
            BAMBlockFormulaStrategy.rebuildStateAfterFunctionCall(
                pathFormula, callstack.pop(), ((FunctionReturnEdge) edge).getPredecessor());
      }

      pathFormula = pmgr.makeAnd(pathFormula, edge);
      ssaMaps.add(pathFormula.getSsa());
    }

    return Pair.of(pathFormula, ssaMaps);
  }

  private PathFormula addAssumptions(PathFormula pathFormula, ARGState nextState)
      throws CPATransferException, InterruptedException {
    if (nextState != null) {
      FluentIterable<AbstractStateWithAssumptions> assumptionStates =
          AbstractStates.projectToType(
              AbstractStates.asIterable(nextState), AbstractStateWithAssumptions.class);
      for (AbstractStateWithAssumptions assumptionState : assumptionStates) {
        if (assumptionState instanceof OverflowState
            && ((OverflowState) assumptionState).hasOverflow()) {
          assumptionState = ((OverflowState) assumptionState).getParent();
        }
        for (AExpression expr : assumptionState.getAssumptions()) {
          assert expr instanceof CExpression : "Expected a CExpression as assumption!";
          pathFormula = pmgr.makeAnd(pathFormula, (CExpression) expr);
        }
      }
    }
    return pathFormula;
  }

  private ImmutableList<ValueAssignment> getModel(ProverEnvironment thmProver) {
    try {
      return thmProver.getModelAssignments();
    } catch (SolverException e) {
      logger.log(
          Level.WARNING,
          "Solver could not produce model, variable assignment of error path can not be dumped.");
      logger.logDebugException(e);
      return ImmutableList.of();
    }
  }


  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {

    StatisticsWriter w0 = writingStatisticsTo(pOut);
    w0.put("VAReuse ", withReuse);
    w0.put("Number of predicate refinements", countNondetVars);

    w0.put("Number of nondetint vars ", countNondetVars);

    w0.put("Number of reuses " , countReuses);

    w0.put("Number of reused variable assgiments ", countReusedVAs);

    w0.put("Number of failed reuses ", countFailedReuses);

    w0.put("Number of full reuses " , countFullReuses);



  }

  @Override
  public @Nullable String getName() {
    return "PathChecker: Reuse Variable Assignments of Nondet Variables";
  }
}
