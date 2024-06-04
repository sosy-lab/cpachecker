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
import com.google.common.base.Preconditions;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
import org.sosy_lab.common.io.PathTemplate;
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

@Options(prefix = "tubes")
public class CounterexampleToC implements Algorithm {

  enum Action {
    EXPORT_CEX_TO_C,
    EXPORT_CEX_INPUTS
  }

  private final Algorithm algorithm;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier notifier;
  private final CFA cfa;

  @Option(
      secure = true,
      name = "errorInputs",
      description = "export counterexample to file, if one is found")
  @FileOption(Type.OUTPUT_FILE)
  private Path exportErrorInputs = Path.of("nondets.txt");

  @Option(
      secure = true,
      name = "counterExampleFiles",
      description = "File name for analysis report in case a counterexample was found.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate counterExamplePerVariable =
      PathTemplate.ofFormatString("Counterexample.variable.%d.txt");

  @Option(
      secure = true,
      name = "counterExampleFiles",
      description = "File name for analysis report in case a counterexample was found.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate counterExampleFiles =
      PathTemplate.ofFormatString("Counterexample.inline.%d.c");

  @Option(secure = true, name = "actions", description = "Actions to perform")
  private List<Action> actions = ImmutableList.of(Action.EXPORT_CEX_TO_C);

  @Option(
      secure = true,
      description = "How many satisfying assignments to find per CEX; -1 for all")
  private int maxAssignments = 10;

  @Option(secure = true, description = "Export all assignments, not just nondet ones")
  private boolean exportAllAssignments = false;

  private int exportedCounterexamples = 0;
  private int exportedPreciseCounterexamples = 0;

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
    if (exportErrorInputs == null) {
      throw new InvalidConfigurationException(
          "Counterexample export path not specified. Please set the option"
              + " cexToC.exportPath=<path>");
    }
    try {
      Files.write(exportErrorInputs, new byte[0]);
    } catch (IOException ioException) {
      throw new CPAException("Cannot write file " + exportErrorInputs, ioException);
    }
  }

  private void convertCounterexamplesToC(Collection<CounterexampleInfo> counterexamples)
      throws IOException {
    for (CounterexampleInfo counterexample : counterexamples) {
      CounterexampleToCodeVisitor visitor = new CounterexampleToCodeVisitor();
      String cCode = visitor.visit(counterexample);
      IO.writeFile(
          counterExampleFiles.getPath(exportedCounterexamples++), StandardCharsets.UTF_8, cCode);
    }
  }

  private List<Map<String, Object>> assignments(
      Solver solver, BooleanFormula pBooleanFormula, boolean pExportAllAssignments)
      throws SolverException, InterruptedException {
    BooleanFormula formulaWithModels = pBooleanFormula;
    BooleanFormulaManagerView bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    ImmutableList.Builder<Map<String, Object>> allIterations = ImmutableList.builder();
    int generatedAssignments = 0;
    while (true) {
      ImmutableMap.Builder<String, Object> currentIteration = ImmutableMap.builder();
      try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        prover.push(formulaWithModels);
        if (prover.isUnsat() || generatedAssignments == maxAssignments) {
          break;
        }
        generatedAssignments++;
        BooleanFormula assignments = bmgr.makeTrue();
        for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
          BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
          if (formula.toString().contains("__VERIFIER_nondet") || pExportAllAssignments) {
            assignments = bmgr.and(assignments, formula);
            currentIteration.put(modelAssignment.getName(), modelAssignment.getValue());
          }
        }
        formulaWithModels = bmgr.and(formulaWithModels, bmgr.not(assignments));
        allIterations.add(currentIteration.buildOrThrow());
      }
    }
    return allIterations.build();
  }

  private void exportErrorInducingInputs(Collection<CounterexampleInfo> counterExamples)
      throws CPAException,
          InterruptedException,
          InvalidConfigurationException,
          SolverException,
          IOException {
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
      List<Map<String, Object>> assignments =
          assignments(solver, cexPath.getFormula(), exportAllAssignments);
      Multimap<String, String> variableToValues = ArrayListMultimap.create();
      for (Map<String, Object> assignment : assignments) {
        assignment.forEach((variable, value) -> variableToValues.put(variable, value.toString()));
      }
      ImmutableMap<String, Integer> lines = variableToLineNumber.buildOrThrow();
      for (Map<String, Object> assignment : assignments) {
        StringBuilder preciseCexExport = new StringBuilder();
        for (Entry<String, Object> variableValue : assignment.entrySet()) {
          String variable = variableValue.getKey();
          String value = variableValue.getValue().toString();
          int line = Objects.requireNonNull(lines.get(variable));
          preciseCexExport.append(String.format("line %d: %s == %s\n", line, variable, value));
        }
        IO.writeFile(
            counterExamplePerVariable.getPath(exportedPreciseCounterexamples++),
            StandardCharsets.UTF_8,
            preciseCexExport.toString());
      }
      cexLinesBuilder.add(
          from(variableToValues.keySet())
              .transform(
                  variable ->
                      lines.get(variable)
                          + " "
                          + Joiner.on(" ").join(variableToValues.get(variable)))
              .join(Joiner.on(" & ")));
    }
    ImmutableList<String> cexLines = cexLinesBuilder.build();
    if (!cexLines.isEmpty()) {
      if (exportErrorInputs.toFile().exists()) {
        cexLinesBuilder.addAll(Files.readAllLines(exportErrorInputs, StandardCharsets.UTF_8));
      }
      IO.writeFile(
          exportErrorInputs,
          StandardCharsets.UTF_8,
          Joiner.on("\n").join(ImmutableSet.copyOf(cexLinesBuilder.build())) + "\n");
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    Preconditions.checkState(
        ImmutableSet.copyOf(actions).size() == actions.size(), "Duplicate actions");
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));
    for (Action action : ImmutableSet.copyOf(actions)) {
      switch (action) {
        case EXPORT_CEX_TO_C:
          try {
            convertCounterexamplesToC(counterExamples.toList());
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not export counterexample to C");
          }
          break;
        case EXPORT_CEX_INPUTS:
          try {
            exportErrorInducingInputs(counterExamples.toList());
          } catch (IOException | InvalidConfigurationException | SolverException e) {
            logger.logUserException(Level.WARNING, e, "Could not export counterexample inputs");
          }
          break;
      }
    }
    return status;
  }
}
