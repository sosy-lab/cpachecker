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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This class can check feasibility of a simple path using an SMT solver.
 */
@Options(prefix="counterexample.export", deprecatedPrefix="cpa.predicate")
public class PathChecker {

  @Option(secure=true, name="formula", deprecatedName="dumpCounterexampleFormula",
      description="where to dump the counterexample formula in case a specification violation is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleFormula = PathTemplate.ofFormatString("Counterexample.%d.smt2");

  @Option(secure=true, name="model", deprecatedName="dumpCounterexampleModel",
      description="where to dump the counterexample model in case a specification violation is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleModel = PathTemplate.ofFormatString("Counterexample.%d.assignment.txt");

  private final LogManager logger;
  private final PathFormulaManager pmgr;
  private final Solver solver;
  private final AssignmentToPathAllocator assignmentToPathAllocator;

  public PathChecker(Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel,
      PathFormulaManager pPmgr,
      Solver pSolver) throws InvalidConfigurationException {
    this(pConfig, pLogger, pPmgr, pSolver, new AssignmentToPathAllocator(pConfig, pShutdownNotifier, pLogger, pMachineModel));
  }

  public PathChecker(
      Configuration pConfig,
      LogManager pLogger,
      PathFormulaManager pPmgr,
      Solver pSolver,
      AssignmentToPathAllocator pAssignmentToPathAllocator) throws InvalidConfigurationException {
    this.logger = pLogger;
    this.pmgr = pPmgr;
    this.solver = pSolver;
    this.assignmentToPathAllocator = pAssignmentToPathAllocator;
    pConfig.inject(this);
  }

  public CounterexampleInfo handleFeasibleCounterexample(final ARGPath allStatesTrace,
      CounterexampleTraceInfo counterexample, boolean branchingOccurred)
          throws InterruptedException {
    checkArgument(!counterexample.isSpurious());

    ARGPath targetPath;
    if (branchingOccurred) {
      Map<Integer, Boolean> preds = counterexample.getBranchingPredicates();
      if (preds.isEmpty()) {
        logger.log(Level.WARNING, "No information about ARG branches available!");
        return createImpreciseCounterexample(allStatesTrace, counterexample);
      }

      // find correct path
      try {
        ARGState root = allStatesTrace.getFirstState();
        ARGState target = allStatesTrace.getLastState();
        Set<ARGState> pathElements = ARGUtils.getAllStatesOnPathsTo(target);

        targetPath = ARGUtils.getPathFromBranchingInformation(root, target, pathElements, preds);

      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, null);
        logger.log(Level.WARNING, "The error path and the satisfying assignment may be imprecise!");

        return createImpreciseCounterexample(allStatesTrace, counterexample);
      }

    } else {
      targetPath = allStatesTrace;
    }

    return createCounterexample(targetPath, counterexample, branchingOccurred);
  }

  /**
   * Create a {@link CounterexampleInfo} object for a given counterexample.
   * If the path has branching it will be checked again with an SMT solver to extract a model
   * that is as precise and simple as possible.
   * If the double-check fails, an imprecise result is returned.
   *
   * @param precisePath The precise ARGPath that represents the counterexample.
   * @param pInfo More information about the counterexample
   * @param pathHasBranching Whether there are branches in the ARG for this path
   * @return a {@link CounterexampleInfo} instance
   */
  public CounterexampleInfo createCounterexample(
      final ARGPath precisePath,
      final CounterexampleTraceInfo pInfo,
      final boolean pathHasBranching)
      throws InterruptedException {

    CFAPathWithAssumptions pathWithAssignments;
    CounterexampleTraceInfo preciseInfo;
    try {
      if (pathHasBranching) {
        Pair<CounterexampleTraceInfo, CFAPathWithAssumptions> replayedPathResult =
            checkPath(precisePath);

        if (replayedPathResult.getFirst().isSpurious()) {
          logger.log(Level.WARNING, "Inconsistent replayed error path!");
          logger.log(Level.WARNING, "The satisfying assignment may be imprecise!");
          return createImpreciseCounterexample(precisePath, pInfo);

        } else {
          preciseInfo = replayedPathResult.getFirst();
          pathWithAssignments = replayedPathResult.getSecond();
        }

      } else {
        preciseInfo = pInfo;
        List<SSAMap> ssamaps = createPrecisePathFormula(precisePath).getSecond();
        pathWithAssignments =
            assignmentToPathAllocator.allocateAssignmentsToPath(
                precisePath, pInfo.getModel(), ssamaps);
      }

    } catch (SolverException | CPATransferException e) {
      // path is now suddenly a problem
      logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
      logger.log(Level.WARNING, "The satisfying assignment may be imprecise!");
      return createImpreciseCounterexample(precisePath, pInfo);
    }

    CounterexampleInfo cex = CounterexampleInfo.feasiblePrecise(precisePath, pathWithAssignments);
    addCounterexampleFormula(preciseInfo, cex);
    addCounterexampleModel(preciseInfo, cex);
    return cex;
  }

  /**
   * Create a {@link CounterexampleInfo} object for a given counterexample.
   * Use this method if a precise {@link ARGPath} for the counterexample could not be constructed.
   *
   * @param imprecisePath Some ARGPath that is related to the counterexample.
   * @param pInfo More information about the counterexample
   * @return a {@link CounterexampleInfo} instance
   */
  private CounterexampleInfo createImpreciseCounterexample(
      final ARGPath imprecisePath, final CounterexampleTraceInfo pInfo) {
    CounterexampleInfo cex =
        CounterexampleInfo.feasibleImprecise(imprecisePath);
    addCounterexampleFormula(pInfo, cex);
    addCounterexampleModel(pInfo, cex);
    return cex;
  }

  private void addCounterexampleModel(
      CounterexampleTraceInfo cexInfo, CounterexampleInfo counterexample) {
    final ImmutableList<ValueAssignment> model =
        Ordering.usingToString().immutableSortedCopy(cexInfo.getModel());

    counterexample.addFurtherInformation(
        new AbstractAppender() {
          @Override
          public void appendTo(Appendable out) throws IOException {
            Joiner.on('\n').appendTo(out, model);
          }
        },
        dumpCounterexampleModel);
  }

  private void addCounterexampleFormula(
      CounterexampleTraceInfo cexInfo, CounterexampleInfo counterexample) {
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();

    BooleanFormula f = bfmgr.and(cexInfo.getCounterExampleFormulas());
    if (!bfmgr.isTrue(f)) {
      counterexample.addFurtherInformation(fmgr.dumpFormula(f), dumpCounterexampleFormula);
    }
  }

  private Pair<CounterexampleTraceInfo, CFAPathWithAssumptions> checkPath(ARGPath pPath)
      throws SolverException, CPATransferException, InterruptedException {

    Pair<PathFormula, List<SSAMap>> result = createPrecisePathFormula(pPath);

    List<SSAMap> ssaMaps = result.getSecond();

    PathFormula pathFormula = result.getFirstNotNull();

    BooleanFormula f = pathFormula.getFormula();

    try (ProverEnvironment thmProver = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      thmProver.push(f);
      if (thmProver.isUnsat()) {
        return Pair.of(CounterexampleTraceInfo.infeasibleNoItp(), null);
      } else {
        List<ValueAssignment> model = getModel(thmProver);
        CFAPathWithAssumptions pathWithAssignments =
            assignmentToPathAllocator.allocateAssignmentsToPath(pPath, model, ssaMaps);

        return Pair.of(
            CounterexampleTraceInfo.feasible(
                ImmutableList.of(f), model, ImmutableMap.<Integer, Boolean>of()),
            pathWithAssignments);
      }
    }
  }

  /**
   * Calculate the precise PathFormula and SSAMaps for the given path.
   * Multi-edges will be resolved. The resulting list of SSAMaps
   * need not be the same size as the given path.
   *
   * @param pPath calculate the precise list of SSAMaps for this path.
   * @return the PathFormula and the precise list of SSAMaps for the given path.
   */
  private Pair<PathFormula, List<SSAMap>> createPrecisePathFormula(ARGPath pPath)
      throws CPATransferException, InterruptedException {

    List<SSAMap> ssaMaps = new ArrayList<>(pPath.size());

    PathFormula pathFormula = pmgr.makeEmptyPathFormula();

    PathIterator pathIt = pPath.fullPathIterator();

    while (pathIt.hasNext()) {
      CFAEdge edge = pathIt.getOutgoingEdge();
      pathIt.advance();

      pathFormula = pmgr.makeAnd(pathFormula, edge);
      ssaMaps.add(pathFormula.getSsa());
    }

    return Pair.of(pathFormula, ssaMaps);
  }

  private List<ValueAssignment> getModel(ProverEnvironment thmProver) {
    try {
      return thmProver.getModelAssignments();
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Solver could not produce model, variable assignment of error path can not be dumped.");
      logger.logDebugException(e);
      return ImmutableList.of();
    }
  }
}