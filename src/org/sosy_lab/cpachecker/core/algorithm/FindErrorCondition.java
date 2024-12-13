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
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
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
  private int maxIterations = -1; // Default: No limit

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

      // TODO: run until no new counterexamples are found!
      int currentIteration = 0;
      do {
        foundNewCounterexamples = false;

        // Run reachability analysis
        status = performReachabilityAnalysis(reachedSet, initialState, ssaBuilder, exclusionFormula,
            manager, solver, quantifierSolver);
        FluentIterable<CounterexampleInfo> counterExamples = getCounterexamples(reachedSet);

        // Collect counterexamples and refine
        if (!counterExamples.isEmpty()) {
          foundNewCounterexamples = true;
          for (CounterexampleInfo cex : counterExamples) {
            PathFormula cexFormula = manager.makeFormulaForPath(cex.getTargetPath().getFullPath());
            exclusionFormula =
                updateExclusionFormula(exclusionFormula, cexFormula, ssaBuilder, solver, manager,
                    quantifierSolver);
          }
          initialState = updateInitialStateWithExclusions(initialState, exclusionFormula);
        }

      } while (foundNewCounterexamples && (++currentIteration < maxIterations
          || maxIterations == -1));

      return status;

    } catch (InvalidConfigurationException | SolverException ex) {
      throw new CPAException("Error during the execution of FindErrorCondition", ex);
    }
  }

  // 1. get the initial state
  private AbstractState getInitialState() throws CPAException, InterruptedException {
    return cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
  }

  // 2. perform reachability analysis and handle counterexamples
  private AlgorithmStatus performReachabilityAnalysis(
      ReachedSet reachedSet,
      AbstractState initialState,
      SSAMapBuilder ssaBuilder,
      PathFormula exclude,
      PathFormulaManager manager,
      Solver solver,
      Solver quantifier_solver) throws CPAException, InterruptedException, SolverException {
    reachedSet.clear();
    reachedSet.add(initialState,
        cpa.getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()));

    AlgorithmStatus status = algorithm.run(reachedSet);

    FluentIterable<CounterexampleInfo> counterExamples = getCounterexamples(reachedSet);
    for (CounterexampleInfo cex : counterExamples) {
      PathFormula fullPath = manager.makeFormulaForPath(cex.getTargetPath().getFullPath());
      exclude =
          updateExclusionFormula(exclude, fullPath, ssaBuilder, solver, manager, quantifier_solver);
    }

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
      PathFormula exclude,
      PathFormula fullPath,
      SSAMapBuilder ssaBuilder,
      Solver solver,
      PathFormulaManager manager,
      Solver quantifier_solver) throws SolverException, InterruptedException {

    for (String variable : fullPath.getSsa().allVariables()) {
      if (!ssaBuilder.build().containsVariable(variable)) {
        ssaBuilder.setIndex(variable, fullPath.getSsa().getType(variable), 1);
      }
    }

    BooleanFormula eliminated_with_quantifier =
        eliminateVariables(quantifier_solver.getFormulaManager()
                .translateFrom(fullPath.getFormula(), solver.getFormulaManager()),
            entry -> !entry.getKey().contains("_nondet"),
            quantifier_solver);

    eliminated_with_quantifier = solver.getFormulaManager()
        .translateFrom(eliminated_with_quantifier, quantifier_solver.getFormulaManager());
    logger.log(Level.INFO, "Eliminated:" + eliminated_with_quantifier + "\n");
    eliminated_with_quantifier = solver.getFormulaManager().simplify(eliminated_with_quantifier);
    logger.log(Level.INFO, "Eliminated Simplified:" + eliminated_with_quantifier + "\n");
    //exclude path formula to ignore already covered paths.
    exclude = manager.makeAnd(exclude,
            solver.getFormulaManager().getBooleanFormulaManager().not(eliminated_with_quantifier))
        .withContext(ssaBuilder.build(), fullPath.getPointerTargetSet());

    String visitorOutput = formatErrorCondition(exclude.getFormula(), solver);
    logger.log(Level.INFO, "Error Condition: " + visitorOutput);
    return exclude;
  }

  // 5. eliminate variables matching a predicate (Quantifier Elimination )
  public BooleanFormula eliminateVariables(
      BooleanFormula pFormula,
      Predicate<Entry<String, Formula>> pVariablesToEliminate,
      Solver pSolver) throws InterruptedException, SolverException {
    Map<String, Formula> formulaNameToFormulaMap =
        pSolver.getFormulaManager().extractVariables(pFormula);
    ImmutableList<Formula> eliminate = FluentIterable.from(formulaNameToFormulaMap.entrySet())
        .filter(pVariablesToEliminate::test)
        .transform(Entry::getValue)
        .toList();
    BooleanFormula quantified = pSolver.getFormulaManager().getQuantifiedFormulaManager()
        .mkQuantifier(Quantifier.EXISTS, eliminate, pFormula);
    //logger.log(Level.INFO, "Eliminating variables: " + eliminate);
    //logger.log(Level.INFO, "Quantified variables: " + quantified);

    return pSolver.getFormulaManager().getQuantifiedFormulaManager()
        .eliminateQuantifiers(quantified);
  }

  // 6. Update the initial state with exclusion formulas for the next run
  private AbstractState updateInitialStateWithExclusions(
      AbstractState initialState,
      PathFormula exclude) {
    ImmutableList.Builder<AbstractState> initialAbstractStates = ImmutableList.builder();
    for (AbstractState abstractState : AbstractStates.asIterable(initialState)) {
      if (abstractState instanceof ARGState) {
        continue;
      }
      if (abstractState instanceof CompositeState) {
        continue;
      }
      if (abstractState instanceof PredicateAbstractState predicateState) {
        PersistentMap<CFANode, Integer> locations =
            predicateState.getAbstractionLocationsOnPath();
        initialAbstractStates.add(PredicateAbstractState.mkAbstractionState(exclude,
            predicateState.getAbstractionFormula(), locations));
      } else {
        initialAbstractStates.add(abstractState);
      }
    }
    return new ARGState(new CompositeState(initialAbstractStates.build()), null);
  }

  private String formatErrorCondition(BooleanFormula formula, Solver solver) {
    FormulaToCVisitor visitor = new FormulaToCVisitor(solver.getFormulaManager(), id -> id);
    solver.getFormulaManager().visit(formula, visitor);
    return visitor.getString()
        .replace("_nondet", "")
        .replace("bvadd_32", "+")
        .replace("bvsub_32", "-")
        .replace("bvslt_32", "<");
  }

  private BooleanFormula minimizeErrorCondition(BooleanFormula formula, Solver solver)
      throws SolverException, InterruptedException {
    return solver.getFormulaManager().simplify(formula);
  }

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
