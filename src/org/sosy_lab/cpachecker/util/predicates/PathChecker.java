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

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.Appender;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Model.ValueAssignment;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.SolverContext.ProverOptions;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

/**
 * This class can check feasibility of a simple path using an SMT solver.
 */
@Options(prefix="cpa.predicate")
public class PathChecker {

  @Option(secure=true,
      description="where to dump the counterexample formula in case a specification violation is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleFormula = PathTemplate.ofFormatString("ErrorPath.%d.smt2");

  @Option(secure=true,
      description="where to dump the counterexample model in case a specification violation is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleModel = PathTemplate.ofFormatString("ErrorPath.%d.assignment.txt");

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

  public CounterexampleInfo createCounterexample(
      final ARGPath precisePath, final Iterable<ValueAssignment> pModel) throws InterruptedException {

    CounterexampleInfo counterexample;
    try {
      CounterexampleTraceInfo info = checkPath(precisePath);

      if (info.isSpurious()) {
        logger.log(Level.WARNING, "Inconsistent replayed error path!");
        logger.log(Level.WARNING, "The satisfying assignment may be imprecise!");
        counterexample = CounterexampleInfo.feasible(precisePath, CFAPathWithAssumptions.empty());
        counterexample.addFurtherInformation(dumpModel(pModel), dumpCounterexampleModel);

      } else {
        counterexample = CounterexampleInfo.feasiblePrecise(precisePath, info.getAssignments());
        counterexample.addFurtherInformation(dumpCounterexample(info), dumpCounterexampleFormula);
        counterexample.addFurtherInformation(dumpModel(info.getModel()), dumpCounterexampleModel);
      }

    } catch (SolverException | CPATransferException e) {
      // path is now suddenly a problem
      logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
      logger.log(Level.WARNING, "The satisfying assignment may be imprecise!");
      counterexample = CounterexampleInfo.feasible(precisePath, CFAPathWithAssumptions.empty());
      counterexample.addFurtherInformation(dumpModel(pModel), dumpCounterexampleModel);
    }
    return counterexample;
  }

  public CounterexampleInfo createCounterexampleForPathWithoutBranching(
      final ARGPath allStatesTrace, final CounterexampleTraceInfo pInfo)
      throws CPATransferException, InterruptedException {
    List<SSAMap> ssamaps = createPrecisePathFormula(allStatesTrace).getSecond();
    CFAPathWithAssumptions pathWithAssignments =
        assignmentToPathAllocator.allocateAssignmentsToPath(
            allStatesTrace, pInfo.getModel(), ssamaps);
    CounterexampleInfo cex =
        CounterexampleInfo.feasiblePrecise(allStatesTrace, pathWithAssignments);
    cex.addFurtherInformation(dumpCounterexample(pInfo), dumpCounterexampleFormula);
    cex.addFurtherInformation(dumpModel(pInfo.getModel()), dumpCounterexampleModel);
    return cex;
  }

  public CounterexampleInfo createImpreciseCounterexample(
      final ARGPath allStatesTrace, final CounterexampleTraceInfo pInfo) {
    CounterexampleInfo cex = CounterexampleInfo.feasible(allStatesTrace, pInfo.getAssignments());
    cex.addFurtherInformation(dumpCounterexample(pInfo), dumpCounterexampleFormula);
    cex.addFurtherInformation(dumpModel(pInfo.getModel()), dumpCounterexampleModel);
    return cex;
  }

  private Appender dumpModel(final Iterable<ValueAssignment> pModel) {
    final ImmutableList<ValueAssignment> model = Ordering.usingToString().immutableSortedCopy(pModel);
    return new AbstractAppender() {

      @Override
      public void appendTo(Appendable out) throws IOException {
        Joiner.on('\n').appendTo(out, model);
      }
    };
  }

  private Appender dumpCounterexample(CounterexampleTraceInfo cex) {
    FormulaManagerView fmgr = solver.getFormulaManager();
    return fmgr.dumpFormula(fmgr.getBooleanFormulaManager().and(cex.getCounterExampleFormulas()));
  }

  public CounterexampleTraceInfo checkPath(ARGPath pPath)
      throws SolverException, CPATransferException, InterruptedException {

    Pair<PathFormula, List<SSAMap>> result = createPrecisePathFormula(pPath);

    List<SSAMap> ssaMaps = result.getSecond();

    PathFormula pathFormula = result.getFirstNotNull();

    BooleanFormula f = pathFormula.getFormula();

    try (ProverEnvironment thmProver = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      thmProver.push(f);
      if (thmProver.isUnsat()) {
        return CounterexampleTraceInfo.infeasibleNoItp();
      } else {
        Iterable<ValueAssignment> model = getModel(thmProver);
        CFAPathWithAssumptions pathWithAssignments =
            assignmentToPathAllocator.allocateAssignmentsToPath(pPath, model, ssaMaps);

        return CounterexampleTraceInfo.feasible(ImmutableList.of(f), model, pathWithAssignments, ImmutableMap.<Integer, Boolean>of());
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

    for (CFAEdge edge : from(pPath.getInnerEdges()).filter(notNull())) {

      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
        for (CFAEdge singleEdge : (MultiEdge) edge) {
          pathFormula = pmgr.makeAnd(pathFormula, singleEdge);
          ssaMaps.add(pathFormula.getSsa());
        }
      } else {
        pathFormula = pmgr.makeAnd(pathFormula, edge);
        ssaMaps.add(pathFormula.getSsa());
      }
    }

    return Pair.of(pathFormula, ssaMaps);
  }

  private Iterable<ValueAssignment> getModel(ProverEnvironment thmProver) {
    try {
      return thmProver.getModel();
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Solver could not produce model, variable assignment of error path can not be dumped.");
      logger.logDebugException(e);
      return ImmutableList.of();
    }
  }
}