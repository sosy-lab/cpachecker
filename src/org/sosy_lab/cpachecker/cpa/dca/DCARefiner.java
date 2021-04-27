// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dca;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.transformulatransformers.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.MoreStrings;
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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManagerOptions;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionStatistics;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils.AbstractionPosition;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.GraphUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
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
import org.sosy_lab.cpachecker.util.predicates.weakening.WeakeningOptions;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.basicimpl.AbstractFormulaManager;

@Options(prefix = "cpa.dca.refiner")
public class DCARefiner implements Refiner, StatisticsProvider, AutoCloseable {

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
  private final PredicateAbstractionManager predicateAbstractionManager;
  private final PathChecker pathChecker;

  private int curRefinementIteration = 0;
  private ReachedSet reached;

  @Option(
      secure = true,
      description =
          "Skip the analysis (including the refinement) entirely, "
              + "so that the ARG is left unmodified. This is used for debugging purposes.")
  private boolean skipAnalysis = false;

  @Option(
      secure = true,
      description = "Analyize the ARG without performing a refinement for infeasible prefixes.")
  private boolean skipRefinement = false;

  @Option(
      secure = true,
      description = "If set to true, all infeasible dummy states will be kept in the ARG.")
  private boolean keepInfeasibleStates = false;

  @Option(
      secure = true,
      description =
          "The max amount of refinements for the trace abstraction algorithm. "
              + "Setting it to 0 leads to an analysis of the ARG without executing any refinements. "
              + "This is used for debugging purposes.")
  private int maxRefinementIterations = 10;

  @Option(
      secure = true,
      name = "dotExport",
      description = "Export the trace-abtraction automaton to a file in dot-format.")
  private boolean export = false;

  @Option(
      secure = true,
      name = "dotExportFile",
      description =
          "Filename that the interpolation automaton will be written to. "
              + "%s will get replaced by the automaton name.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dotExportFile = PathTemplate.ofFormatString("%s.dot");

  @SuppressWarnings({"resource", "unchecked"})
  private DCARefiner(
      final ARGCPA pArgCpa,
      final DCACPA pDcaCpa,
      final FormulaManagerView pPredFormulaManagerView,
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
    AbstractionManager abstractionManager =
        new AbstractionManager(regionManager, pConfig, pLogger, solver);
    PredicateAbstractionManagerOptions abstractionOptions =
        new PredicateAbstractionManagerOptions(pConfig);
    PredicateAbstractionsStorage abstractionStorage;
    try {
      abstractionStorage =
          new PredicateAbstractionsStorage(
              abstractionOptions.getReuseAbstractionsFrom(),
              logger,
              solver.getFormulaManager(),
              null);
    } catch (PredicateParsingFailedException e) {
      throw new InvalidConfigurationException(e.getMessage(), e);
    }
    predicateAbstractionManager =
        new PredicateAbstractionManager(
            abstractionManager,
            pathFormulaManager,
            solver,
            abstractionOptions,
            new WeakeningOptions(pConfig),
            abstractionStorage,
            pLogger,
            pNotifier,
            new PredicateAbstractionStatistics(),
            TrivialInvariantSupplier.INSTANCE);

    itpAutomatonBuilder =
        new InterpolationAutomatonBuilder(
            formulaManagerView,
            logger,
            pPredFormulaManagerView,
            predicateAbstractionManager,
            false);

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

    CFA cfa = predicateCpa.getCfa();
    FormulaManagerView predFormulaManagerView = predicateCpa.getSolver().getFormulaManager();

    Configuration config =
        Configuration.builder()
            .copyFrom(predicateCpa.getConfiguration())
            .setOption("solver.solver", SMTINTERPOL.name())
            .build();
    Configuration adjustedConfig = Solver.adjustConfigForSolver(config);

    return new DCARefiner(
        argCpa, dcaCpa, predFormulaManagerView, cfa, pLogger, pNotifier, adjustedConfig);
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

    logger.logf(Level.INFO, "Current iteration: %d", curRefinementIteration);

    // The following states were already proven to be unsat when the reached-set was built.
    // They can be safely removed from the ARG. This also includes all parents states that are both
    // a target state and do not have more than one child-element.
    ImmutableList<ARGState> infeasibleDummyStates =
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
      logger.logf(
          Level.INFO,
          "Found %d unsat predicates while building the ARG. Removing them from it.",
          infeasibleDummyStates.size());
      for (ARGState state : infeasibleDummyStates) {
        removeInfeasibleStatesFromARG(state);
      }
    } else {
      logger.logf(
          Level.INFO,
          "Not removing any infeasible predicate dummy states. There are in total %d of such states",
          infeasibleDummyStates.size());
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
                  AbstractionPosition.NONE);
          PathFormula stemPathFormula = createSinglePathFormula(stemPathFormulaList);

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

          // create loop
          List<PathFormula> loopPathFormulaList =
              SlicingAbstractionsUtils.getFormulasForPath(
                  pathFormulaManager,
                  solver,
                  firstNodeInCycle,
                  loopPath.asStatesList(),
                  stemPathFormula.getSsa(),
                  Iterables.getLast(stemPathFormulaList).getPointerTargetSet(),
                  AbstractionPosition.NONE);
          PathFormula loopPathFormula =
              createSinglePathFormula(loopPathFormulaList, stemPathFormula);

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
          ImmutableList<CFANode> cfaNodesOfCurrentCycle =
              cycle
                  .stream()
                  .map(AbstractStates::extractLocation)
                  .collect(ImmutableList.toImmutableList());
          ImmutableList<Loop> loops =
              FluentIterable.from(loopStructure.getAllLoops())
                  .filter(x -> x.getLoopNodes().containsAll(cfaNodesOfCurrentCycle))
                  .toList();
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
        "No cycles left to check, continuing with simple paths (paths with loose ends).");

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

      if (path.asStatesList().stream().anyMatch(infeasibleDummyStates::contains)) {
        verify(keepInfeasibleStates);
        logger.logf(
            Level.INFO,
            "Path contains a predicate dummy state that has already been proven "
                + "infeasible. Not performing a refinement due to the option "
                + " 'keepInfeasibleStates' being set.");
        continue;
      }

      List<PathFormula> pathFormulaList =
          SlicingAbstractionsUtils.getFormulasForPath(
              pathFormulaManager,
              solver,
              (ARGState) reached.getFirstState(),
              path.asStatesList(),
              AbstractionPosition.NONE);

      // check path for infeasibility
      ImmutableList<BooleanFormula> bfList =
          transformedImmutableListCopy(pathFormulaList, PathFormula::getFormula);

      try {
        if (!isUnsat(bfList)) {
          continue;
        }
      } catch (SolverException e) {
        throw new CPAException(e.getMessage(), e);
      }

      if (refineFinitePrefixes(path, pathFormulaList)) {
        // Immediately perform an abstraction refinement next
        return true;

      } else {
        logger.log(Level.INFO, "Feasible errorpath with target states found.");

        CounterexampleTraceInfo cexTraceInfo =
            interpolationManager.buildCounterexampleTrace(
                new BlockFormulas(bfList),
                transformedImmutableListCopy(
                    path.asStatesList(), PredicateAbstractState::getPredicateState));
        CounterexampleInfo cexInfo = pathChecker.createCounterexample(path, cexTraceInfo);

        path.getLastState().addCounterexampleInformation(cexInfo);
        return false;
      }
    }

    logger.log(Level.INFO, "Finished checking the simple paths for targetstates.");

    return false;
  }

  private void removeInfeasibleStatesFromARG(ARGState pState) {
    verify(pState.getChildren().isEmpty());
    Set<ARGState> statesToRemove = new HashSet<>();
    collectStatesToRemove(pState, statesToRemove);
    logger.logf(
        Level.INFO,
        "Removing %d state(s) that are attached to the given state",
        statesToRemove.size());
    statesToRemove.forEach(ARGState::removeFromARG);
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
    logger.logf(
        Level.INFO,
        "Mapping of interpolants to arg-states:\n%s",
        lazyPrintItpToTransitionMapping(pPath, interpolants));

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

      if (export) {
        logger.log(Level.INFO, automaton);
        if (dotExportFile != null) {
          try (Writer w =
              IO.openOutputFile(
                  dotExportFile.getPath(automaton.getName()),
                  Charset.defaultCharset())) {
            automaton.writeDotFile(w);
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
          }
        }
      }

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

  private Object
      lazyPrintItpToTransitionMapping(ARGPath pPath, List<BooleanFormula> pInterpolants) {
    return MoreStrings.lazyString(
        () -> Streams.zip(
            pPath.getStatePairs().stream(),
            pInterpolants.stream(),
            (statePair, itp) -> String.format(
                "%d:%s -> %d:%s : %s",
                statePair.getFirstNotNull().getStateId(),
                AbstractStates.extractLocation(statePair.getFirstNotNull()),
                statePair.getSecondNotNull().getStateId(),
                AbstractStates.extractLocation(statePair.getSecondNotNull()),
                itp))
            .collect(Collectors.joining("\n")));
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

  public boolean isUnsat(BooleanFormula... pFormulas) throws InterruptedException, SolverException {
    return isUnsat(ImmutableList.copyOf(pFormulas));
  }

  private boolean isUnsat(Collection<BooleanFormula> pFormulas)
      throws InterruptedException, SolverException {
    try (ProverEnvironment proverEnvironment = solver.newProverEnvironment()) {
      for (BooleanFormula formula : pFormulas) {
        proverEnvironment.push(formula);
      }
      return proverEnvironment.isUnsat();
    }
  }

  private PathFormula createSinglePathFormula(
      List<PathFormula> pPathFormulas, PathFormula pStartFormula) {
    PathFormula result = pathFormulaManager.makeEmptyPathFormula(pStartFormula);
    for (PathFormula next : pPathFormulas) {
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
        () -> FluentIterable.from(pArgStates)
            .transform(x -> (x.getStateId() + ":" + AbstractStates.extractLocation(x)))
            .toString());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  @Override
  public void close() {
    lassoAnalysis.close();
  }
}
