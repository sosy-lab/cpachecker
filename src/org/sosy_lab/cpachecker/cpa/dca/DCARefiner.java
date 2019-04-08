/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.dca;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.termination.ClassVariables;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder.Dnf;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder.StemAndLoop;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.StronglyConnectedComponent;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.basicimpl.AbstractFormulaManager;

@Options(prefix = "cpa.dca.refiner")
public class DCARefiner implements Refiner, StatisticsProvider {

  private static final Solvers SMTINTERPOL = Solvers.SMTINTERPOL;

  @SuppressWarnings("unused")
  private final ARGCPA argCpa;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final DCAStatistics statistics = new DCAStatistics();

  private final PathFormulaManager pathFormulaManager;
  private final ClassVariables variables;
  private final LassoAnalysis lassoAnalysis;
  private final LassoBuilder lassoBuilder;
  private final Solver solver;
  private final AbstractFormulaManager<Term, ?, ?, ?> formulaManager;
  private final FormulaManagerView formulaManagerView;
  private final InterpolationManager interpolationManager;

  @Option(
    secure = false,
    name = "skipRefinement",
    description = "Allows to abort the refinement early in order to only produce the ARG for debugging purposes")
  private boolean skipRefinement = false;

  private ARGState rootState;

  @SuppressWarnings({"resource", "unchecked"})
  private DCARefiner(
      final ARGCPA pArgCpa,
      final CFA pCfa,
      final LogManager pLogger,
      final ShutdownNotifier pNotifier,
      final Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    argCpa = checkNotNull(pArgCpa);
    cfa = checkNotNull(pCfa);
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pNotifier);

    variables = ClassVariables.collectDeclarations(cfa);

    SolverContext solverContext =
        SolverContextFactory.createSolverContext(pConfig, pLogger, pNotifier, SMTINTERPOL);
    formulaManager = (AbstractFormulaManager<Term, ?, ?, ?>) solverContext.getFormulaManager();
    SolverContextFactory solverContextFactory =
        new SolverContextFactory(pConfig, pLogger, pNotifier);
    solver = Solver.create(solverContextFactory, SMTINTERPOL, solverContext, pConfig, pLogger);
    formulaManagerView = solver.getFormulaManager();
    pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManagerView,
            pConfig,
            pLogger,
            pNotifier,
            pCfa,
            AnalysisDirection.FORWARD);

    Supplier<ProverEnvironment> proverEnvironmentSupplier = () -> solver.newProverEnvironment();
    lassoBuilder =
        new LassoBuilder(
            pConfig,
            pLogger,
            pNotifier,
            formulaManager,
            formulaManagerView,
            proverEnvironmentSupplier,
            pathFormulaManager,
            statistics);
    RankingRelationBuilder rankingRelationBuilder =
        new RankingRelationBuilder(
            pCfa.getMachineModel(),
            pLogger,
            formulaManagerView,
            formulaManager.getFormulaCreator());
    lassoAnalysis =
        LassoAnalysis.create(
            lassoBuilder,
            rankingRelationBuilder,
            solverContext,
            pLogger,
            pConfig,
            pNotifier,
            statistics);

    interpolationManager =
        new InterpolationManager(
            pathFormulaManager,
            solver,
            cfa.getLoopStructure(),
            cfa.getVarClassification(),
            pConfig,
            pNotifier,
            pLogger);
  }

  @SuppressWarnings("resource")
  public static Refiner create(
      final ConfigurableProgramAnalysis pCpa,
      final LogManager pLogger,
      final ShutdownNotifier pNotifier)
      throws InvalidConfigurationException {

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, DCARefiner.class);
    PredicateCPA predicateCpa = CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, DCARefiner.class);

    Configuration config = predicateCpa.getConfiguration();
    CFA cfa = predicateCpa.getCfa();

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(config).setOption("solver.solver", SMTINTERPOL.name());

    return new DCARefiner(argCpa, cfa, pLogger, pNotifier, configBuilder.build());
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached)
      throws CPAException, InterruptedException {
    if (skipRefinement) {
      logger.logf(Level.WARNING, "Received flag to skip the refinement. Aborting analysis...");
      return false;
    }

    statistics.argUpdate.start();
    try {
      performRefinement0(pReached);
    } finally {
      statistics.argUpdate.stop();
    }
    return false;
  }

  private void performRefinement0(final ReachedSet pReached)
      throws InterruptedException, CPAException {

    rootState = (ARGState) pReached.getFirstState();

    while (true) {
      shutdownNotifier.shutdownIfNecessary();

      // Retrieve all SCCs from the reached set that contain at least one target state
      Set<StronglyConnectedComponent> SCCs =
          ARGUtils.retrieveSCCs(pReached)
              .stream()
              .filter(x -> x.getNodes().size() > 1)
              .filter(x -> x.hasTargetStates())
              .collect(ImmutableSet.toImmutableSet());

      if (SCCs.isEmpty()) {
        // Reached set does not contain any cycles at all
        // --> if ltl-prop is a liveness-prop, we're done here; i.e. program is safe
        ARGState lastState = (ARGState) pReached.getLastState();
        if (lastState == null) {
          return;
        }
        checkNotNull(lastState);
        try {
          // boolean feasiblePrefix = hasFeasibleFinitePrefixFromTo(rootState, lastState);
          hasFeasibleFinitePrefixFromTo(rootState, lastState);
          // TODO: handle result (i.e. sat or unsat)
          return;
        } catch (SolverException e) {
          logger.logfException(Level.SEVERE, e, e.getMessage());
          throw new CPAException(e.getMessage(), e);
        }
      }

      if (!SCCs.isEmpty() && SCCs.stream().anyMatch(x -> !x.hasTargetStates())) {
        // The SCCs in the ARG don't have a target state
        // --> if ltl-prop is a liveness-prop, we're done here; i.e. program is safe
        // TODO: otherwise - handle arg that has cycles, but none with a target state
        ImmutableList<StronglyConnectedComponent> SCCsWithoutTarget =
            SCCs.stream()
                .filter(x -> !x.hasTargetStates())
                .collect(ImmutableList.toImmutableList());
        for (StronglyConnectedComponent scc : SCCsWithoutTarget) {

          List<List<ARGState>> cycles =
              ARGUtils.retrieveSimpleCycles(
                  scc.getNodes(),
                  Optional.of(
                      pReached.asCollection()
                          .stream()
                          .map(x -> (ARGState) x)
                          .collect(Collectors.toCollection(HashSet::new))));
          for (List<ARGState> cycle : cycles) {
            logger.log(
                Level.WARNING,
                String.format("Cycle without target-states in ssc: %s\n", printNodes(cycle)));
          }
        }
        return;
      }

      for (StronglyConnectedComponent scc : SCCs) {

        shutdownNotifier.shutdownIfNecessary();
        List<List<ARGState>> sscCycles = ARGUtils.retrieveSimpleCycles(scc.getNodes());

        for (List<ARGState> cycle : sscCycles) {
          logger.log(Level.WARNING, String.format("Found cycle in scc: %s\n", printNodes(cycle)));

          // Path to the first ARG-node in the cycle-set
          ARGState firstNodeInCycle = cycle.iterator().next();

          shutdownNotifier.shutdownIfNecessary();
          ARGPath stemPath = ARGUtils.getShortestPathTo(firstNodeInCycle);
          ARGPath loopPath = new ARGPath(cycle);
          if (!loopPath.asStatesList().equals(cycle)) {
            throw new RuntimeException(
                String.format(
                    "Nodes in cycle are not consistent to nodes in the ARGPath"
                        + "\n(Nodes in cycle: %s)"
                        + "\n(Nodes in loop: %s)",
                    printNodes(cycle),
                    printNodes(loopPath)));
          }
          logger.log(Level.WARNING, String.format("Path to cycle: %s\n", printNodes(stemPath)));

          try {
            // create stem
            List<PathFormula> stemPathFormulaList =
                SlicingAbstractionsUtils.getFormulasForPath(
                    pathFormulaManager,
                    solver,
                    rootState,
                    stemPath.asStatesList(),
                    false);
            PathFormula stemPathFormula = createSinglePathFormula(stemPathFormulaList);

            // check stem prefixes for infeasibility
            ImmutableList<BooleanFormula> stemBFList =
                stemPathFormulaList.stream()
                    .map(x -> x.getFormula())
                    .collect(ImmutableList.toImmutableList());
            if (isUnsat(stemBFList)) {
              logger.log(Level.SEVERE, String.format("Found unsat predicates in stem\n"));
              getCounterexampleTraceInfo(stemBFList);
            }

            // create loop
            List<PathFormula> loopPathFormulaList =
                SlicingAbstractionsUtils.getFormulasForPath(
                    pathFormulaManager,
                    solver,
                    firstNodeInCycle,
                    loopPath.asStatesList(),
                    stemPathFormula.getSsa(),
                    Iterables.getLast(stemPathFormulaList).getPointerTargetSet(),
                    false);
            PathFormula loopPathFormula =
                createSinglePathFormula(loopPathFormulaList, stemPathFormula);

            // check loop prefixes for infeasibility
            ImmutableList<BooleanFormula> loopBFList =
                loopPathFormulaList.stream()
                    .map(x -> x.getFormula())
                    .collect(ImmutableList.toImmutableList());
            if (isUnsat(loopBFList)) {
              logger.log(Level.SEVERE, String.format("Found unsat predicates in loop\n"));
              getCounterexampleTraceInfo(loopBFList);
            }

            // check stem and loop prefixes for infeasibility
            ImmutableList<BooleanFormula> stemAndLoopBFList =
                ImmutableList.<BooleanFormula>builder()
                    .addAll(stemBFList)
                    .addAll(loopBFList.subList(1, loopBFList.size()))
                    .build();

            if (isUnsat(stemAndLoopBFList)) {
              logger.log(
                  Level.SEVERE,
                  String.format("Found unsat predicates in stem concatenated with loop\n"));
              getCounterexampleTraceInfo(stemAndLoopBFList);
            }

            // create lassos and check for ranking function
            StemAndLoop stemAndLoop =
                new LassoBuilder.StemAndLoop(
                    stemPathFormula,
                    loopPathFormula,
                    stemPathFormula.getSsa());

            Dnf stemDnf = lassoBuilder.toDnf(stemAndLoop.getStem());
            Dnf loopDnf =
                lassoBuilder.toDnf(stemAndLoop.getLoop(), stemDnf.getUfEliminationResult());

            LoopStructure loopStructure = cfa.getLoopStructure().get();
            ImmutableList<Loop> loops =
                loopStructure.getAllLoops()
                    .stream()
                    .filter(
                        x -> cycle.stream()
                            .map(AbstractStates::extractLocation)
                            .allMatch(y -> x.getLoopNodes().contains(y)))
                    .collect(ImmutableList.toImmutableList());
            Loop loop = loops.iterator().next();

            Set<CVariableDeclaration> varDeclarations = variables.getDeclarations(loop);
            ImmutableMap<String, CVariableDeclaration> varDeclarationsForName =
                Maps.uniqueIndex(varDeclarations, AVariableDeclaration::getQualifiedName);

            shutdownNotifier.shutdownIfNecessary();
            Collection<Lasso> lassos =
                lassoBuilder
                    .createLassos(stemAndLoop, stemDnf, loopDnf, varDeclarationsForName, false);

            for (Lasso lasso : lassos) {
              LassoAnalysisResult checkTermination =
                  lassoAnalysis.checkTermination(loop, ImmutableList.of(lasso), varDeclarations);
              if (checkTermination.hasNonTerminationArgument()) {
                // feasible loop for CEX found
                @SuppressWarnings("unused")
                NonTerminationArgument nonTerminationArgument =
                    checkTermination.getNonTerminationArgument();
                logger.logf(Level.SEVERE, "LassoRanker has non-termination argument");
              }
              if (checkTermination.hasTerminationArgument()) {
                @SuppressWarnings("unused")
                RankingRelation terminationArgument = checkTermination.getTerminationArgument();
                logger.logf(Level.SEVERE, "LassoRanker has termination argument");
              }
              if (checkTermination.isUnknown()) {
                logger.logf(Level.SEVERE, "Argument from LassoRanker is unknown");
              }
            }
          } catch (TermException | SolverException | SMTLIBException | IOException e) {
            logger.logfException(Level.SEVERE, e, e.getMessage());
            throw new CPAException(e.getMessage(), e);
          }
        }
      }

      return;
    }
  }

  private CounterexampleTraceInfo
      getCounterexampleTraceInfo(ImmutableList<BooleanFormula> pBooleanFormulas)
          throws CPAException, InterruptedException {
    CounterexampleTraceInfo cexTraceInfo =
        interpolationManager.buildCounterexampleTrace(new BlockFormulas(pBooleanFormulas));
    logger.log(
        Level.WARNING,
        String.format(
            "Distinct interpolants:\n%s",
            cexTraceInfo.getInterpolants()
                .stream()
                .distinct()
                .collect(ImmutableList.toImmutableList())));
    return cexTraceInfo;
  }

  private boolean hasFeasibleFinitePrefixFromTo(ARGState from, ARGState to)
      throws InterruptedException, CPAException, SolverException {
    ARGPath path = ARGUtils.getShortestPathTo(to);
    PathFormula pathFormula =
        SlicingAbstractionsUtils.buildPathFormula(
            from,
            to,
            path.asStatesList(),
            SSAMap.emptySSAMap().withDefault(-1),
            PointerTargetSet.emptyPointerTargetSet(),
            solver,
            pathFormulaManager,
            false);
    if (isUnsat(pathFormula.getFormula())) {
      logger.log(Level.SEVERE, String.format("Found unsat predicates in path\n"));
      return true;
    }

    return false;
  }

  private boolean isUnsat(BooleanFormula formula) throws InterruptedException, SolverException {
    return isUnsat(ImmutableList.of(formula));
  }

  private boolean isUnsat(List<BooleanFormula> formulas)
      throws InterruptedException, SolverException {
    try (ProverEnvironment proverEnvironment = solver.newProverEnvironment()) {
      for (BooleanFormula formula : formulas) {
        proverEnvironment.push(formula);
      }
      return proverEnvironment.isUnsat();
    }
  }

  private PathFormula
      createSinglePathFormula(List<PathFormula> pPathFormulas, PathFormula pStartFormula) {
    PathFormula result = pathFormulaManager.makeEmptyPathFormula(pStartFormula);
    for (Iterator<PathFormula> iterator = pPathFormulas.iterator(); iterator.hasNext();) {
      PathFormula next = iterator.next();
      BooleanFormula resultFormula =
          formulaManagerView.getBooleanFormulaManager().and(result.getFormula(), next.getFormula());
      result =
          new PathFormula(
              resultFormula,
              next.getSsa(),
              next.getPointerTargetSet(),
              result.getLength() + next.getLength());
    }

    PathFormula lastListElement = Iterables.getLast(pPathFormulas);
    if (!result.getSsa().equals(lastListElement.getSsa())) {
      throw new RuntimeException(
          String.format(
              "Inconsistent SSA-map produced:" + "\n(actual: %s)" + "\n(expected %s)",
              result.getSsa(),
              lastListElement.getSsa()));
    }

    if (!result.getPointerTargetSet().equals(lastListElement.getPointerTargetSet())) {
      throw new RuntimeException(
          String.format(
              "Inconsistent pointertarget-set produced:" + "\n(actual: %s)" + "\n(expected %s)",
              result.getPointerTargetSet(),
              lastListElement.getPointerTargetSet()));
    }

    return result;
  }

  private PathFormula createSinglePathFormula(List<PathFormula> pPathFormulas) {
    return createSinglePathFormula(pPathFormulas, pathFormulaManager.makeEmptyPathFormula());
  }

  private String printNodes(ARGPath pStemPath) {
    return printNodes(pStemPath.asStatesList());
  }

  private String printNodes(Collection<ARGState> pArgStates) {
    List<String> formattedNodes =
        pArgStates.stream()
            .map(x -> (x.getStateId() + ":" + AbstractStates.extractLocation(x)))
            .collect(Collectors.toCollection(ArrayList::new));
    return String.valueOf(formattedNodes);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

}
