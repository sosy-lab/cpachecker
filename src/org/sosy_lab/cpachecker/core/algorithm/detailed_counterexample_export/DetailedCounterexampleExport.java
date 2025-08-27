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

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
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
      description = "How many satisfying assignments to find per CEX; -1 for all")
  private int maxAssignments = 10;

  record FormulaAndName(Formula formula, String name) {}

  record PathAliasesAndVariables(
      PathFormula path,
      ImmutableMap<String, String> aliases,
      ImmutableMap<FormulaAndName, CFAEdge> variables) {}

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
  }

  /**
   * Finds all satisfying assignments for the given path formula. The number of assignments is
   * limited by the maxAssignments parameter. If maxAssignments is -1, all satisfying assignments
   * are found.
   *
   * @param solver The solver to use for finding assignments.
   * @param pPathFormula The path formula for which to find assignments.
   * @return A list of maps, where each map contains variable names and their corresponding values.
   * @throws SolverException Thrown if the solver encounters an error.
   * @throws InterruptedException Thrown if the operation is interrupted.
   * @throws InvalidConfigurationException Thrown if the AssumptionAllocator configuration is
   *     invalid.
   */
  private List<Map<String, Object>> assignments(Solver solver, PathFormula pPathFormula)
      throws SolverException, InterruptedException, InvalidConfigurationException {
    AssumptionToEdgeAllocator allocator =
        AssumptionToEdgeAllocator.create(config, logger, cfa.getMachineModel());
    BooleanFormulaManagerView bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    ImmutableList.Builder<Map<String, Object>> allIterations = ImmutableList.builder();
    int generatedAssignments = 0;
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(pPathFormula.getFormula());
      while (true) {
        // maps the variable name to the value
        ImmutableMap.Builder<String, Object> currentIteration = ImmutableMap.builder();
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
          if (type instanceof CSimpleType simpleType) {
            value = allocator.convert(modelAssignment.getValue(), simpleType).toString();
          }
          BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
          if (!formula.toString().contains("__VERIFIER_nondet")) {
            assignments = bmgr.and(assignments, formula);
            currentIteration.put(modelAssignment.getName(), value);
          }
        }
        // incremental solver usage
        prover.push(bmgr.not(assignments));
        allIterations.add(currentIteration.buildKeepingLast());
      }
    }
    return allIterations.build();
  }

  /**
   * Computes the mapping of SMT variables to their corresponding CFA edges in the counterexample
   * path. This is done by iterating over the edges in the counterexample path and extracting the
   * variables from the path formula. The mapping is built by comparing the variables before and
   * after each edge.
   *
   * @param solver The solver to use for extracting variables.
   * @param pmgr The path formula manager to use for creating path formulas.
   * @param counterExample The counterexample containing the path.
   * @return A PathAliasesAndVariables object containing the path formula and the mapping of
   *     variables to edges.
   * @throws CPATransferException Thrown if there is an error in the transfer relation.
   * @throws InterruptedException Thrown if the operation is interrupted.
   */
  private PathAliasesAndVariables computeVariablesToEdgeMap(
      Solver solver, PathFormulaManager pmgr, CounterexampleInfo counterExample)
      throws CPATransferException, InterruptedException {
    ImmutableMap.Builder<String, String> aliases = ImmutableMap.builder();
    PathFormula cexPath = pmgr.makeEmptyPathFormula();
    Set<FormulaAndName> before = ImmutableSet.of();
    ImmutableMap.Builder<FormulaAndName, CFAEdge> variableToLineNumber = ImmutableMap.builder();
    for (CFAEdgeWithAssumptions cfaEdgeWithAssumptions :
        counterExample.getCFAPathWithAssignments()) {
      CFAEdge cfaEdge = cfaEdgeWithAssumptions.getCFAEdge();
      cexPath = pmgr.makeAnd(cexPath, cfaEdge);

      if (maxAssignments == 1) {
        for (AExpressionStatement aExpressionStatement : cfaEdgeWithAssumptions.getExpStmts()) {
          if (aExpressionStatement instanceof CExpressionStatement pCExpressionStatement) {
            cexPath = pmgr.makeAnd(cexPath, pCExpressionStatement.getExpression());
          }
        }
      }

      Set<FormulaAndName> after =
          transformedImmutableSetCopy(
              solver.getFormulaManager().extractVariables(cexPath.getFormula()).entrySet(),
              entry -> new FormulaAndName(entry.getValue(), entry.getKey()));
      for (FormulaAndName variable : Sets.difference(after, before)) {
        variableToLineNumber.put(variable, cfaEdge);
        if (variable.name().startsWith("*")
            && (cfaEdge.getEdgeType() == CFAEdgeType.StatementEdge
                || cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge)) {
          if (cfaEdge.getRawStatement().contains(" = ")) {
            aliases.put(
                variable.name(),
                Splitter.on(" = ").splitToList(cfaEdge.getRawStatement()).get(0).trim());
          }
        }
      }
      before = after;
    }
    return new PathAliasesAndVariables(
        cexPath, aliases.buildKeepingLast(), variableToLineNumber.buildOrThrow());
  }

  public void exportErrorInducingInputs(CounterexampleInfo counterExample, Path pCounterexamplePath)
      throws InvalidConfigurationException,
          SolverException,
          InterruptedException,
          CPATransferException,
          IOException {
    Solver solver = Solver.create(config, logger, notifier);
    PathFormulaManager pmgr =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(), config, logger, notifier, cfa, AnalysisDirection.FORWARD);
    PathAliasesAndVariables cexPathAliasesAndVariables =
        computeVariablesToEdgeMap(solver, pmgr, counterExample);
    List<Map<String, Object>> assignments = assignments(solver, cexPathAliasesAndVariables.path());
    Multimap<String, String> variableToValues = ArrayListMultimap.create();
    for (Map<String, Object> assignment : assignments) {
      assignment.forEach((variable, value) -> variableToValues.put(variable, value.toString()));
    }
    ImmutableMap.Builder<String, CFAEdge> linesBuilder = ImmutableMap.builder();
    for (Entry<FormulaAndName, CFAEdge> formulaAndNameCFAEdgeEntry :
        cexPathAliasesAndVariables.variables().entrySet()) {
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
        variable = cexPathAliasesAndVariables.aliases().getOrDefault(variable, variable);
        if (variable.contains("::")) {
          variable = Splitter.on("::").limit(2).splitToList(variable).get(1);
        }
        if (variable.contains("@")) {
          variable = Splitter.on("@").limit(2).splitToList(variable).get(0);
        }
        preciseCexExport
            .append("Functions: ")
            .append(cfaEdge.getSuccessor().getFunction())
            .append("\n")
            .append(cfaEdge)
            .append('\n')
            .append("  ")
            .append(variable)
            .append(" == ")
            .append(value)
            .append(";\n");
      }
      IO.writeFile(pCounterexamplePath, StandardCharsets.UTF_8, preciseCexExport.toString());
    }
  }

  /**
   * Exports a detailed counterexample to a file. The counterexample is exported in a format that
   * includes the variable assignments at each location in the counterexample path. The
   * counterexample is exported to a file specified by the detailedCounterexample option.
   *
   * @param counterExamples The collection of counterexamples to export.
   * @param pCounterexamplePath The path template for the counterexample file. Will be instantiated
   *     with the number of the CEX being exported.
   */
  public void exportManyErrorInducingInputs(
      Collection<CounterexampleInfo> counterExamples, PathTemplate pCounterexamplePath)
      throws CPAException,
          InterruptedException,
          InvalidConfigurationException,
          SolverException,
          IOException {
    int cexCount = 0;

    for (CounterexampleInfo counterExample : counterExamples) {
      Path counterexamplePath = pCounterexamplePath.getPath(cexCount++);
      exportErrorInducingInputs(counterExample, counterexamplePath);
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
      exportManyErrorInducingInputs(counterExamples.toList(), detailedCounterexample);
    } catch (IOException | InvalidConfigurationException | SolverException e) {
      logger.logUserException(Level.WARNING, e, "Could not export counterexample inputs.");
    }
    return status;
  }
}
