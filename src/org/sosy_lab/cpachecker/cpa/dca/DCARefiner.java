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
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier.TrivialInvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.termination.ClassVariables;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder.Dnf;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder.StemAndLoop;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
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
import org.sosy_lab.cpachecker.cpa.automaton.InterpolationAutomaton;
import org.sosy_lab.cpachecker.cpa.automaton.InterpolationAutomatonBuilder;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.GraphUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
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
  private final InterpolationAutomatonBuilder itpAutomatonBuilder;
  private final AbstractionManager abstractionManager;
  private final PredicateAbstractionManager predicateAbstractionManager;

  private final PathChecker pathChecker;

  private int curRefinementIteration = 0;

  @Option(
      secure = false,
      description = "Allows to abort the analysis early in order to only produce the ARG")
  private boolean skipAnalysis = false;

  @Option(
      secure = false,
      description = "Abort the refinement of finite prefixes for the purpose of better debugging")
  private boolean skipRefinement = false;

  @Option(
      secure = false,
      description = "Keep infeasible dummy states that allow for better debugging")
  private boolean keepInfeasibleStates = false;

  @Option(
      secure = false,
      description = "Set number of refinements for the trace abstraction algorithm")
  private int maxRefinementIterations = 0;

  private ReachedSet reached;

  @SuppressWarnings({"resource", "unchecked"})
  private DCARefiner(
      final ARGCPA pArgCpa,
      final DCACPA pDcaCpa,
      final FormulaManagerView pPredFormulaManagerView,
      final CFA pCfa,
      final LogManager pLogger,
      final ShutdownNotifier pNotifier,
      Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig = Solver.adjustConfigForSolver(pConfig);
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
            formulaManagerView, pConfig, pLogger, pNotifier, pCfa, AnalysisDirection.FORWARD);

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

    SymbolicRegionManager regionManager = new SymbolicRegionManager(solver);
    abstractionManager = new AbstractionManager(regionManager, pConfig, pLogger, solver);
    try {
      predicateAbstractionManager =
          new PredicateAbstractionManager(
              abstractionManager,
              pathFormulaManager,
              solver,
              pConfig,
              pLogger,
              pNotifier,
              TrivialInvariantSupplier.INSTANCE);
    } catch (PredicateParsingFailedException e) {
      throw new InvalidConfigurationException(e.getMessage(), e);
    }

    itpAutomatonBuilder =
        new InterpolationAutomatonBuilder(
            this, formulaManagerView, logger, pPredFormulaManagerView);

    AssignmentToPathAllocator pathAllocator =
        new AssignmentToPathAllocator(pConfig, shutdownNotifier, logger, cfa.getMachineModel());
    pathChecker = new PathChecker(pConfig, logger, pathFormulaManager, solver, pathAllocator);
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
    FormulaManagerView predFormulaManagerView = predicateCpa.getSolver().getFormulaManager();

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(config).setOption("solver.solver", SMTINTERPOL.name());

    return new DCARefiner(
        argCpa, dcaCpa, predFormulaManagerView, cfa, pLogger, pNotifier, configBuilder.build());
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

  public PredicateAbstractionManager getPredicateAbstractionManager() {
    return predicateAbstractionManager;
  }

  private boolean performRefinement0(final ReachedSet pReached)
      throws InterruptedException, CPAException {
    reached = pReached;

    // The following states were already proven to be unsat during the creation of the reached-set.
    // Hence, they can be safely removed from the ARG, including all parents states that are are both
    // a target state and do not have more than one child-element
    ImmutableList<ARGState> dummyStatesToRemove =
        pReached
            .asCollection()
            .stream()
            .filter(
                x ->
                    AbstractStates.extractStateByType(x, PredicateAbstractState.class)
                        instanceof PredicateAbstractState.InfeasibleDummyState)
            .map(x -> (ARGState) x)
            .collect(ImmutableList.toImmutableList());

    if (!keepInfeasibleStates) {
      for (ARGState state : dummyStatesToRemove) {
        logger.log(
            Level.INFO, "Found unsat predicates during creation of the ARG. Removing them...");
        removeInfeasibleStatesFromARG(state);
      }
    } else {
      logger.logf(
          Level.INFO,
          "Not removing any infeasible predicate dummy states. There are in total %d of such states",
          dummyStatesToRemove.size());
    }

    shutdownNotifier.shutdownIfNecessary();
    Set<StronglyConnectedComponent> SCCs =
        GraphUtils.retrieveSCCs(reached)
            .stream()
            .filter(x -> x.getNodes().size() > 1)
            .filter(StronglyConnectedComponent::hasTargetStates)
            .collect(ImmutableSet.toImmutableSet());

    logger.logf(Level.INFO, "Found %d SCC(s) with target-states", SCCs.size());

    for (StronglyConnectedComponent scc : SCCs) {

      shutdownNotifier.shutdownIfNecessary();
      List<List<ARGState>> sscCycles = GraphUtils.retrieveSimpleCycles(scc.getNodes(), reached);
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
              transformedImmutableListCopy(stemPathFormulaList, PathFormula::getFormula);
          if (isUnsat(stemBFList)) {
            logger.log(Level.SEVERE, "Found unsat predicates in stem");
            if (refineFinitePrefixes(stemPath, stemPathFormulaList)) {
              // received flag to immediately perform an abstraction refinement
              return true;
            } else {
              continue;
            }
          }

          // check loop prefixes for infeasibility
          ImmutableList<BooleanFormula> loopBFList =
              transformedImmutableListCopy(loopPathFormulaList, PathFormula::getFormula);
          if (isUnsat(loopBFList)) {
            logger.log(Level.SEVERE, "Found unsat predicates in loop");
            if (refineFinitePrefixes(loopPath, loopPathFormulaList)) {
              // received flag to immediately perform an abstraction refinement
              return true;
            } else {
              continue;
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
          ImmutableList<PathFormula> stemAndLoopPathFormulaList =
              FluentIterable.from(stemPathFormulaList)
                  .append(Iterables.skip(loopPathFormulaList, 1))
                  .toList();
          if (isUnsat(stemAndLoopBFList)) {
            logger.log(Level.SEVERE, "Found unsat predicates in stem concatenated with loop");
            if (refineFinitePrefixes(stemAndLoopPath, stemAndLoopPathFormulaList)) {
              // received flag to immediately perform an abstraction refinement
              return true;
            } else {
              continue;
            }
          }

          // create a lasso and check it using LassoRanker for a termination argument
          // (more specifically, a ranking function)
          StemAndLoop stemAndLoop =
              new LassoBuilder.StemAndLoop(
                  stemPathFormula, loopPathFormula, stemPathFormula.getSsa());

          Dnf stemDnf = lassoBuilder.toDnf(stemAndLoop.getStem());
          Dnf loopDnf = lassoBuilder.toDnf(stemAndLoop.getLoop(), stemDnf.getUfEliminationResult());

          LoopStructure loopStructure = cfa.getLoopStructure().orElseThrow();
          // TODO: Rewrite the code below so that always the correct loop is selected
          // The current logic below will most likely not work for programs with several / complex
          // loops
          ImmutableList<CFANode> collect =
              cycle
                  .stream()
                  .map(AbstractStates::extractLocation)
                  .collect(ImmutableList.toImmutableList());
          ImmutableList<Loop> loops =
              loopStructure
                  .getAllLoops()
                  .stream()
                  .filter(x -> collect.containsAll(x.getLoopNodes()))
                  .collect(ImmutableList.toImmutableList());
          Loop loop = loops.iterator().next();

          Set<CVariableDeclaration> varDeclarations = variables.getDeclarations(loop);
          ImmutableMap<String, CVariableDeclaration> varDeclarationsForName =
              Maps.uniqueIndex(varDeclarations, AVariableDeclaration::getQualifiedName);

          shutdownNotifier.shutdownIfNecessary();
          Collection<Lasso> lassos =
              lassoBuilder.createLassos(
                  stemAndLoop, stemDnf, loopDnf, varDeclarationsForName, false);

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

    // All loops containing target states have been handled now - either all of them were
    // explicitly proven as safe, or there were no cycles available in the first place
    logger.log(
        Level.INFO,
        "No cycles left to check, continuing with simple paths now (i.e., paths with loose ends).");

    ImmutableList<ARGState> targetStatesWithoutChildren =
        reached
            .asCollection()
            .stream()
            .map(x -> (ARGState) x)
            .filter(x -> x.getChildren().isEmpty())
            .filter(AbstractStates::isTargetState)
            .collect(ImmutableList.toImmutableList());
    if (targetStatesWithoutChildren.isEmpty()) {
      logger.log(Level.INFO, "Did not found any finite paths that contain target states.");
    }
    for (ARGState stateWithoutChildren : targetStatesWithoutChildren) {
      ARGPath path = ARGUtils.getShortestPathTo(stateWithoutChildren);
      logger.logf(Level.INFO, "Path to last node: %s\n", lazyPrintNodes(path));

      List<PathFormula> pathFormulaList =
          SlicingAbstractionsUtils.getFormulasForPath(
              pathFormulaManager,
              solver,
              (ARGState) reached.getFirstState(),
              path.asStatesList(),
              false);

      // check path for infeasibility
      ImmutableList<BooleanFormula> bfList =
          transformedImmutableListCopy(pathFormulaList, PathFormula::getFormula);
      try {
        if (isUnsat(bfList)) {
          // Found unsat predicates in path -> simply remove them from the ARG

          if (!keepInfeasibleStates) {
            logger.log(
                Level.INFO, "Found unsat predicates in finite path. Removing it from the ARG.");
            removeInfeasibleStatesFromARG(stateWithoutChildren);
          } else {
            logger.logf(
                Level.INFO, "Flag received to skip removing any infeasible predicate dummy state.");
          }

        } else {
          logger.log(Level.INFO, "Found feasible errorpath with target states.");

          CounterexampleTraceInfo cexTraceInfo =
              interpolationManager.buildCounterexampleTrace(new BlockFormulas(bfList));
          CounterexampleInfo cexInfo = pathChecker.createCounterexample(path, cexTraceInfo);

          path.getLastState().addCounterexampleInformation(cexInfo);
          return false;
        }
      } catch (SolverException e) {
        throw new CPAException(e.getMessage(), e);
      }
    }

    logger.log(Level.INFO, "Finished checking the simple paths.");

    return false;
  }

  private void removeInfeasibleStatesFromARG(ARGState pState) {
    verify(pState.getChildren().isEmpty());
    Set<ARGState> statesToRemove = new HashSet<>();
    collectStatesToRemove(pState, statesToRemove);
    logger.logf(
        Level.INFO,
        "Removing %d states that are attached to the given state",
        statesToRemove.size());
    statesToRemove.stream().forEach(ARGState::removeFromARG);
    reached.removeAll(statesToRemove);
  }

  private void collectStatesToRemove(ARGState pState, Collection<ARGState> pStatesToRemove) {
    Collection<ARGState> parents = pState.getParents();
    verify(!parents.isEmpty());

    pStatesToRemove.add(pState);

    parents
        .stream()
        // do not add parent-states to the removal list that have more than one child
        .filter(x -> x.getChildren().size() == 1)
        // if the state is a nontarget-state, there is no need to remove it from the reached-set
        .filter(AbstractStates::isTargetState)
        .forEach(x -> collectStatesToRemove(x, pStatesToRemove));
  }

  private boolean refineFinitePrefixes(ARGPath pPath, List<PathFormula> pPathFormulaList)
      throws CPAException, InterruptedException {
    ImmutableList<BooleanFormula> booleanFormulas =
        transformedImmutableListCopy(pPathFormulaList, PathFormula::getFormula);
    CounterexampleTraceInfo cexTraceInfo =
        interpolationManager.buildCounterexampleTrace(new BlockFormulas(booleanFormulas));

    List<BooleanFormula> interpolants = cexTraceInfo.getInterpolants();
    logger.logf(Level.WARNING, "Interpolants:\n%s", interpolants);

    // TODO: eventually change this to an assertion
    verify(
        interpolants.stream().anyMatch(formulaManagerView.getBooleanFormulaManager()::isTrue)
            || interpolants
                .stream()
                .anyMatch(formulaManagerView.getBooleanFormulaManager()::isFalse));

    try {
      InterpolationAutomaton itpAutomaton =
          itpAutomatonBuilder.buildAutomatonFromPath(pPath, interpolants, curRefinementIteration);
      itpAutomatonBuilder.addAdditionalTransitions(itpAutomaton, pPath, interpolants);
      Automaton automaton = itpAutomaton.createAutomaton();

      // TODO: dump automaton to file (optionally) instead of printing it to stdout
      logger.log(Level.INFO, automaton);

      if (skipRefinement || curRefinementIteration >= maxRefinementIterations) {
        logger.log(Level.SEVERE, "Skipping the refinement");
        return false;
      }

      dcaCPA.addAutomaton(automaton);

      curRefinementIteration++;

      reinitializeReachedSet();
    } catch (InvalidAutomatonException e) {
      throw new CPAException(e.getMessage(), e);
    }

    return true;
  }

  private void reinitializeReachedSet() throws InterruptedException {
    reached.clear();

    // For an example how to update the PredicatePrecision, see the implementation in
    // PredicateStaticRefiner.java (future work)

    FunctionEntryNode mainFunction = cfa.getMainFunction();
    StateSpacePartition defaultPartition = StateSpacePartition.getDefaultPartition();
    AbstractState initialState = argCPA.getInitialState(mainFunction, defaultPartition);
    Precision initialPrecision = argCPA.getInitialPrecision(mainFunction, defaultPartition);
    reached.add(initialState, initialPrecision);
  }

  public ImmutableList<AbstractionPredicate> makePredicates(BooleanFormula pInterpolant) {
    return ImmutableList.of(abstractionManager.makePredicate(pInterpolant));
  }

  public boolean isUnsat(BooleanFormula... formulas) throws InterruptedException, SolverException {
    return isUnsat(ImmutableList.copyOf(formulas));
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

  private PathFormula createSinglePathFormula(
      List<PathFormula> pPathFormulas, PathFormula pStartFormula) {
    PathFormula result = pathFormulaManager.makeEmptyPathFormula(pStartFormula);
    for (Iterator<PathFormula> iterator = pPathFormulas.iterator(); iterator.hasNext(); ) {
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
