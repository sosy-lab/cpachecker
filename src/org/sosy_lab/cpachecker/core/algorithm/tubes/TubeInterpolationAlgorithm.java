// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import com.google.common.collect.Iterables;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "tubes")
public class TubeInterpolationAlgorithm implements Algorithm {

  private final CFA cfa;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;
  private final Algorithm algorithm;

  @FileOption(Type.OUTPUT_FILE)
  @Option(
      secure = true,
      description = "where to write interpolation output")
  private final Path outputPath = Path.of("interpolants.txt");

  public TubeInterpolationAlgorithm(
      CFA pCfa,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger,
      Algorithm pAlgorithm) {
    cfa = pCfa;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    algorithm = pAlgorithm;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    ARGState lastState = (ARGState) reachedSet.getLastState();
    ARGPath argPath = ARGUtils.getOnePathTo(lastState);
    List<CFAEdge> originalPath = argPath.getFullPath();
    List<CFAEdge> fullPath = new ArrayList<>(originalPath);
    if (fullPath.isEmpty()) {
      return status;
    }
    CFAEdge current = fullPath.remove(fullPath.size() - 1);
    while (!(current instanceof CAssumeEdge a)) {
      current = fullPath.remove(fullPath.size() - 1);
    }
    CFAEdge finalCurrent = current;
    fullPath.add(
        Iterables.getOnlyElement(
            CFAUtils.leavingEdges(current.getPredecessor())
                .filter(e -> !e.equals(finalCurrent) && e instanceof CAssumeEdge)));
    try {
      Solver solver = Solver.create(config, logger, shutdownNotifier);
      PathFormulaManagerImpl manager =
          new PathFormulaManagerImpl(
              solver.getFormulaManager(),
              config,
              logger,
              shutdownNotifier,
              cfa,
              AnalysisDirection.FORWARD);
      FormulaContext context =
          new FormulaContext(solver, manager, cfa, logger, config, shutdownNotifier);
      PathFormula pathFormula = manager.makeFormulaForPath(originalPath);
      solver.getFormulaManager().dumpFormulaToFile(pathFormula.getFormula(), outputPath);
      TraceFormulaOptions options = new TraceFormulaOptions(config);
      Trace trace = Trace.fromCounterexample(fullPath, context, options);
      InterpolationManager interpolationManager =
          new InterpolationManager(
              manager,
              solver,
              Optional.empty(),
              Optional.empty(),
              config,
              shutdownNotifier,
              logger);
      List<BooleanFormula> interpolants =
          interpolationManager.interpolate(trace.toFormulaList()).orElseThrow();
      logger.log(Level.INFO, "Interpolants: ", interpolants);
    } catch (InvalidConfigurationException pE) {
      throw new CPAException("Invalid configuration", pE);
    }
    return status;
  }
}
