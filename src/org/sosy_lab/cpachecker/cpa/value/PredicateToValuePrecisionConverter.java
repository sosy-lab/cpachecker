// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionBootstrapper.InitialPredicatesOptions;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph.BackwardsVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraphBuilder;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.BackwardsVisitOnceVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.value.reuse.precision.predicate")
public class PredicateToValuePrecisionConverter implements Statistics {

  public enum PredicateConverterStrategy {
    CONVERT_ONLY,
    CONVERT_AND_ADD_FLOW_BACKWARD,
    CONVERT_AND_ADD_FLOW_BIDIRECTED,
  }

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private static final CFANode dummyNode = CFANode.newDummyCFANode();
  private final CFAEdge dummyEdge;

  @Option(
      secure = true,
      name = "strategy",
      description = "which strategy to use to convert predicate to value precision")
  private PredicateConverterStrategy converterStrategy = PredicateConverterStrategy.CONVERT_ONLY;

  @Option(
      secure = true,
      name = "useControl",
      description = "also consider control dependencies during adaption of predicate precision")
  private boolean considerControlDependence = false;

  @Option(
      secure = true,
      name = "includeControlNonEquiv",
      description =
          "also consider other binary operators then ==, !== when considering control dependencies"
              + " while adapting predicate precision")
  private boolean nonEquivalenceInControl = false;

  @Option(
      secure = true,
      name = "relevantProperties",
      description =
          "comma-separated list of files with property specifications that should be considered "
              + "when determining the relevant edges for predicate precision adaption")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> relevantProperties = ImmutableList.of();

  @Option(
      secure = true,
      description =
          "Overall timelimit for computing initial value precision from given predicate precision"
              + "(use seconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
  private TimeSpan adaptionLimit = TimeSpan.ofNanos(0);

  private final Timer conversionTime = new Timer();
  private int numVarsAddedToPrecision = 0;

  public PredicateToValuePrecisionConverter(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    dummyEdge = new DummyCFAEdge(dummyNode, dummyNode);
    config.inject(this);
  }

  public Multimap<CFANode, MemoryLocation> convertPredPrecToVariableTrackingPrec(
      final Path pPredPrecFile) throws InvalidConfigurationException {
    ResourceLimitChecker limitChecker = null;
    ShutdownNotifier conversionShutdownNotifier;
    if (!adaptionLimit.isEmpty()) {
      ShutdownManager conversionShutdownManager =
          ShutdownManager.createWithParent(shutdownNotifier);
      conversionShutdownNotifier = conversionShutdownManager.getNotifier();
      ResourceLimit limit = WalltimeLimit.fromNowOn(adaptionLimit);
      limitChecker = new ResourceLimitChecker(conversionShutdownManager, ImmutableList.of(limit));
      limitChecker.start();
    } else {
      conversionShutdownNotifier = shutdownNotifier;
    }

    Multimap<CFANode, MemoryLocation> result = null;

    conversionTime.start();
    try (Solver solver = Solver.create(config, logger, conversionShutdownNotifier)) {
      FormulaManagerView formulaManager = solver.getFormulaManager();

      RegionManager regionManager = new SymbolicRegionManager(solver);
      AbstractionManager abstractionManager =
          new AbstractionManager(regionManager, config, logger, solver);

      PredicatePrecision predPrec =
          parsePredPrecFile(formulaManager, abstractionManager, pPredPrecFile);

      conversionShutdownNotifier.shutdownIfNecessary();

      if (!predPrec.isEmpty()) {
        logger.log(Level.INFO, "Derive value precision from given predicate precision");
        result = convertPredPrecToVariableTrackingPrec(predPrec, formulaManager, dummyNode);

        conversionShutdownNotifier.shutdownIfNecessary();

        if (converterStrategy != PredicateConverterStrategy.CONVERT_ONLY) {
          try {
            logger.log(
                Level.FINE,
                "Enhance value precision converted from predicate precision with additional"
                    + " relevant variables");
            // TODO disable option dependencegraph.controldeps.considerPointees?
            Configuration depGraphConfig =
                Configuration.builder()
                    .copyFrom(config)
                    .setOption("dependencegraph.flowdeps.use", "true")
                    .setOption(
                        "dependencegraph.controldeps.use",
                        considerControlDependence ? "true" : "false")
                    .build();
            CSystemDependenceGraph depGraph =
                new CSystemDependenceGraphBuilder(
                        cfa, depGraphConfig, logger, conversionShutdownNotifier)
                    .build();

            conversionShutdownNotifier.shutdownIfNecessary();

            Collection<CFAEdge> relevantEdges =
                determineEdgesRelevantForProperty(conversionShutdownNotifier);

            conversionShutdownNotifier.shutdownIfNecessary();

            Deque<MemoryLocation> toProcess = new ArrayDeque<>(result.values());
            Collection<MemoryLocation> inspectedVars = new HashSet<>(toProcess);
            BackwardsVisitOnceVisitor<Node> cdVisit =
                depGraph.createVisitOnceVisitor(
                    new ControlDependenceVisitor(inspectedVars, toProcess, result));
            MemoryLocation var;
            Collection<CSystemDependenceGraph.Node> relevantGraphNodes;
            boolean allUsesTracked, oneUseTracked;
            ImmutableSet<MemoryLocation> defs;
            while (!toProcess.isEmpty()) {
              conversionShutdownNotifier.shutdownIfNecessary();
              var = toProcess.pop();

              relevantGraphNodes = getRelevantGraphDefining(var, depGraph, relevantEdges);
              for (CSystemDependenceGraph.Node relVarDef : relevantGraphNodes) {
                conversionShutdownNotifier.shutdownIfNecessary();

                for (MemoryLocation varDep : depGraph.getUses(relVarDef)) {
                  registerRelevantVar(varDep, inspectedVars, toProcess, result);
                }
              }

              conversionShutdownNotifier.shutdownIfNecessary();

              if (considerControlDependence) {
                cdVisit.reset();
                depGraph.traverse(relevantGraphNodes, cdVisit);
              }

              conversionShutdownNotifier.shutdownIfNecessary();

              if (converterStrategy == PredicateConverterStrategy.CONVERT_AND_ADD_FLOW_BIDIRECTED) {
                relevantGraphNodes = getRelevantGraphUsing(var, depGraph, relevantEdges);
                for (CSystemDependenceGraph.Node relVarUse : relevantGraphNodes) {
                  defs = depGraph.getDefs(relVarUse);
                  if (!defs.isEmpty()) {
                    allUsesTracked = true;
                    oneUseTracked = false;

                    for (MemoryLocation varDep : depGraph.getUses(relVarUse)) {
                      if (inspectedVars.contains(varDep)) {
                        oneUseTracked = true;
                      } else if (!defs.contains(varDep)) {
                        allUsesTracked = false;
                        break;
                      }
                    }

                    conversionShutdownNotifier.shutdownIfNecessary();

                    if (oneUseTracked && allUsesTracked) {
                      for (MemoryLocation varDef : defs) {
                        registerRelevantVar(varDef, inspectedVars, toProcess, result);
                      }
                    }
                  }
                }
              }
            }
          } catch (CPAException e) {
            logger.logException(Level.WARNING, e, "Failed to add additional relevant variables");
          }
        }
      } else {
        logger.log(
            Level.WARNING,
            "Provided predicate precision is empty and does not contain predicates.");
      }
    } catch (InterruptedException e) {
      logger.logException(Level.INFO, e, "Precision adaption was interrupted.");
    } finally {
      conversionTime.stopIfRunning();
    }

    if (limitChecker != null) {
      limitChecker.cancel();
    }

    if (result == null) {
      return ImmutableListMultimap.of();
    }
    numVarsAddedToPrecision += result.size();
    return ImmutableListMultimap.copyOf(result);
  }

  private PredicatePrecision parsePredPrecFile(
      final FormulaManagerView pFMgr,
      final AbstractionManager abstractionManager,
      final Path pPredPrecFile) {

    // create managers for the predicate map parser for parsing the predicates from the given
    // predicate precision file

    PredicateMapParser mapParser =
        new PredicateMapParser(
            cfa, logger, pFMgr, abstractionManager, new InitialPredicatesOptions());

    try {
      return mapParser.parsePredicates(pPredPrecFile);
    } catch (IOException | PredicateParsingFailedException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not read precision from file named " + pPredPrecFile);
      return PredicatePrecision.empty();
    }
  }

  private Multimap<CFANode, MemoryLocation> convertPredPrecToVariableTrackingPrec(
      final PredicatePrecision pPredPrec,
      final FormulaManagerView pFMgr,
      final CFANode pDummyNode) {
    Collection<AbstractionPredicate> predicates = new HashSet<>();

    predicates.addAll(pPredPrec.getLocalPredicates().values());
    predicates.addAll(pPredPrec.getGlobalPredicates());
    predicates.addAll(pPredPrec.getFunctionPredicates().values());

    SetMultimap<CFANode, MemoryLocation> trackedVariables = HashMultimap.create();

    // Get the variables from the predicate precision
    for (AbstractionPredicate pred : predicates) {
      for (String var : pFMgr.extractVariables(pred.getSymbolicAtom()).keySet()) {
        trackedVariables.put(pDummyNode, MemoryLocation.parseExtendedQualifiedName(var));
      }
    }

    return trackedVariables;
  }

  private Collection<CFAEdge> determineEdgesRelevantForProperty(
      final ShutdownNotifier pConversionShutdownNotifier) {
    if (!relevantProperties.isEmpty()) {
      try {
        Configuration reachPropConfig =
            Configuration.builder()
                .setOption("cpa", "cpa.arg.ARGCPA")
                .setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA")
                .setOption(
                    "CompositeCPA.cpas", "cpa.location.LocationCPA,cpa.callstack.CallstackCPA")
                .setOption("cpa.composite.aggregateBasicBlocks", "false")
                .setOption("cpa.callstack.skipRecursion", "true")
                .setOption("cpa.automaton.breakOnTargetState", "-1")
                .setOption("output.disable", "true")
                .build();

        ReachedSetFactory rsFactory = new ReachedSetFactory(reachPropConfig, logger);
        ConfigurableProgramAnalysis cpa =
            new CPABuilder(reachPropConfig, logger, pConversionShutdownNotifier, rsFactory)
                .buildCPAs(
                    cfa,
                    Specification.fromFiles(
                        relevantProperties,
                        cfa,
                        reachPropConfig,
                        logger,
                        pConversionShutdownNotifier),
                    AggregatedReachedSets.empty());
        ReachedSet reached =
            rsFactory.createAndInitialize(
                cpa, cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

        CPAAlgorithm.create(cpa, logger, reachPropConfig, pConversionShutdownNotifier).run(reached);
        Preconditions.checkState(!reached.hasWaitingState());

        Deque<ARGState> toExplore =
            new ArrayDeque<>(
                FluentIterable.from(reached.asCollection())
                    .filter(state -> ((ARGState) state).isTarget())
                    .transform(state -> (ARGState) state)
                    .toList());
        Set<ARGState> visited = new HashSet<>(toExplore);
        ARGState exploring;
        CFAEdge edge;
        ImmutableSet.Builder<CFAEdge> relevantEdges = ImmutableSet.builder();

        while (!toExplore.isEmpty()) {
          exploring = toExplore.pop();
          for (ARGState covered : exploring.getCoveredByThis()) {
            if (visited.add(covered)) {
              toExplore.add(covered);
            }
          }
          for (ARGState parent : exploring.getParents()) {
            edge = parent.getEdgeToChild(exploring);
            Preconditions.checkNotNull(edge);
            relevantEdges.add(edge);

            if (visited.add(parent)) {
              toExplore.add(parent);
            }
          }
        }

        return relevantEdges.build();

      } catch (InvalidConfigurationException | CPAException | InterruptedException e) {
        logger.logException(Level.SEVERE, e, "Failed to determine relevant edges");
      }
    }
    return FluentIterable.from(cfa.getAllNodes())
        .transformAndConcat(node -> CFAUtils.leavingEdges(node))
        .toSet();
  }

  private Collection<Node> getRelevantGraphUsing(
      final MemoryLocation pVar,
      final CSystemDependenceGraph pDepGraph,
      final Collection<CFAEdge> pRelevantEdges) {
    return getRelevantGraphNodes(pVar, pDepGraph, pRelevantEdges, false);
  }

  private Collection<Node> getRelevantGraphDefining(
      final MemoryLocation pVar,
      final CSystemDependenceGraph pDepGraph,
      final Collection<CFAEdge> pRelevantEdges) {
    return getRelevantGraphNodes(pVar, pDepGraph, pRelevantEdges, true);
  }

  private Collection<Node> getRelevantGraphNodes(
      final MemoryLocation pVar,
      final CSystemDependenceGraph pDepGraph,
      final Collection<CFAEdge> pRelevantEdges,
      boolean extractDefinitions) {
    return FluentIterable.from(pDepGraph.getNodes())
        .filter(
            node ->
                (extractDefinitions
                        ? pDepGraph.getDefs(node).contains(pVar)
                        : pDepGraph.getUses(node).contains(pVar))
                    && (relevantProperties.isEmpty()
                        || pRelevantEdges.contains(node.getStatement().orElse(dummyEdge))))
        .toList();
  }

  public boolean collectedStats() {
    return conversionTime.getNumberOfIntervals() > 0;
  }

  @Override
  public void printStatistics(
      final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
    put(pOut, 0, "Time for adapting predicate precision", conversionTime);
    put(pOut, 0, "Number of tracked variables added", numVarsAddedToPrecision);
  }

  @Override
  public String getName() {
    return "Predicate Precision Adapter";
  }

  private static void registerRelevantVar(
      MemoryLocation pVar,
      Collection<MemoryLocation> pInspectedVars,
      Deque<MemoryLocation> pToProcess,
      final Multimap<CFANode, MemoryLocation> pResult) {
    if (pInspectedVars.add(pVar)) {
      pToProcess.push(pVar);
      pResult.put(dummyNode, pVar);
    }
  }

  private class ControlDependenceVisitor implements BackwardsVisitor {

    private final AssumptionVisitor assumeVisitor;

    public ControlDependenceVisitor(
        final Collection<MemoryLocation> pInspectedVars,
        final Deque<MemoryLocation> pToProcess,
        final Multimap<CFANode, MemoryLocation> pResult) {
      assumeVisitor = new AssumptionVisitor(pInspectedVars, pToProcess, pResult);
    }

    @Override
    public VisitResult visitNode(final Node pNode) {
      return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitEdge(
        final EdgeType pType, final Node pPredecessor, final Node pSuccessor) {
      if (pType == EdgeType.CONTROL_DEPENDENCY) {
        CFAEdge edge = pSuccessor.getStatement().orElse(null);
        if (edge instanceof CAssumeEdge) {
          ((CAssumeEdge) edge).getExpression().accept(assumeVisitor);
        }
        return VisitResult.CONTINUE;
      }
      return VisitResult.SKIP;
    }
  }

  private class AssumptionVisitor extends DefaultCExpressionVisitor<Void, NoException> {

    private final Collection<MemoryLocation> inspectedVars;
    private final Deque<MemoryLocation> toProcess;
    private final Multimap<CFANode, MemoryLocation> result;

    public AssumptionVisitor(
        final Collection<MemoryLocation> pInspectedVars,
        final Deque<MemoryLocation> pToProcess,
        final Multimap<CFANode, MemoryLocation> pResult) {
      inspectedVars = pInspectedVars;
      toProcess = pToProcess;
      result = pResult;
    }

    @Override
    protected Void visitDefault(final CExpression pExp) {
      return null;
    }

    @Override
    public Void visit(CBinaryExpression e) {
      if (e.getOperator().equals(BinaryOperator.EQUALS)
          || e.getOperator().equals(BinaryOperator.NOT_EQUALS)
          || (nonEquivalenceInControl
              && (e.getOperator().equals(BinaryOperator.GREATER_EQUAL)
                  || e.getOperator().equals(BinaryOperator.GREATER_THAN)
                  || e.getOperator().equals(BinaryOperator.LESS_EQUAL)
                  || e.getOperator().equals(BinaryOperator.LESS_THAN)))) {

        if (e.getOperand1() instanceof CLiteralExpression) {
          insertVariable(e.getOperand2());
        } else if (e.getOperand2() instanceof CLiteralExpression) {
          insertVariable(e.getOperand1());
        } else {
          Optional<MemoryLocation> var1 = getVariable(e.getOperand1());
          Optional<MemoryLocation> var2 = getVariable(e.getOperand2());

          if (var1.isPresent() && var2.isPresent()) {
            if (inspectedVars.contains(var1.orElseThrow())) {
              registerRelevantVar(var2.orElseThrow(), inspectedVars, toProcess, result);
            } else if (inspectedVars.contains(var2.orElseThrow())) {
              registerRelevantVar(var1.orElseThrow(), inspectedVars, toProcess, result);
            }
          }
        }
      }
      return null;
    }

    private void insertVariable(final CExpression exp) {
      Optional<MemoryLocation> var = getVariable(exp);
      if (var.isPresent()) {
        registerRelevantVar(var.orElseThrow(), inspectedVars, toProcess, result);
      }
    }

    private Optional<MemoryLocation> getVariable(final CExpression exp) {
      if (exp instanceof CIdExpression) {
        return Optional.of(
            MemoryLocation.parseExtendedQualifiedName(
                ((CIdExpression) exp).getDeclaration().getQualifiedName()));
      }
      return Optional.empty();
    }
  }
}
