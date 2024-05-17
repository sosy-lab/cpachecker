// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options
public class CounterexampleToC implements Algorithm {

  private final Algorithm algorithm;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier notifier;
  private final CFA cfa;

  @Option(
      secure = true,
      name = "cexToC.exportPath",
      description = "export counterexample to file, if one is found")
  @FileOption(Type.OUTPUT_FILE)
  private Path exportErrorPath = Path.of("nondets.txt");

  public CounterexampleToC(
      Algorithm pAlgorithm,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pNotifier,
      CFA pCfa)
      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);
    algorithm = pAlgorithm;
    config = pConfig;
    logger = pLogger;
    notifier = pNotifier;
    cfa = pCfa;
    if (exportErrorPath == null) {
      throw new InvalidConfigurationException(
          "Counterexample export path not specified. Please set the option cexToC.exportPath=<path>");
    }
    try {
      Files.write(exportErrorPath, new byte[0]);
    } catch (IOException pE) {
      throw new CPAException("Cannot write file " + exportErrorPath, pE);
    }
  }

  public String convertCounterexampleToC(CounterexampleInfo counterexample) {
    CounterexampleToCodeVisitor visitor = new CounterexampleToCodeVisitor();
    counterexample
        .getTargetPath()
        .getFullPath()
        .forEach(edge -> edge.getRawAST().ifPresent(ast -> ast.accept_(visitor)));
    return visitor.finish();
  }

  private List<Map<String, Object>> assignments(Solver solver, BooleanFormula pBooleanFormula)
      throws SolverException, InterruptedException {
    BooleanFormula formulaWithModels = pBooleanFormula;
    BooleanFormulaManagerView bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    ImmutableList.Builder<Map<String, Object>> allIterations = ImmutableList.builder();
    while (true) {
      ImmutableMap.Builder<String, Object> currentIteration = ImmutableMap.builder();
      try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        prover.push(formulaWithModels);
        if (prover.isUnsat()) {
          break;
        }
        for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
          BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
          if (formula.toString().contains("__VERIFIER_nondet")) {
            formulaWithModels = bmgr.and(formulaWithModels, bmgr.not(formula));
            currentIteration.put(modelAssignment.getName(), modelAssignment.getValue());
          }
        }
        allIterations.add(currentIteration.buildOrThrow());
      }
    }
    return allIterations.build();
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));
    try {
      Solver solver = Solver.create(config, logger, notifier);
      PathFormulaManager pmgr =
          new PathFormulaManagerImpl(
              solver.getFormulaManager(), config, logger, notifier, cfa, AnalysisDirection.FORWARD);
      ImmutableList.Builder<String> cexLinesBuilder = ImmutableList.builder();
      for (CounterexampleInfo counterExample : counterExamples) {
        PathFormula cexPath = pmgr.makeEmptyPathFormula();
        Set<String> before = ImmutableSet.of();
        ImmutableMap.Builder<String, Integer> variableToLineNumber = ImmutableMap.builder();
        for (CFAEdge cfaEdge : counterExample.getTargetPath().getFullPath()) {
          cexPath = pmgr.makeAnd(cexPath, cfaEdge);
          Set<String> after =
              solver.getFormulaManager().extractVariables(cexPath.getFormula()).keySet();
          for (String variable : Sets.difference(after, before)) {
            variableToLineNumber.put(variable, cfaEdge.getFileLocation().getStartingLineNumber());
          }
          before = after;
        }
        List<Map<String, Object>> assignments = assignments(solver, cexPath.getFormula());
        Multimap<String, String> variableToValues = ArrayListMultimap.create();
        for (Map<String, Object> assignment : assignments) {
          assignment.forEach((variable, value) -> variableToValues.put(variable, value.toString()));
        }
        ImmutableMap<String, Integer> lines = variableToLineNumber.buildOrThrow();
        cexLinesBuilder.add(
            from(variableToValues.keys())
                .transform(
                    variable ->
                        lines.get(variable)
                            + " "
                            + Joiner.on("\n").join(variableToValues.get(variable)))
                .join(Joiner.on(" & ")));
        logger.log(Level.INFO, convertCounterexampleToC(counterExample));
      }
      ImmutableList<String> cexLines = cexLinesBuilder.build();
      if (!cexLines.isEmpty()) {
        IO.appendToFile(
            exportErrorPath, StandardCharsets.UTF_8, Joiner.on(",\n").join(cexLines) + "\n");
      }
    } catch (InvalidConfigurationException pE) {
      throw new CPAException("Invalid configuration", pE);
    } catch (SolverException pE) {
      throw new CPAException("Invalid solver calls", pE);
    } catch (IOException pE) {
      throw new CPAException("Cannot write file " + exportErrorPath, pE);
    }
    return status;
  }
}
