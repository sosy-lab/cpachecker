// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.flowdep.FlowDependenceState;
import org.sosy_lab.cpachecker.cpa.flowdep.FlowDependenceState.FlowDependence;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.DGNode.EdgeNode;
import org.sosy_lab.cpachecker.util.dependencegraph.DGNode.UnknownPointerNode;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph.DependenceType;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph.NodeMap;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;
import org.sosy_lab.cpachecker.util.dependencegraph.FlowDepAnalysis.DependenceConsumer;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/** Factory for creating a {@link DependenceGraph} from a {@link CFA}. */
@Options(prefix = "dependencegraph")
public class DependenceGraphBuilder implements StatisticsProvider {

  private final MutableCFA cfa;
  private final Optional<VariableClassification> varClassification;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private NodeMap nodes;
  private Table<DGNode, DGNode, DependenceType> adjacencyMatrix;

  private final StatTimer dependenceGraphConstructionTimer = new StatTimer("Time for dep. graph");
  private StatInt flowDependenceNumber = new StatInt(StatKind.SUM, "Number of flow dependences");
  private StatInt controlDependenceNumber =
      new StatInt(StatKind.SUM, "Number of control dependences");
  private StatCounter isolatedNodes = new StatCounter("Number of isolated nodes");
  private final StatTimer flowDependenceTimer = new StatTimer("Time for flow deps.");
  private final StatTimer controlDependenceTimer = new StatTimer("Time for control deps.");

  @Option(
      secure = true,
      description =
          "File to export dependence graph to. If `null`, dependence"
              + " graph will not be exported as dot.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportDot = Paths.get("DependenceGraph.dot");

  @Option(
      secure = true,
      name = "controldeps.use",
      description = "Whether to consider control dependencies.")
  private boolean considerControlDeps = true;

  @Option(
      secure = true,
      name = "controldeps.considerInverseAssumption",
      description =
          "Whether to take an assumption edge 'p' as control dependence if edge 'not p' is a"
              + " control dependence. This creates a larger slice, but may reduce the size of the"
              + " state space for deterministic programs. This behavior is also closer to the"
              + " static program slicing based on control-flow graphs (CFGs), where branching is"
              + " represented by a single assumption (with true- and false-edges)")
  private boolean controlDepsTakeBothAssumptions = false;

  @Option(
      secure = true,
      name = "flowdeps.use",
      description = "Whether to consider (data-)flow dependencies.")
  private boolean considerFlowDeps = true;

  public DependenceGraphBuilder(
      final MutableCFA pCfa,
      final Optional<VariableClassification> pVarClassification,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    varClassification = pVarClassification;
  }

  public DependenceGraph build()
      throws InvalidConfigurationException, InterruptedException, CPAException {
    dependenceGraphConstructionTimer.start();
    nodes = new NodeMap();
    adjacencyMatrix = HashBasedTable.create();

    // If you add additional types of dependencies, they should probably be added to this check,
    // as well
    if (!considerFlowDeps && !considerControlDeps) {
      throw new InvalidConfigurationException(
          "At least one kind of dependency is required"
              + " to build a meaningful dependence graph");
    }

    if (considerFlowDeps) {
      flowDependenceTimer.start();
      try {
        // addFlowDependences();
        addFlowDependencesNew();
      } finally {
        flowDependenceTimer.stop();
      }
    }

    if (considerControlDeps) {
      controlDependenceTimer.start();
      try {
        addControlDependences();
      } finally {
        controlDependenceTimer.stop();
      }
    }
    addMissingNodes();

    DependenceGraph dg = new DependenceGraph(nodes, adjacencyMatrix, shutdownNotifier);
    export(dg);
    logger.log(
        Level.FINE,
        "Create dependence graph with ",
        nodes.size(),
        " nodes and ",
        adjacencyMatrix.size(),
        " edges.");
    dependenceGraphConstructionTimer.stop();
    return dg;
  }

  private void addMissingNodes() {
    EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverse(cfa.getMainFunction(), edgeCollector);
    List<CFAEdge> allEdges = edgeCollector.getVisitedEdges();

    for (CFAEdge e : allEdges) {
      if (!nodes.containsANodeForEdge(e)) {
        nodes.getNodesForEdges().put(e, Optional.empty(), new EdgeNode(e));
        isolatedNodes.inc();
      }
    }
  }

  private static Iterable<CFANode> createNodeIterable(
      CFANode pNode, boolean pForward, Predicate<CFANode> pStop, Predicate<CFANode> pFilter) {

    if (pStop.test(pNode)) {
      return Collections::emptyIterator;
    }

    Iterator<CFANode> iterator =
        (pForward ? CFAUtils.allSuccessorsOf(pNode) : CFAUtils.allPredecessorsOf(pNode)).iterator();

    return () -> Iterators.filter(iterator, pFilter);
  }

  private static Iterable<CFANode> iteratePredecessors(CFANode pNode) {

    return createNodeIterable(
        pNode,
        false,
        node -> node instanceof FunctionEntryNode,
        node -> !(node instanceof FunctionExitNode));
  }

  private static Iterable<CFANode> iterateSuccessors(CFANode pNode) {

    return createNodeIterable(
        pNode,
        true,
        node -> node instanceof FunctionExitNode,
        node -> !(node instanceof FunctionEntryNode));
  }

  private boolean ignoreFunctionEdge(CFAEdge pEdge) {
    return pEdge instanceof CFunctionCallEdge || pEdge instanceof CFunctionReturnEdge;
  }

  private static List<CFAEdge> getGlobalDeclarationEdges(CFA pCfa) {

    CFANode node = pCfa.getMainFunction();
    List<CFAEdge> declEdges = new ArrayList<>();
    Set<CFANode> visited = new HashSet<>();

    while (node.getNumLeavingEdges() == 1 && !visited.contains(node)) {

      visited.add(node);

      CFAEdge edge = node.getLeavingEdge(0);

      if (edge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
        if (declaration.isGlobal()) {
          declEdges.add(edge);
        }
      }

      node = edge.getSuccessor();
    }

    return ImmutableList.copyOf(declEdges);
  }

  private void addFlowDependencesNew() throws InterruptedException, CPAException {

    GlobalPointerState pointerState =
        GlobalPointerState.createFlowSensitive(cfa, logger, shutdownNotifier);

    boolean unknownPointer = false;

    outer:
    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {

        EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(edge);

        for (CExpression expression :
            Iterables.concat(edgeDefUseData.getPointeeDefs(), edgeDefUseData.getPointeeUses())) {

          Set<MemoryLocation> possiblePointees = pointerState.getPossiblePointees(edge, expression);

          // if there are no possible pointees, the pointer is unknown
          if (possiblePointees.isEmpty()) {
            unknownPointer = true;
            break outer;
          }

          // the current pointer analysis doesn't support structs and unions
          // potential pointers to struct instances point to the struct-type declaration
          // if such an unsupported usage is encountered, the pointer is unknown
          for (MemoryLocation possiblePointee : possiblePointees) {
            String identifier = possiblePointee.getIdentifier();
            if (identifier.contains("struct ") || identifier.contains("union ")) {
              unknownPointer = true;
              break outer;
            }
          }
        }
      }
    }

    if (unknownPointer) {

      for (CFANode node : cfa.getAllNodes()) {
        for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
          addDependence(
              getDGNodeForUnknownPointer(), getDGNode(edge, Optional.empty()), DependenceType.FLOW);
        }
      }

      return;
    }

    ForeignDefUseData foreignDefUseData = ForeignDefUseData.extract(cfa, pointerState);

    List<CFAEdge> globalEdges = getGlobalDeclarationEdges(cfa);
    Map<String, CFAEdge> declarationEdges = new HashMap<>();

    for (CFAEdge edge : globalEdges) {
      if (edge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
        if (declaration instanceof CFunctionDeclaration) {
          String name = ((CFunctionDeclaration) declaration).getQualifiedName();
          declarationEdges.put(name, edge);
        } else if (declaration instanceof CComplexTypeDeclaration) {
          CComplexType globalType = ((CComplexTypeDeclaration) declaration).getType();
          String name = globalType.getQualifiedName();
          declarationEdges.put(name, edge);
        }
      }
    }

    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {

      // TODO: use more reasonable counter
      var flowDepCount =
          new Object() {
            int value = 0;
          };

      CFAEdge funcDeclEdge = declarationEdges.get(entryNode.getFunctionName());
      for (CFAEdge callEdge : CFAUtils.enteringEdges(entryNode)) {
        addDependence(
            getDGNode(funcDeclEdge, Optional.empty()),
            getDGNode(callEdge, Optional.empty()),
            DependenceType.FLOW);
        flowDepCount.value++;
      }

      DomTree<CFANode> domTree =
          Dominance.createDomTree(
              entryNode,
              DependenceGraphBuilder::iterateSuccessors,
              DependenceGraphBuilder::iteratePredecessors);

      DependenceConsumer dependenceConsumer =
          (defEdge, useEdge, cause) -> {
            Optional<MemoryLocation> defEdgeCause = Optional.empty();
            Optional<MemoryLocation> useEdgeCause = Optional.empty();

            if (defEdge instanceof CFunctionCallEdge || defEdge instanceof CFunctionReturnEdge) {
              defEdgeCause = Optional.of(cause);
            }

            if (useEdge instanceof CFunctionCallEdge || useEdge instanceof CFunctionReturnEdge) {
              useEdgeCause = Optional.of(cause);
            }

            addDependence(
                getDGNode(defEdge, defEdgeCause),
                getDGNode(useEdge, useEdgeCause),
                DependenceType.FLOW);
            flowDepCount.value++;
          };

      boolean isMain = entryNode.equals(cfa.getMainFunction());

      new FlowDepAnalysis(
              domTree,
              Dominance.createDomFrontiers(domTree),
              entryNode,
              isMain ? ImmutableList.of() : globalEdges,
              pointerState,
              foreignDefUseData,
              declarationEdges,
              dependenceConsumer)
          .run();

      flowDependenceNumber.setNextValue(flowDepCount.value);
    }
  }

  private void addControlDependence(CFAEdge pDependingOnEdge, CFAEdge pDependentEdge) {
    addDependence(
        getDGNode(pDependingOnEdge, Optional.empty()),
        getDGNode(pDependentEdge, Optional.empty()),
        DependenceType.CONTROL);
  }

  private void addControlDependences() {

    int controlDepCount = 0;
    boolean dependOnBothAssumptions = controlDepsTakeBothAssumptions;

    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {

      DomTree<CFANode> domTree =
          Dominance.createDomTree(
              entryNode.getExitNode(),
              DependenceGraphBuilder::iteratePredecessors,
              DependenceGraphBuilder::iterateSuccessors);

      DomFrontiers<CFANode> frontiers = Dominance.createDomFrontiers(domTree);
      Set<CFAEdge> dependentEdges = new HashSet<>();

      for (CFANode dependentNode : domTree) {
        int nodeId = domTree.getId(dependentNode);
        for (CFANode branchNode : frontiers.getFrontier(dependentNode)) {
          for (CFAEdge assumeEdge : CFAUtils.leavingEdges(branchNode)) {
            int assumeSuccessorId = domTree.getId(assumeEdge.getSuccessor());
            if (dependOnBothAssumptions
                || nodeId == assumeSuccessorId
                || domTree.isAncestorOf(nodeId, assumeSuccessorId)) {
              for (CFAEdge dependentEdge : CFAUtils.allLeavingEdges(dependentNode)) {
                if (!ignoreFunctionEdge(dependentEdge) && !assumeEdge.equals(dependentEdge)) {
                  addControlDependence(assumeEdge, dependentEdge);
                  controlDepCount++;
                  dependentEdges.add(dependentEdge);
                }
              }
            }
          }
        }
      }

      Set<CFAEdge> noDomEdges = new HashSet<>();
      for (CFANode node : cfa.getFunctionNodes(entryNode.getFunction().getQualifiedName())) {
        int nodeId = domTree.getId(node);
        if (nodeId == Dominance.UNDEFINED || !domTree.hasParent(nodeId)) {
          Iterables.addAll(noDomEdges, CFAUtils.allEnteringEdges(node));
          Iterables.addAll(noDomEdges, CFAUtils.allLeavingEdges(node));
        }
      }

      Set<CFAEdge> noDomAssumes = new HashSet<>();
      for (CFAEdge edge : noDomEdges) {
        if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          noDomAssumes.add(edge);
        }
      }

      for (CFAEdge dependentEdge : noDomEdges) {
        if (!ignoreFunctionEdge(dependentEdge)) {
          for (CFAEdge assumeEdge : noDomAssumes) {
            if (!assumeEdge.equals(dependentEdge)) {
              addControlDependence(assumeEdge, dependentEdge);
              controlDepCount++;
              dependentEdges.add(dependentEdge);
            }
          }
        }
      }

      Set<CFAEdge> callEdges = new HashSet<>();
      for (CFAEdge callEdge : CFAUtils.enteringEdges(entryNode)) {
        if (callEdge instanceof CFunctionCallEdge) {
          CFAEdge summaryEdge = ((CFunctionCallEdge) callEdge).getSummaryEdge();
          callEdges.add(callEdge);
          addControlDependence(summaryEdge, callEdge);
          controlDepCount++;
        }
      }

      for (CFANode node : cfa.getFunctionNodes(entryNode.getFunction().getQualifiedName())) {
        for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
          if (!dependentEdges.contains(edge) && !ignoreFunctionEdge(edge)) {
            for (CFAEdge callEdge : callEdges) {
              addControlDependence(callEdge, edge);
              controlDepCount++;
            }
          }
        }
      }

      controlDependenceNumber.setNextValue(controlDepCount);
    }
  }

  @SuppressWarnings("unused") // old method for computing flow dependences
  private void addFlowDependences()
      throws InvalidConfigurationException, InterruptedException, CPAException {
    FlowDependences flowDependences =
        FlowDependences.create(cfa, varClassification, config, logger, shutdownNotifier);
    for (Cell<CFAEdge, Optional<MemoryLocation>, FlowDependence> c : flowDependences.cellSet()) {
      CFAEdge edgeDepending = checkNotNull(c.getRowKey());
      Optional<MemoryLocation> specificDefAtEdge = checkNotNull(c.getColumnKey());
      FlowDependence uses = checkNotNull(c.getValue());
      DGNode nodeDepending;
      if (specificDefAtEdge.isPresent()) {
        nodeDepending = getDGNode(edgeDepending, specificDefAtEdge);
      } else {
        nodeDepending = getDGNode(edgeDepending, Optional.empty());
      }
      int flowDepCount = 0;
      if (uses.isUnknownPointerDependence()) {
        DGNode dependency = getDGNodeForUnknownPointer();
        addDependence(dependency, nodeDepending, DependenceType.FLOW);
        flowDepCount++;
      } else {
        for (Entry<MemoryLocation, CFAEdge> useAndDef : uses.entries()) {
          DGNode dependency = getDGNode(useAndDef.getValue(), Optional.of(useAndDef.getKey()));
          addDependence(dependency, nodeDepending, DependenceType.FLOW);
          flowDepCount++;
        }
      }
      flowDependenceNumber.setNextValue(flowDepCount);
    }
  }

  /**
   * Returns the {@link DGNode} corresponding to the given {@link CFAEdge}. If a node for this edge
   * already exists, the existing node is returned. Otherwise, a new node is created.
   *
   * <p>Always use this method and never use {@link #createNode(CFAEdge, Optional)}!
   */
  private DGNode getDGNode(final CFAEdge pCfaEdge, final Optional<MemoryLocation> pCause) {
    if (!nodes.getNodesForEdges().contains(pCfaEdge, pCause)) {
      nodes.getNodesForEdges().put(pCfaEdge, pCause, createNode(pCfaEdge, pCause));
    }
    return nodes.getNodesForEdges().get(pCfaEdge, pCause);
  }

  private DGNode getDGNodeForUnknownPointer() {
    DGNode unk = UnknownPointerNode.getInstance();
    nodes.getSpecialNodes().add(unk);
    return unk;
  }

  /**
   * Adds the given dependence edge to the set of dependence edges and tells the nodes of the edge
   * about the new edge.
   */
  private void addDependence(DGNode pDependentOn, DGNode pDepending, DependenceType pType) {
    adjacencyMatrix.put(pDependentOn, pDepending, pType);
  }

  /**
   * Creates a new node. Never call this method directly, but use {@link #getDGNode(CFAEdge,
   * Optional)} to retrieve nodes for {@link CFAEdge CFAEdges}!
   */
  private DGNode createNode(final CFAEdge pCfaEdge, final Optional<MemoryLocation> pCause) {
    if (pCause.isPresent()) {
      return new EdgeNode(pCfaEdge, pCause.orElseThrow());
    } else {
      return new EdgeNode(pCfaEdge);
    }
  }

  private void export(DependenceGraph pDg) {
    if (exportDot != null) {
      try (Writer w = IO.openOutputFile(exportDot, Charset.defaultCharset())) {
        DGExporter.generateDOT(w, pDg);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write dependence graph to dot file");
        // continue with analysis
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public void printStatistics(
          final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
        StatInt nodeNumber = new StatInt(StatKind.SUM, "Number of DG nodes");
        nodeNumber.setNextValue(nodes.size());
        if (dependenceGraphConstructionTimer.getUpdateCount() > 0) {
          put(pOut, 3, dependenceGraphConstructionTimer);
          put(pOut, 4, flowDependenceTimer);
          put(pOut, 4, controlDependenceTimer);
          put(pOut, 4, nodeNumber);
          put(pOut, 4, flowDependenceNumber);
          put(pOut, 4, controlDependenceNumber);
          put(pOut, 4, isolatedNodes);
        }
      }

      @Override
      public String getName() {
        return ""; // empty name for nice output under CFACreator statistics
      }
    });
  }

  /**
   * Flow dependences of nodes in a {@link CFA}.
   *
   * <p>A node <code>I</code> is flow dependent on a node <code>J</code> if <code>J</code>
   * represents a variable assignment and the assignment is part of node <code>I</code>'s use-def
   * relation.
   */
  private static class FlowDependences
      extends ForwardingTable<CFAEdge, Optional<MemoryLocation>, FlowDependence> {

    @Options(prefix = "dependencegraph.flowdep")
    private static class FlowDependenceConfig {
      @Option(secure = true, description = "Run flow dependence analysis with constant propagation")
      boolean constantPropagation = false;

      @Option(
          secure = true,
          name = "constraintIsDef",
          description =
              "Whether to consider constraints on program variables (e.g., x > 10)"
                  + " as definitions.")
      // Note that, currently, flow dependences can only be 'control' OR 'flow'.
      // That means, that if one node is both flow and control dependent on another,
      // only the one added later will be added to the dependence graph. This is not a problem
      // for most use-cases, but if the type of dependences matter, this must be changed
      private boolean considerConstraintAsDef = false;

      FlowDependenceConfig(final Configuration pConfig) throws InvalidConfigurationException {
        pConfig.inject(this);
      }
    }

    // CFAEdge + defined memory location -> Edge defining the uses
    private Table<CFAEdge, Optional<MemoryLocation>, FlowDependence> dependences;

    private FlowDependences(
        final Table<CFAEdge, Optional<MemoryLocation>, FlowDependence> pDependences) {
      dependences = pDependences;
    }

    @Override
    protected Table<CFAEdge, Optional<MemoryLocation>, FlowDependence> delegate() {
      return dependences;
    }

    public static FlowDependences create(
        final MutableCFA pCfa,
        final Optional<VariableClassification> pVariableClassification,
        final Configuration pConfig,
        final LogManager pLogger,
        final ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException, CPAException, InterruptedException {
      CFA cfa = pCfa;
      if (pVariableClassification.isPresent()) {
        cfa = pCfa.makeImmutableCFA(pVariableClassification, Optional.empty());
      }

      return createDependences(cfa, pConfig, pLogger, pShutdownNotifier);
    }

    private static FlowDependences createDependences(
        final CFA pCfa,
        final Configuration pConfig,
        final LogManager pLogger,
        final ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException, CPAException, InterruptedException {
      FlowDependenceConfig options = new FlowDependenceConfig(pConfig);
      String configFile;
      if (options.constantPropagation) {
        configFile = "flowDependences-constantProp.properties";
      } else {
        configFile = "flowDependences.properties";
      }

      ConfigurationBuilder configBuilder =
          Configuration.builder().loadFromResource(FlowDependences.class, configFile);

      if (options.considerConstraintAsDef) {
        configBuilder.setOption("cpa.reachdef.constraintIsDef", "true");
      }

      Configuration config = configBuilder.build();

      ReachedSetFactory reachedFactory = new ReachedSetFactory(config, pLogger);
      ConfigurableProgramAnalysis cpa =
          new CPABuilder(config, pLogger, pShutdownNotifier, reachedFactory)
              .buildCPAs(pCfa, Specification.alwaysSatisfied(), new AggregatedReachedSets());
      Algorithm algorithm = CPAAlgorithm.create(cpa, pLogger, config, pShutdownNotifier);
      ReachedSet reached = reachedFactory.create();

      AbstractState initialState =
          cpa.getInitialState(pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      Precision initialPrecision =
          cpa.getInitialPrecision(
              pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      reached.add(initialState, initialPrecision);

      // populate reached set
      algorithm.run(reached);
      assert !reached.hasWaitingState()
          : "CPA algorithm finished, but waitlist not empty: " + reached.getWaitlist();

      Table<CFAEdge, Optional<MemoryLocation>, FlowDependence> dependencyMap =
          HashBasedTable.create();
      for (AbstractState s : reached) {
        assert s instanceof ARGState;
        ARGState wrappingState = (ARGState) s;
        FlowDependenceState flowDepState = getState(wrappingState, FlowDependenceState.class);

        for (CFAEdge g : flowDepState.getAllEdgesWithDependencies()) {
          Set<Optional<MemoryLocation>> defs = flowDepState.getNewDefinitionsByEdge(g);
          for (Optional<MemoryLocation> d : defs) {
            FlowDependence memLocUsedAndDefiner =
                flowDepState.getDependenciesOfDefinitionAtEdge(g, d);
            if (dependencyMap.contains(g, d)) {
              memLocUsedAndDefiner = dependencyMap.get(g, d).union(memLocUsedAndDefiner);
            }
            dependencyMap.put(g, d, memLocUsedAndDefiner);
          }
        }
      }

      return new FlowDependences(dependencyMap);
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractState> T getState(
        final ARGState pState, final Class<T> stateClass) {
      T s = AbstractStates.extractStateByType(pState, stateClass);
      assert s != null
          : "No "
              + ReachingDefState.class.getCanonicalName()
              + " found in composite state: "
              + pState.toString();

      return s;
    }
  }
}
