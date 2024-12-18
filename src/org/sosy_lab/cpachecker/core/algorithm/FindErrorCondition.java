// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaToCVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "findErrorCondition")
public class FindErrorCondition implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final CFA cfa;
  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;

  @Option(description = "Maximum iterations for error condition refinement.")
  private int maxIterations = -1; // Default: no iteration limit
  private final Map<String, String> variableMapping = new HashMap<>();


  public FindErrorCondition(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa) throws InvalidConfigurationException {
    pConfig.inject(this);
    algorithm = pAlgorithm;
    cpa = pCpa;
    cfa = pCfa;
    logger = pLogger;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;

  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Finding error condition...");

    try {
      AlgorithmStatus status = AlgorithmStatus.NO_PROPERTY_CHECKED;
      PredicateCPA predicateCPA = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, getClass());
      PathFormulaManager manager = predicateCPA.getPathFormulaManager();
      Solver solver = predicateCPA.getSolver();
      Solver quantifierSolver = Solver.create(
          Configuration.builder().copyFrom(config)
              .setOption("solver.solver", Solvers.Z3.name())
              .build()
          , logger, shutdownNotifier);

      // Initialize exclusion formula and iteration variables
      AbstractState initialState = getInitialState();
      SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
      PathFormula exclusionFormula = manager.makeEmptyPathFormula();
      boolean foundNewCounterexamples;
      int currentIteration = 0;

      logger.log(Level.INFO, "Entering While Loop...");
      do {
        logger.log(Level.INFO, "Iteration: " + currentIteration);

        foundNewCounterexamples = false;

        // Run reachability analysis
        status = performReachabilityAnalysis(reachedSet, initialState, currentIteration);
        // Collect counterexamples
        FluentIterable<CounterexampleInfo> counterExamples = getCounterexamples(reachedSet);
        logger.log(Level.INFO,
            String.format("Iteration %d: Found %d Counterexamples", currentIteration,
                counterExamples.size()));
        // Refinement
        if (!counterExamples.isEmpty()) {
          foundNewCounterexamples = true;
          logger.log(Level.INFO,
              String.format("Iteration %d: Entering For Loop...", currentIteration));
          for (CounterexampleInfo cex : counterExamples) {
            PathFormula cexFormula = manager.makeFormulaForPath(cex.getTargetPath().getFullPath());
            logger.log(Level.INFO,
                String.format("Iteration %d: Current CEX FORMULA: %s \n", currentIteration,
                    cexFormula.getFormula()));
            mapNonDetToOriginalNames(cexFormula, solver, currentIteration);
            exclusionFormula =
                updateExclusionFormula(exclusionFormula, cexFormula, ssaBuilder, solver, manager,
                    quantifierSolver, currentIteration);
          }
          initialState =
              updateInitialStateWithExclusions(initialState, exclusionFormula, currentIteration);
        }

      } while (foundNewCounterexamples && (++currentIteration < maxIterations
          || maxIterations == -1));

      return status;

    } catch (InvalidConfigurationException | SolverException ex) {
      throw new CPAException("Error during the execution of FindErrorCondition", ex);
    }
  }

  // 1. get the initial state
  private AbstractState getInitialState() throws InterruptedException {
    return cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
  }

  // 2. perform reachability analysis and handle counterexamples
  private AlgorithmStatus performReachabilityAnalysis(
      ReachedSet reachedSet,
      AbstractState initialState,
      Integer currentIteration) throws CPAException, InterruptedException, SolverException {

    reachedSet.clear();
    reachedSet.add(initialState,
        cpa.getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()));

    AlgorithmStatus status = algorithm.run(reachedSet);
    logger.log(Level.INFO, String.format(
        "Iteration %d: Performed Reachability Analysis: \n status: %s \n",
        currentIteration, status));

//    logger.log(Level.FINE, String.format(
//        "Iteration %d: Reached Set: \n : %s",
//        currentIteration, reachedSet));

    //    FluentIterable<CounterexampleInfo> counterExamples = getCounterexamples(reachedSet);
    //    for (CounterexampleInfo cex : counterExamples) {
    //      PathFormula fullPath = manager.makeFormulaForPath(cex.getTargetPath().getFullPath());
    //      exclusionFormula =
    //          updateExclusionFormula(exclusionFormula, fullPath, ssaBuilder, solver, manager, quantifier_solver);
    //    }

    return status;
  }

  // 3. collect counterexamples
  private FluentIterable<CounterexampleInfo> getCounterexamples(ReachedSet reachedSet) {
    return Optionals.presentInstances(
        from(reachedSet)
            .filter(AbstractStates::isTargetState)
            .filter(ARGState.class)
            .transform(ARGState::getCounterexampleInformation)
    );
  }

  // 4. update exclusion formula with counterexample information
  private PathFormula updateExclusionFormula(
      PathFormula exclusionFormula,
      PathFormula cexFormula,
      SSAMapBuilder ssaBuilder,
      Solver solver,
      PathFormulaManager manager,
      Solver quantifierSolver,
      Integer currentIteration) throws SolverException, InterruptedException {
    logger.log(Level.INFO,
        "******************************** Exclusion Formula Update ********************************");

    for (String variable : cexFormula.getSsa().allVariables()) {
      if (!ssaBuilder.build().containsVariable(variable)) {
        ssaBuilder.setIndex(variable, cexFormula.getSsa().getType(variable), 1);
      }
    }
    // formula translation between solvers, e.g. MATHSAT5 and Z3
    BooleanFormula translatedFormula = quantifierSolver.getFormulaManager().translateFrom(
        cexFormula.getFormula(), solver.getFormulaManager());
    logger.log(Level.INFO,
        String.format("Iteration %d: Translated Formula:\n%s \n", currentIteration,
            translatedFormula));

    BooleanFormula eliminatedByQuantifier = eliminateVariables(
        translatedFormula,
        // this predicate filters out variables (keys) that include "_nondet" in their names
        entry -> !entry.getKey().contains("_nondet"),
        // this predicate keeps the non-det variables
        entry -> entry.getKey().contains("_nondet"),
        quantifierSolver, currentIteration);

    // translate back
    eliminatedByQuantifier = solver.getFormulaManager()
        .translateFrom(eliminatedByQuantifier, quantifierSolver.getFormulaManager());
    logger.log(Level.INFO,
        String.format("Iteration %d: To Be Eliminated (excluded): %s \n", currentIteration,
            eliminatedByQuantifier));


    //update exclusion formula with the new quantified variables from this iteration.
    exclusionFormula = manager.makeAnd(exclusionFormula,
            solver.getFormulaManager().getBooleanFormulaManager().not(eliminatedByQuantifier))
        .withContext(ssaBuilder.build(), cexFormula.getPointerTargetSet());

    logger.log(Level.INFO,
        String.format("Iteration %d: Updated Exclusion Formula: %s \n", currentIteration,
            exclusionFormula.getFormula()));

    String formattedErrorCondition = formatErrorCondition(exclusionFormula.getFormula(), solver);
    logger.log(Level.INFO,
        String.format("Iteration %d: Error Condition in this iteration: %s \n", currentIteration,
            formattedErrorCondition));
    return exclusionFormula;
  }

  private void mapNonDetToOriginalNames(
      PathFormula cexFormula,
      Solver solver,
      Integer currentIteration) {
    List<String> cexVarNames = new ArrayList<>(
        solver.getFormulaManager().extractVariableNames(cexFormula.getFormula()));

    // Map SSA variable names to original names (e.g., `__VERIFIER_int!2@` -> `x`)
    for (String cexVarName : cexVarNames) {
      if (cexVarName.contains("_nondet") && !variableMapping.containsKey(cexVarName)) {
        int index = cexVarNames.indexOf(cexVarName);
        variableMapping.put(cexVarName, cexVarNames.get(index - 1));
      }
    }
    logger.log(Level.INFO, String.format("Iteration %d: CEX ALL VARS: %s", currentIteration,
        cexFormula.getSsa().allVariables()));
    logger.log(Level.INFO,
        String.format("Iteration %d: CEX EXTRACTED VAR NAMES: %s", currentIteration,
            solver.getFormulaManager().extractVariableNames(
                cexFormula.getFormula())));
    logger.log(Level.INFO,
        String.format("Iteration %d: CEX Non-Det Variables Mapping: %s", currentIteration,
            variableMapping));
  }


  // 5. eliminate variables matching a predicate (Quantifier Elimination)
  public BooleanFormula eliminateVariables(
      BooleanFormula translatedFormula,
      Predicate<Entry<String, Formula>> deterministicVariablesPredicate,
      Predicate<Entry<String, Formula>> nonDetVariablesPredicate,
      Solver quantifierSolver,
      Integer currentIteration
  ) throws InterruptedException, SolverException {

    logger.log(Level.INFO,
        "******************************** Quantifier Elimination ********************************");

    Map<String, Formula> formulaNameToFormulaMap =
        quantifierSolver.getFormulaManager().extractVariables(translatedFormula);
    logger.log(Level.INFO,
        String.format("Iteration %d: formulaNameToFormulaMap: %s", currentIteration,
            formulaNameToFormulaMap.entrySet()));

    ImmutableList<Formula> eliminate = FluentIterable.from(formulaNameToFormulaMap.entrySet())
        .filter(deterministicVariablesPredicate::test)
        .transform(Entry::getValue)
        .toList();

    ImmutableList<Formula> nonDetVariables = FluentIterable.from(formulaNameToFormulaMap.entrySet())
        .filter(nonDetVariablesPredicate::test)
        .transform(Entry::getValue)
        .toList();


    logger.log(Level.INFO,
        String.format("Iteration %d: Deterministic variables to eliminate: %s", currentIteration,
            eliminate));
    logger.log(Level.INFO,
        String.format("Iteration %d: Non-Deterministic variables : %s", currentIteration,
            nonDetVariables));

    BooleanFormula quantified = quantifierSolver.getFormulaManager().getQuantifiedFormulaManager()
        .mkQuantifier(Quantifier.EXISTS, eliminate, translatedFormula);
    logger.log(Level.INFO,
        String.format("Iteration %d: Quantified deterministic variables: \n%s", currentIteration,
            quantified));

    return quantifierSolver.getFormulaManager().getQuantifiedFormulaManager()
        .eliminateQuantifiers(quantified);
  }

  // 6. Update the initial state with exclusion formulas for the next run
  private AbstractState updateInitialStateWithExclusions(
      AbstractState initialState,
      PathFormula exclusionFormula,
      Integer currentIteration) {
    ImmutableList.Builder<AbstractState> initialAbstractStates = ImmutableList.builder();
    for (AbstractState abstractState : AbstractStates.asIterable(initialState)) {
      if (abstractState instanceof ARGState) {
        // TODO handle ARGState instances
        continue;
      }
      if (abstractState instanceof CompositeState) {
        // TODO handle CompositeState instances
        continue;
      }
      if (abstractState instanceof PredicateAbstractState predicateState) {
        PersistentMap<CFANode, Integer> locations =
            predicateState.getAbstractionLocationsOnPath();
        initialAbstractStates.add(PredicateAbstractState.mkAbstractionState(exclusionFormula,
            predicateState.getAbstractionFormula(), locations));
      } else {
        initialAbstractStates.add(abstractState);
      }
    }
    logger.log(Level.INFO,
        String.format(
            "Iteration %d: Updated initial state with the exclusion formula for next iteration.",
            currentIteration));
    logger.log(Level.FINE, String.format("Iteration %s: Updated initial state: ", initialState));
    return new ARGState(new CompositeState(initialAbstractStates.build()), null);
  }

  private String formatErrorCondition(BooleanFormula formula, Solver solver)
      throws InterruptedException {
    FormulaToCVisitor visitor = new FormulaToCVisitor(solver.getFormulaManager(), id -> id);
    BooleanFormula simplifiedFormula = solver.getFormulaManager().simplify(formula);
    solver.getFormulaManager().visit(simplifiedFormula, visitor);

    String rawFormula = visitor.getString();

    // Clean up nondet annotations
    for (Map.Entry<String, String> entry : variableMapping.entrySet()) {
      String ssaVariable = entry.getKey();
      String originalName = entry.getValue().replace("main::", "");
      rawFormula = rawFormula.replace(ssaVariable, originalName);
    }

    return rawFormula;
  }

//  private void minimizeErrorCondition(BooleanFormula formula, Solver solver)
//      throws SolverException, InterruptedException {
//    // TODO minimize Error condition remove redundant expressions
//  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    //TODO add way for printing statistics
  }

  @Override
  public @Nullable String getName() {
    return "FindErrorCondition";
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    //TODO add way for collecting statistics
  }

}
