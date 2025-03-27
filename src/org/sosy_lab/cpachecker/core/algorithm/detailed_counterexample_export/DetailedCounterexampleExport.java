// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.detailed_counterexample_export;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.math.BigInteger;
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
import java.util.regex.Pattern;
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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "detailed_cex")
public class DetailedCounterexampleExport implements Algorithm {

  private final Algorithm algorithm;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier notifier;
  private final CFA cfa;

  @Option(
      secure = true,
      description = "File name for analysis report in case a counterexample was found.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate detailedCounterexample =
      PathTemplate.ofFormatString("Counterexample.variable.%d.txt");

  @Option(
      secure = true,
      name = "errorInputs",
      description = "export counterexample to file, if one is found")
  @FileOption(Type.OUTPUT_FILE)
  private Path exportErrorInputs = Path.of("nondets.txt");

  @Option(
      secure = true,
      description = "How many satisfying assignments to find per CEX; -1 for all")
  private int maxAssignments = 10;

  @Option(secure = true, description = "Export all assignments, not just nondet ones")
  private boolean exportAllAssignments = false;

  private int exportedPreciseCounterexamples = 0;
  private final AssumptionToEdgeAllocator allocator;

  record FormulaAndName(Formula formula, String name) {}

  record PathAndVariables(PathFormula path, ImmutableMap<FormulaAndName, CFAEdge> variables) {}

  /**
   * Exports concrete variable assignments of all variables at all locations. If there is a loop,
   * there are as many assignments as loop iterations for the specific location.
   *
   * @param pAlgorithm The base algorithm to find counterexamples.
   * @param pConfig The user configuration.
   * @param pLogger Manager for logging warnings.
   * @param pNotifier Notifier for user shut-down requests.
   * @param pCfa The CFA of the input program.
   * @throws InvalidConfigurationException Thrown if the configuration is invalid.
   */
  public DetailedCounterexampleExport(
      Algorithm pAlgorithm,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    algorithm = pAlgorithm;
    config = pConfig;
    logger = pLogger;
    notifier = pNotifier;
    cfa = pCfa;
    allocator = AssumptionToEdgeAllocator.create(config, logger, cfa.getMachineModel());
  }

  private List<Map<String, Object>> assignments(
      Solver solver, PathFormula pPathFormula, boolean pExportAllAssignments)
      throws SolverException, InterruptedException {
    BooleanFormula formulaWithModels = pPathFormula.getFormula();
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
          if (modelAssignment.getName().contains("__VERIFIER_nondet")) {
            // we are only interested in real variables
            continue;
          }
          String value = modelAssignment.getValue().toString();
          String variableName =
              solver.getFormulaManager().uninstantiate(modelAssignment.getKey()).toString();
          CType type = pPathFormula.getSsa().getType(variableName);
          if (Pattern.compile("-?\\d+").matcher(value).matches()
              && type instanceof CSimpleType simpleType) {
            value =
                PotentialOverflowHandler.handlePotentialIntegerOverflow(
                        allocator, new BigInteger(value), simpleType)
                    .map(v -> v.value().toString())
                    .orElse("");
          }
          BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
          if (formula.toString().contains("__VERIFIER_nondet") || pExportAllAssignments) {
            assignments = bmgr.and(assignments, formula);
            currentIteration.put(modelAssignment.getName(), value);
          }
        }
        formulaWithModels = bmgr.and(formulaWithModels, bmgr.not(assignments));
        allIterations.add(currentIteration.buildKeepingLast());
      }
    }
    return allIterations.build();
  }

  private PathAndVariables computeVariablesToEdgeMap(
      Solver solver, PathFormulaManager pmgr, CounterexampleInfo counterExample)
      throws CPATransferException, InterruptedException {
    PathFormula cexPath = pmgr.makeEmptyPathFormula();
    Set<FormulaAndName> before = ImmutableSet.of();
    ImmutableMap.Builder<FormulaAndName, CFAEdge> variableToLineNumber = ImmutableMap.builder();
    for (CFAEdge cfaEdge : counterExample.getTargetPath().getFullPath()) {
      cexPath = pmgr.makeAnd(cexPath, cfaEdge);
      Set<FormulaAndName> after =
          transformedImmutableSetCopy(
              solver.getFormulaManager().extractVariables(cexPath.getFormula()).entrySet(),
              entry -> new FormulaAndName(entry.getValue(), entry.getKey()));
      for (FormulaAndName variable : Sets.difference(after, before)) {
        variableToLineNumber.put(variable, cfaEdge);
      }
      before = after;
    }
    return new PathAndVariables(cexPath, variableToLineNumber.buildOrThrow());
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
      PathAndVariables cexPathAndVariables =
          computeVariablesToEdgeMap(solver, pmgr, counterExample);
      List<Map<String, Object>> assignments =
          assignments(solver, cexPathAndVariables.path(), exportAllAssignments);
      Multimap<String, String> variableToValues = ArrayListMultimap.create();
      for (Map<String, Object> assignment : assignments) {
        assignment.forEach((variable, value) -> variableToValues.put(variable, value.toString()));
      }
      ImmutableMap.Builder<String, CFAEdge> linesBuilder = ImmutableMap.builder();
      for (Entry<FormulaAndName, CFAEdge> formulaAndNameCFAEdgeEntry :
          cexPathAndVariables.variables().entrySet()) {
        linesBuilder.put(
            formulaAndNameCFAEdgeEntry.getKey().name(), formulaAndNameCFAEdgeEntry.getValue());
      }
      ImmutableMap<String, CFAEdge> lines = linesBuilder.buildOrThrow();
      for (Map<String, Object> assignment : assignments) {
        StringBuilder preciseCexExport = new StringBuilder();
        for (Entry<String, Object> variableValue : assignment.entrySet()) {
          String variable = variableValue.getKey();
          String value = variableValue.getValue().toString();
          if (lines.get(variable) == null) {
            logger.logf(
                Level.WARNING,
                "Variable %s does not appear in 'lines', skipping assignment %s",
                variable,
                value);
            continue;
          }
          CFAEdge cfaEdge = Objects.requireNonNull(lines.get(variable));
          if (variable.contains("::")) {
            variable = Splitter.on("::").limit(2).splitToList(variable).get(1);
          }
          if (variable.contains("@")) {
            variable = Splitter.on("@").limit(2).splitToList(variable).get(0);
          }
          preciseCexExport.append(cfaEdge).append('\n');
          preciseCexExport.append("  ").append(variable).append(" == ").append(value).append(";\n");
        }
        IO.writeFile(
            detailedCounterexample.getPath(exportedPreciseCounterexamples++),
            StandardCharsets.UTF_8,
            preciseCexExport.toString());
      }
      cexLinesBuilder.add(
          from(variableToValues.keySet())
              .transform(
                  variable ->
                      Objects.requireNonNull(lines.get(variable))
                              .getFileLocation()
                              .getStartingLineInOrigin()
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
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));
    try {
      exportErrorInducingInputs(counterExamples.toList());
    } catch (IOException | InvalidConfigurationException | SolverException e) {
      logger.logUserException(Level.WARNING, e, "Could not export counterexample inputs.");
    }
    return status;
  }
}
