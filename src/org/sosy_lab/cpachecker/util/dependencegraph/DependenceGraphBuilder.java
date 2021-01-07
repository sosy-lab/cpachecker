// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.ControlDependenceBuilder.ControlDependency;
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
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final EdgeDefUseData.Extractor defUseExtractor;

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
  private SystemDependenceGraph<CFAEdge, MemoryLocation> systemDependenceGraph;

  public DependenceGraphBuilder(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    defUseExtractor =
        new EdgeDefUseData.Extractor() {

          private final Map<Equivalence.Wrapper<Object>, EdgeDefUseData> cache = new HashMap<>();
          private final EdgeDefUseData.Extractor delegateExtractor =
              EdgeDefUseData.createExtractor(considerPointees);

          @Override
          public EdgeDefUseData extract(CFAEdge pEdge) {
            return cache.computeIfAbsent(
                Equivalence.identity().wrap(pEdge), key -> delegateExtractor.extract(pEdge));
          }

          @Override
          public EdgeDefUseData extract(CAstNode pAstNode) {
            return cache.computeIfAbsent(
                Equivalence.identity().wrap(pAstNode), key -> delegateExtractor.extract(pAstNode));
          }
        };

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

    systemDependenceGraph = builder.build();
    dependenceGraphConstructionTimer.stop();

    if (exportDot != null) {
      DotExporter.export(systemDependenceGraph, exportDot, logger);
    }

    return systemDependenceGraph;
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
      // TODO: handle unknown pointers
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
        builder
            .node(NodeType.STATEMENT, callEdge, Optional.empty())
            .depends(EdgeType.FLOW_DEPENDENCY, Optional.empty())
            .on(NodeType.STATEMENT, funcDeclEdge, Optional.empty());
        flowDepCounter.inc();
      }

      DomTree<CFANode> domTree = DominanceUtils.createFunctionDomTree(entryNode);

      DependenceConsumer dependenceConsumer =
          (defEdge, useEdge, cause) -> {
            if (defEdge instanceof CFunctionSummaryEdge && useEdge instanceof CFunctionCallEdge) {

              builder
                  .node(NodeType.FORMAL_IN, useEdge, Optional.of(cause))
                  .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                  .on(NodeType.ACTUAL_IN, defEdge, Optional.of(cause));
              flowDepCounter.inc();

            } else if (defEdge instanceof CFunctionReturnEdge
                && useEdge instanceof CFunctionSummaryEdge) {

              builder
                  .node(NodeType.ACTUAL_OUT, useEdge, Optional.of(cause))
                  .depends(EdgeType.FLOW_DEPENDENCY, Optional.of(cause))
                  .on(NodeType.FORMAL_OUT, defEdge, Optional.of(cause));
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
                defNodeVariable = Optional.of(cause);

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
                CFunctionCall functionCall = getFunctionCallWithoutParameters(summaryEdge);
                EdgeDefUseData defUseData = defUseExtractor.extract(functionCall);
                Optional<MemoryLocation> returnVariable = getReturnVariable(summaryEdge);

                if (defUseData.getUses().contains(cause)) {
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
                    for (CExpression pointeeExpression : argumentDefUseData.getPointeeUses()) {
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

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
            StatInt nodeNumber = new StatInt(StatKind.SUM, "Number of DG nodes");
            nodeNumber.setNextValue(systemDependenceGraph.getNodes().size());
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
}
