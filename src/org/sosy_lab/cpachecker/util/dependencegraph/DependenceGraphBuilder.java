// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import java.util.HashMap;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
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
import org.sosy_lab.cpachecker.util.dependencegraph.ControlDependenceBuilder.ControlDependency;
import org.sosy_lab.cpachecker.util.dependencegraph.DGNode.EdgeNode;
import org.sosy_lab.cpachecker.util.dependencegraph.DGNode.UnknownPointerNode;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph.DependenceType;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph.NodeMap;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;
import org.sosy_lab.cpachecker.util.dependencegraph.FlowDepAnalysis.DependenceConsumer;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.NodeType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

/** Factory for creating a {@link DependenceGraph} from a {@link CFA}. */
@Options(prefix = "dependencegraph")
public class DependenceGraphBuilder implements StatisticsProvider {

  private final CFA cfa;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final EdgeDefUseData.Extractor defUseExtractor;
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
  private boolean controlDepsTakeBothAssumptions = true;

  @Option(
      secure = true,
      name = "flowdeps.use",
      description = "Whether to consider (data-)flow dependencies.")
  private boolean considerFlowDeps = true;

  @Option(
      secure = true,
      name = "considerPointees",
      description =
          "Whether to consider pointees. Only if this option is set to true, a pointer analysis is"
              + " run during dependence graph construction. If this option is set to false,"
              + " pointers are ignored and the resulting dependence graph misses all dependencies"
              + " where pointers are involved in.")
  private boolean considerPointees = true;

  private final SystemDependenceGraph.Builder<CFAEdge, MemoryLocation> builder;

  public DependenceGraphBuilder(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    defUseExtractor = EdgeDefUseData.createExtractor(considerPointees);

    // If you add additional types of dependencies, they should probably be added to this check,
    // as well
    if (!considerFlowDeps && !considerControlDeps) {
      throw new InvalidConfigurationException(
          "At least one kind of dependency is required"
              + " to build a meaningful dependence graph");
    }

    builder = SystemDependenceGraph.builder();
  }

  public SystemDependenceGraph<CFAEdge, MemoryLocation> build()
      throws InterruptedException, CPAException {

    dependenceGraphConstructionTimer.start();

    if (considerFlowDeps) {
      flowDependenceTimer.start();
      try {
        // addFlowDependences();
        addFlowDependecies();
      } finally {
        flowDependenceTimer.stop();
      }
    }

    if (considerControlDeps) {
      controlDependenceTimer.start();
      try {
        addControlDependencies();
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
    return builder.build();
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

  private CFunctionCallEdge getCallEdge(CFunctionSummaryEdge pSummaryEdge) {

    for (CFAEdge edge : CFAUtils.leavingEdges(pSummaryEdge.getPredecessor())) {
      if (edge instanceof CFunctionCallEdge) {
        return (CFunctionCallEdge) edge;
      }
    }

    throw new AssertionError("No CFunctionCallEdge for CFunctionSummaryEdge");
  }

  private CFunctionCall getFunctionCallWithoutParameters(CFunctionSummaryEdge pSummaryEdge) {

    CFunctionCall functionCall = pSummaryEdge.getExpression();

    if (functionCall instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement statement = (CFunctionCallAssignmentStatement) functionCall;

      CLeftHandSide lhs = statement.getLeftHandSide();
      CFunctionCallExpression rhs = statement.getRightHandSide();

      CFunctionCallExpression functionCallExpression =
          new CFunctionCallExpression(
              rhs.getFileLocation(),
              rhs.getExpressionType(),
              rhs.getFunctionNameExpression(),
              ImmutableList.of(),
              rhs.getDeclaration());

      return new CFunctionCallAssignmentStatement(
          statement.getFileLocation(), lhs, functionCallExpression);

    } else if (functionCall instanceof CFunctionCallStatement) {

      CFunctionCallStatement statement = (CFunctionCallStatement) functionCall;
      CFunctionCallExpression expression = statement.getFunctionCallExpression();

      CFunctionCallExpression functionCallExpression =
          new CFunctionCallExpression(
              expression.getFileLocation(),
              expression.getExpressionType(),
              expression.getFunctionNameExpression(),
              ImmutableList.of(),
              expression.getDeclaration());

      return new CFunctionCallStatement(statement.getFileLocation(), functionCallExpression);
    } else {
      throw new AssertionError("Unsupported function call: " + functionCall);
    }
  }

  private Optional<MemoryLocation> getReturnVariable(CFunctionSummaryEdge pSummaryEdge) {

    CFunctionCallEdge callEdge = getCallEdge(pSummaryEdge);
    Optional<CVariableDeclaration> returnVariable =
        callEdge.getSummaryEdge().getFunctionEntry().getReturnVariable().toJavaUtil();

    if (returnVariable.isPresent()) {
      String variableName = returnVariable.orElseThrow().getQualifiedName();
      return Optional.of(MemoryLocation.valueOf(variableName));
    } else {
      return Optional.empty();
    }
  }

  private void addFlowDependence(
      CFAEdge pDefEdge,
      Optional<MemoryLocation> pDefEdgeCause,
      CFAEdge pUseEdge,
      Optional<MemoryLocation> pUseEdgeCause) {
    addDependence(
        getDGNode(pDefEdge, pDefEdgeCause),
        getDGNode(pUseEdge, pUseEdgeCause),
        DependenceType.FLOW);
  }

  private void addFlowDependecies() throws InterruptedException, CPAException {

    GlobalPointerState pointerState;
    if (considerPointees) {
      pointerState = GlobalPointerState.createFlowSensitive(cfa, logger, shutdownNotifier);
    } else {
      pointerState = GlobalPointerState.EMPTY;
    }

    boolean unknownPointer = false;

    outer:
    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {

        EdgeDefUseData edgeDefUseData = defUseExtractor.extract(edge);

        for (CExpression expression :
            Iterables.concat(edgeDefUseData.getPointeeDefs(), edgeDefUseData.getPointeeUses())) {

          ImmutableSet<MemoryLocation> possiblePointees =
              pointerState.getPossiblePointees(edge, expression);

          // if there are no possible pointees, the pointer is unknown
          if (possiblePointees.isEmpty()) {
            unknownPointer = true;
            break outer;
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

    ForeignDefUseData foreignDefUseData =
        ForeignDefUseData.extract(cfa, defUseExtractor, pointerState);

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

      StatCounter flowDepCounter = new StatCounter("Flow Dependency Counter");

      CFAEdge funcDeclEdge = declarationEdges.get(entryNode.getFunctionName());
      for (CFAEdge callEdge : CFAUtils.enteringEdges(entryNode)) {
        addFlowDependence(funcDeclEdge, Optional.empty(), callEdge, Optional.empty());
        flowDepCounter.inc();
      }

      DomTree<CFANode> domTree = DominanceUtils.createFunctionDomTree(entryNode);

      DependenceConsumer dependenceConsumer =
          (defEdge, useEdge, cause) -> {
            if (defEdge instanceof CFunctionSummaryEdge && useEdge instanceof CFunctionCallEdge) {

              builder
                  .node(NodeType.ACTUAL_IN, defEdge, Optional.of(cause))
                  .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                  .on(NodeType.FORMAL_IN, useEdge, Optional.of(cause));
              flowDepCounter.inc();

            } else if (defEdge instanceof CFunctionReturnEdge
                && useEdge instanceof CFunctionSummaryEdge) {

              builder
                  .node(NodeType.FORMAL_OUT, defEdge, Optional.of(cause))
                  .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                  .on(NodeType.ACTUAL_OUT, useEdge, Optional.of(cause));
              flowDepCounter.inc();

            } else {

              NodeType defNodeType;
              Optional<MemoryLocation> defNodeVariable = Optional.empty();

              if (defEdge instanceof CFunctionCallEdge) {
                defNodeType = NodeType.FORMAL_IN;
                defNodeVariable = Optional.of(cause);
              } else if (defEdge instanceof CFunctionReturnEdge) {
                defNodeType = NodeType.FORMAL_OUT;
                defNodeVariable = Optional.of(cause);
              } else if (defEdge instanceof CFunctionSummaryEdge) {

                defNodeType = NodeType.ACTUAL_OUT;

                CFunctionSummaryEdge summaryEdge = (CFunctionSummaryEdge) defEdge;
                CFunctionCall functionCall = getFunctionCallWithoutParameters(summaryEdge);
                EdgeDefUseData defUseData = defUseExtractor.extract(functionCall);

                if (defUseData.getDefs().contains(cause)) {
                  defNodeVariable = getReturnVariable(summaryEdge);
                } else {
                  for (CExpression pointeeExpression : defUseData.getPointeeDefs()) {
                    if (pointerState
                        .getPossiblePointees(defEdge, pointeeExpression)
                        .contains(cause)) {
                      defNodeVariable = getReturnVariable(summaryEdge);
                    }
                  }
                }
              } else {
                defNodeType = NodeType.STATEMENT;
              }

              if (useEdge instanceof CFunctionCallEdge) {
                builder
                    .node(NodeType.FORMAL_IN, useEdge, Optional.of(cause))
                    .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                    .on(defNodeType, defEdge, defNodeVariable);
                flowDepCounter.inc();
              } else if (useEdge instanceof CFunctionReturnEdge) {
                builder
                    .node(NodeType.FORMAL_OUT, useEdge, Optional.of(cause))
                    .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                    .on(defNodeType, defEdge, defNodeVariable);
                flowDepCounter.inc();
              } else if (useEdge instanceof CFunctionSummaryEdge) {

                CFunctionSummaryEdge summaryEdge = (CFunctionSummaryEdge) useEdge;
                CFunctionCall functionCall =
                    getFunctionCallWithoutParameters((CFunctionSummaryEdge) defEdge);
                EdgeDefUseData defUseData = defUseExtractor.extract(functionCall);
                Optional<MemoryLocation> returnVariable = getReturnVariable(summaryEdge);

                if (defUseData.getPointeeUses().contains(cause)) {
                  builder
                      .node(NodeType.ACTUAL_OUT, useEdge, returnVariable)
                      .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                      .on(defNodeType, defEdge, defNodeVariable);
                  flowDepCounter.inc();
                } else {
                  for (CExpression pointeeExpression : defUseData.getPointeeUses()) {
                    if (pointerState
                        .getPossiblePointees(useEdge, pointeeExpression)
                        .contains(cause)) {
                      builder
                          .node(NodeType.ACTUAL_OUT, useEdge, returnVariable)
                          .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                          .on(defNodeType, defEdge, defNodeVariable);
                      flowDepCounter.inc();
                    }
                  }
                }

                if (foreignDefUseData
                    .getForeignUses(summaryEdge.getFunctionEntry().getFunction())
                    .contains(cause)) {
                  builder
                      .node(NodeType.ACTUAL_IN, useEdge, Optional.of(cause))
                      .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                      .on(defNodeType, defEdge, defNodeVariable);
                  flowDepCounter.inc();
                }

                List<CParameterDeclaration> params =
                    summaryEdge.getFunctionEntry().getFunctionParameters();
                List<CExpression> expressions =
                    summaryEdge
                        .getExpression()
                        .getFunctionCallExpression()
                        .getParameterExpressions();

                for (int index = 0; index < Math.min(params.size(), expressions.size()); index++) {

                  EdgeDefUseData argumentDefUseData =
                      defUseExtractor.extract(expressions.get(index));
                  Optional<MemoryLocation> paramVariable =
                      Optional.of(MemoryLocation.valueOf(params.get(index).getQualifiedName()));

                  if (argumentDefUseData.getUses().contains(cause)) {
                    builder
                        .node(NodeType.ACTUAL_IN, useEdge, paramVariable)
                        .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                        .on(defNodeType, defEdge, defNodeVariable);
                    flowDepCounter.inc();
                  } else {
                    for (CExpression pointeeExpression : defUseData.getPointeeUses()) {
                      if (pointerState
                          .getPossiblePointees(useEdge, pointeeExpression)
                          .contains(cause)) {
                        builder
                            .node(NodeType.ACTUAL_IN, useEdge, paramVariable)
                            .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                            .on(defNodeType, defEdge, defNodeVariable);
                        flowDepCounter.inc();
                      }
                    }
                  }
                }

              } else {
                builder
                    .node(NodeType.STATEMENT, useEdge, Optional.empty())
                    .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                    .on(defNodeType, defEdge, defNodeVariable);
                flowDepCounter.inc();
              }
            }
          };

      boolean isMain = entryNode.equals(cfa.getMainFunction());

      new FlowDepAnalysis(
              domTree,
              Dominance.createDomFrontiers(domTree),
              entryNode,
              isMain ? ImmutableList.of() : globalEdges,
              defUseExtractor,
              pointerState,
              foreignDefUseData,
              declarationEdges,
              dependenceConsumer)
          .run();

      flowDependenceNumber.setNextValue((int) flowDepCounter.getValue());
    }
  }

  private void addControlDependencies() {

    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {

      int controlDepCounter = 0;
      ImmutableSet<ControlDependency> controlDependencies =
          ControlDependenceBuilder.computeControlDependencies(
              entryNode, controlDepsTakeBothAssumptions);

      for (ControlDependency controlDependency : controlDependencies) {

        builder
            .node(NodeType.STATEMENT, controlDependency.getDependentEdge(), Optional.empty())
            .depends(EdgeType.CONTROL_DEPENDENCY, Optional.empty())
            .on(NodeType.STATEMENT, controlDependency.getControlEdge(), Optional.empty());

        controlDepCounter++;
      }

      controlDependenceNumber.setNextValue(controlDepCounter);
    }
  }

  @SuppressWarnings("unused") // old method for computing flow dependences
  private void addFlowDependences()
      throws InvalidConfigurationException, InterruptedException, CPAException {
    FlowDependences flowDependences = FlowDependences.create(cfa, config, logger, shutdownNotifier);
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
        final CFA pCfa,
        final Configuration pConfig,
        final LogManager pLogger,
        final ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException, CPAException, InterruptedException {

      return createDependences(pCfa, pConfig, pLogger, pShutdownNotifier);
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
