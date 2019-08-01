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
import static com.google.common.collect.FluentIterable.from;

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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.sosy_lab.common.MoreStrings;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.termination.ClassVariables;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder.Dnf;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder.StemAndLoop;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.StronglyConnectedComponent;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBuilder;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
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

  private final ARGCPA argCPA;
  private final DCACPA dcaCPA;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final DCAStatistics statistics;

  private final PathFormulaManager pathFormulaManager;
  private final ClassVariables variables;
  private final LassoAnalysis lassoAnalysis;
  private final LassoBuilder lassoBuilder;
  private final Solver solver;
  private final FormulaManagerView formulaManagerView;
  private final InterpolationManager interpolationManager;
  private final AutomatonBuilder automatonBuilder;

  private int curRefinementIteration = 0;

  @Option(
    secure = false,
    description = "Allows to abort the analysis early in order to only produce the ARG")
  private boolean skipAnalysis = false;

  @Option(
    secure = false,
    description = "Abort the refinement of finite prefixes for the purpose of better debugging")
  private boolean skipFiniteRefinement = false;

  @Option(
    secure = false,
    description = "Set number of refinements for the trace abstraction algorithm")
  private int maxRefinementIterations = 0;

  private ReachedSet reached;

  @SuppressWarnings({"resource", "unchecked"})
  private DCARefiner(
      final ARGCPA pArgCpa,
      final DCACPA pDcaCpa,
      final CFA pCfa,
      final LogManager pLogger,
      final ShutdownNotifier pNotifier,
      final Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    argCPA = checkNotNull(pArgCpa);
    dcaCPA = checkNotNull(pDcaCpa);
    cfa = checkNotNull(pCfa);
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pNotifier);

    statistics = new DCAStatistics(pDcaCpa);

    variables = ClassVariables.collectDeclarations(cfa);

    SolverContext solverContext =
        SolverContextFactory.createSolverContext(pConfig, pLogger, pNotifier, SMTINTERPOL);
    AbstractFormulaManager<Term, ?, ?, ?> formulaManager =
        (AbstractFormulaManager<Term, ?, ?, ?>) solverContext.getFormulaManager();
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

    automatonBuilder = new AutomatonBuilder(formulaManagerView, cfa, pConfig, logger);
  }

  @SuppressWarnings("resource")
  public static Refiner create(
      final ConfigurableProgramAnalysis pCpa,
      final LogManager pLogger,
      final ShutdownNotifier pNotifier)
      throws InvalidConfigurationException {

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, DCARefiner.class);
    DCACPA dcaCpa = CPAs.retrieveCPAOrFail(pCpa, DCACPA.class, DCARefiner.class);
    PredicateCPA predicateCpa = CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, DCARefiner.class);

    Configuration config = predicateCpa.getConfiguration();
    CFA cfa = predicateCpa.getCfa();

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(config).setOption("solver.solver", SMTINTERPOL.name());

    return new DCARefiner(
        argCpa,
        dcaCpa,
        cfa,
        pLogger,
        pNotifier,
        configBuilder.build());
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached)
      throws CPAException, InterruptedException {
    if (skipAnalysis) {
      // Option is used to output the computed ARG without any further refinements
      // (i.e., for debugging)
      logger.log(Level.INFO, "Received flag to skip the refinement. Aborting analysis...");
      return false;
    }

    logger.log(Level.INFO, "Starting to analyse the reached set...");
    statistics.refinementTotalTimer.start();
    try {
      return performRefinement0(pReached);
    } finally {
      statistics.refinementTotalTimer.stop();
      logger.log(Level.INFO, "Finished analysing the reached set.");
    }
  }

  private boolean performRefinement0(final ReachedSet pReached)
      throws InterruptedException, CPAException {
    reached = pReached;

    shutdownNotifier.shutdownIfNecessary();
    Set<StronglyConnectedComponent> SCCs =
        ARGUtils.retrieveSCCs(reached)
            .stream()
            .filter(x -> x.getNodes().size() > 1)
            .filter(StronglyConnectedComponent::hasTargetStates)
            .collect(ImmutableSet.toImmutableSet());

    logger.logf(Level.INFO, "Found %d SCC(s) with target-states", SCCs.size());

    for (StronglyConnectedComponent scc : SCCs) {

      shutdownNotifier.shutdownIfNecessary();
      List<List<ARGState>> sscCycles = ARGUtils.retrieveSimpleCycles(scc.getNodes(), reached);
      logger.logf(Level.INFO, "Found %d cycle(s) in current SCC", sscCycles.size());

      for (List<ARGState> cycle : sscCycles) {
        logger.logf(Level.INFO, "Analyzing cycle: %s\n", lazyPrintNodes(cycle));
        assert cycle.stream().anyMatch(ARGState::isTarget) : "Cycle does not contain a target";

        shutdownNotifier.shutdownIfNecessary();
        ARGState firstNodeInCycle = cycle.iterator().next();
        ARGPath stemPath = ARGUtils.getShortestPathTo(firstNodeInCycle);
        ARGPath loopPath = new ARGPath(cycle);
        assert loopPath.asStatesList().equals(cycle)
            : String.format(
                "Nodes in cycle are not consistent with nodes in the ARGPath"
                    + "%n(Nodes in cycle: %s)"
                    + "%n(Nodes in loop: %s)",
                lazyPrintNodes(cycle), lazyPrintNodes(loopPath));
        logger.logf(Level.INFO, "Path to cycle: %s\n", lazyPrintNodes(stemPath));

        try {
          // create stem
          List<PathFormula> stemPathFormulaList =
              SlicingAbstractionsUtils.getFormulasForPath(
                  pathFormulaManager,
                  solver,
                  (ARGState) reached.getFirstState(),
                  stemPath.asStatesList(),
                  false);
          PathFormula stemPathFormula = createSinglePathFormula(stemPathFormulaList);

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

          // check stem prefixes for infeasibility
          ImmutableList<BooleanFormula> stemBFList =
              stemPathFormulaList.stream()
                  .map(x -> x.getFormula())
                  .collect(ImmutableList.toImmutableList());
          if (isUnsat(stemBFList)) {
            logger.log(Level.SEVERE, "Found unsat predicates in stem");
            boolean refine = refineFinitePrefixes(stemPath, stemBFList);
            if (refine) {
              return true;
            }
          }

          // check loop prefixes for infeasibility
          ImmutableList<BooleanFormula> loopBFList =
              loopPathFormulaList.stream()
                  .map(x -> x.getFormula())
                  .collect(ImmutableList.toImmutableList());
          if (isUnsat(loopBFList)) {
            logger.log(Level.SEVERE, "Found unsat predicates in loop");
            boolean refine = refineFinitePrefixes(loopPath, loopBFList);
            if (refine) {
              return true;
            }
          }

          // check stem and loop prefixes for infeasibility
          ImmutableList<ARGState> stemAndLoopStates =
              ImmutableList.<ARGState>builder()
                  .addAll(stemPath.asStatesList())
                  .addAll(loopPath.asStatesList().subList(1, loopPath.asStatesList().size()))
                  .build();
          ARGPath stemAndLoopPath = new ARGPath(stemAndLoopStates);
          ImmutableList<BooleanFormula> stemAndLoopBFList =
              ImmutableList.<BooleanFormula>builder()
                  .addAll(stemBFList)
                  .addAll(loopBFList.subList(1, loopBFList.size()))
                  .build();
          if (isUnsat(stemAndLoopBFList)) {
            logger.log(Level.SEVERE, "Found unsat predicates in stem concatenated with loop");
            boolean refine = refineFinitePrefixes(stemAndLoopPath, stemAndLoopBFList);
            if (refine) {
              return true;
            }
          }

          // create lassos and check for ranking function
          StemAndLoop stemAndLoop =
              new LassoBuilder.StemAndLoop(
                  stemPathFormula,
                  loopPathFormula,
                  stemPathFormula.getSsa());

          Dnf stemDnf = lassoBuilder.toDnf(stemAndLoop.getStem());
          Dnf loopDnf = lassoBuilder.toDnf(stemAndLoop.getLoop(), stemDnf.getUfEliminationResult());

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

          LassoAnalysisResult checkTermination =
              lassoAnalysis.checkTermination(loop, lassos, varDeclarations);
          if (checkTermination.hasNonTerminationArgument()) {
            // feasible loop for CEX found
            NonTerminationArgument nonTerminationArgument =
                checkTermination.getNonTerminationArgument();
            logger.logf(
                Level.SEVERE,
                "LassoRanker has non-termination argument: %s",
                nonTerminationArgument);
          }
          if (checkTermination.hasTerminationArgument()) {
            RankingRelation terminationArgument = checkTermination.getTerminationArgument();
            logger.logf(
                Level.SEVERE,
                "LassoRanker has termination argument %s",
                terminationArgument.getSupportingInvariants().isEmpty()
                    ? ""
                    : "\nInvariants: " + terminationArgument.getSupportingInvariants());
          }
          if (checkTermination.isUnknown()) {
            logger.logf(Level.SEVERE, "Argument from LassoRanker is unknown");
          }
        } catch (TermException | SolverException | SMTLIBException | IOException e) {
          throw new CPAException(e.getMessage(), e);
        }
      }
    }

    return false;
  }

  private boolean refineFinitePrefixes(ARGPath pPath, List<BooleanFormula> pBooleanFormulas)
      throws CPAException, InterruptedException {
    CounterexampleTraceInfo cexTraceInfo =
        interpolationManager.buildCounterexampleTrace(new BlockFormulas(pBooleanFormulas));

    List<BooleanFormula> interpolants = cexTraceInfo.getInterpolants();
    ImmutableList<BooleanFormula> distinctInterpolants =
        interpolants.stream()
            .filter(x -> !formulaManagerView.getBooleanFormulaManager().isTrue(x))
            .filter(x -> !formulaManagerView.getBooleanFormulaManager().isFalse(x))
            .distinct()
            .map(formulaManagerView::uninstantiate)
            .collect(ImmutableList.toImmutableList());
    Optional<BooleanFormula> interpolantOpt =
        Optional.ofNullable(distinctInterpolants.iterator().next());
    if (!interpolantOpt.isPresent()) {
      logger.logf(
          Level.WARNING,
          "Could not find any interpolants to do a finite-prefix refinement. "
              + "Skipping the process.\nInterpolants: %s",
          interpolants);
      return false;
    }
    if (distinctInterpolants.size() > 1) {
      logger.logf(
          Level.SEVERE,
          "Found more than one interpolant to do a finite-prefix refinement. "
              + "\nInterpolants: %s",
          interpolants);
      return false;
    }

    logger.logf(Level.WARNING, "Interpolants:\n%s", interpolants);

    try {
      Automaton itpAutomaton =
          automatonBuilder
              .buildInterpolantAutomaton(
                  pPath,
                  interpolants,
                  interpolantOpt,
                  curRefinementIteration);
      logger.log(Level.INFO, itpAutomaton);

      if (skipFiniteRefinement || curRefinementIteration >= maxRefinementIterations) {
        return false;
      }

      dcaCPA.addAutomaton(itpAutomaton);

      curRefinementIteration++;

      reinitializeReachedSet();
      logger.logf(
          Level.SEVERE,
          "Refining the arg with automaton using interpolant: %s",
          interpolantOpt.get());
    } catch (InvalidAutomatonException e) {
      throw new CPAException(e.getMessage(), e);
    }

    return true;
  }

  private void reinitializeReachedSet() throws InterruptedException {
    reached.clear();

    FunctionEntryNode mainFunction = cfa.getMainFunction();
    StateSpacePartition defaultPartition = StateSpacePartition.getDefaultPartition();
    AbstractState initialState = argCPA.getInitialState(mainFunction, defaultPartition);
    Precision initialPrecision = argCPA.getInitialPrecision(mainFunction, defaultPartition);
    reached.add(initialState, initialPrecision);
  }

  private boolean isUnsat(Collection<BooleanFormula> formulas)
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
    assert result.getSsa().equals(lastListElement.getSsa())
        : String.format(
            "Inconsistent SSA-map produced:" + "%n(actual: %s)" + "%n(expected %s)",
            result.getSsa(), lastListElement.getSsa());
    assert result.getPointerTargetSet().equals(lastListElement.getPointerTargetSet())
        : String.format(
            "Inconsistent pointertarget-set produced:" + "%n(actual: %s)" + "%n(expected %s)",
            result.getPointerTargetSet(), lastListElement.getPointerTargetSet());

    return result;
  }

  private PathFormula createSinglePathFormula(List<PathFormula> pPathFormulas) {
    return createSinglePathFormula(pPathFormulas, pathFormulaManager.makeEmptyPathFormula());
  }

  private Object lazyPrintNodes(ARGPath pStemPath) {
    return lazyPrintNodes(pStemPath.asStatesList());
  }

  private Object lazyPrintNodes(Collection<ARGState> pArgStates) {
    return MoreStrings.lazyString(
        () ->
            from(pArgStates)
                .transform(x -> (x.getStateId() + ":" + AbstractStates.extractLocation(x)))
                .toString());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

}
