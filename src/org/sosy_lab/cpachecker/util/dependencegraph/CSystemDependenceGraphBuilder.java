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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import java.util.concurrent.TimeUnit;
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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
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
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;
import org.sosy_lab.cpachecker.util.dependencegraph.FlowDepAnalysis.DependenceConsumer;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.NodeType;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

/** Factory for creating a {@link SystemDependenceGraph} from a {@link CFA}. */
@Options(prefix = "dependencegraph")
public class CSystemDependenceGraphBuilder implements StatisticsProvider {

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final EdgeDefUseData.Extractor defUseExtractor;

  private final StatTimer dependenceGraphConstructionTimer = new StatTimer("Time for dep. graph");
  private final StatTimer flowDependenceTimer = new StatTimer("Time for flow deps.");
  private final StatTimer controlDependenceTimer = new StatTimer("Time for control deps.");
  private final StatTimer summaryEdgeTimer = new StatTimer("Time for summary edges");

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

  @Option(
      secure = true,
      name = "onlyReachableFunctions",
      description =
          "Whether to include only functions reachable from the main function in the dependence"
              + " graph.")
  private boolean onlyReachableFunctions = false;

  @Option(
      secure = true,
      name = "pointerAnalysisTime",
      description =
          "The maximum duration a single pointer analysis method is allowed to run (use seconds or"
              + " specify a unit; 0 for infinite).")
  @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
  private TimeSpan pointerAnalysisTime = TimeSpan.ofSeconds(0);

  @Option(
      secure = true,
      name = "pointerStateComputationMethods",
      description =
          "The computation methods used for the pointer analysis. The specified methods are run"
              + " after each other in the specified order, until a method is able to create a valid"
              + " pointer state. The time limit for every method is the same and set by"
              + " pointerAnalysisTime.")
  private List<PointerStateComputationMethod> pointerStateComputationMethods =
      ImmutableList.of(PointerStateComputationMethod.FLOW_SENSITIVE);

  private final SystemDependenceGraph.Builder<AFunctionDeclaration, CFAEdge, MemoryLocation>
      builder;
  private SystemDependenceGraph<AFunctionDeclaration, CFAEdge, MemoryLocation>
      systemDependenceGraph = SystemDependenceGraph.empty();
  private String usedGlobalPointerState = "none";

  public CSystemDependenceGraphBuilder(
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

  public SystemDependenceGraph<AFunctionDeclaration, CFAEdge, MemoryLocation> build()
      throws CPAException {

    dependenceGraphConstructionTimer.start();

    CallGraph<AFunctionDeclaration> callGraph = CallGraphUtils.createCallGraph(cfa);
    ImmutableSet<AFunctionDeclaration> reachableFunctions = ImmutableSet.of();
    if (onlyReachableFunctions) {
      reachableFunctions =
          callGraph.getReachableFrom(ImmutableSet.of(cfa.getMainFunction().getFunction()));
    }

    if (considerFlowDeps) {
      flowDependenceTimer.start();
      try {
        insertFlowDependencies(reachableFunctions);
      } finally {
        flowDependenceTimer.stop();
      }
    }

    if (considerControlDeps) {
      controlDependenceTimer.start();
      try {
        insertControlDependencies(reachableFunctions);
      } finally {
        controlDependenceTimer.stop();
      }
    }

    summaryEdgeTimer.start();

    SummaryEdgeBuilder.insertSummaryEdges(
        builder, callGraph, cfa.getMainFunction().getFunction(), SummaryEdgeBuilder.Method.BATCH);
    summaryEdgeTimer.stop();

    systemDependenceGraph = builder.build();
    dependenceGraphConstructionTimer.stop();

    if (exportDot != null) {
      new CSdgDotExporter().export(systemDependenceGraph, exportDot, logger);
    }

    return systemDependenceGraph;
  }

  private static Optional<AFunctionDeclaration> getOptionalFunction(CFAEdge pEdge) {

    CFANode node =
        pEdge instanceof CFunctionReturnEdge ? pEdge.getPredecessor() : pEdge.getSuccessor();

    return Optional.of(node.getFunction());
  }

  private GlobalPointerState createGlobalPointerState() throws CPAException {

    GlobalPointerState pointerState = null;
    if (considerPointees) {

      for (PointerStateComputationMethod method : pointerStateComputationMethods) {

        final ResourceLimitChecker pointerTimeChecker;
        final ShutdownNotifier pointerShutdownNotifier;
        if (!pointerAnalysisTime.isEmpty()) {
          ShutdownManager pointerShutdownManager =
              ShutdownManager.createWithParent(shutdownNotifier);
          pointerShutdownNotifier = pointerShutdownManager.getNotifier();
          ResourceLimit timeLimit = WalltimeLimit.fromNowOn(pointerAnalysisTime);
          pointerTimeChecker =
              new ResourceLimitChecker(pointerShutdownManager, ImmutableList.of(timeLimit));
          pointerTimeChecker.start();
        } else {
          pointerShutdownNotifier = shutdownNotifier;
          pointerTimeChecker = null;
        }

        try {
          if (method == PointerStateComputationMethod.FLOW_SENSITIVE) {
            pointerState =
                GlobalPointerState.createFlowSensitive(cfa, logger, pointerShutdownNotifier);
          } else if (method == PointerStateComputationMethod.FLOW_INSENSITIVE) {
            pointerState = GlobalPointerState.createFlowInsensitive(cfa, pointerShutdownNotifier);
          } else {
            throw new AssertionError("Invalid PointerStateComputationMethod: " + method);
          }
        } catch (InterruptedException ex) {
          if (shutdownNotifier.shouldShutdown()) {
            break;
          }
        } finally {
          if (pointerTimeChecker != null) {
            pointerTimeChecker.cancel();
          }
        }

        if (pointerState != null) {
          break;
        }
      }

      if (pointerState == null) {
        pointerState = GlobalPointerState.creatUnknown(cfa);
      }

    } else {
      pointerState = GlobalPointerState.EMPTY;
    }

    return pointerState;
  }

  private static ImmutableList<CFAEdge> getGlobalDeclarationEdges(CFA pCfa) {

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

  private ImmutableMap<String, CFAEdge> getDeclarationEdges(List<CFAEdge> pGlobalEdges) {

    Map<String, CFAEdge> declarationEdges = new HashMap<>();

    for (CFAEdge edge : pGlobalEdges) {
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

    return ImmutableMap.copyOf(declarationEdges);
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

  /** Insert declaration edge between a function and the corresponding function declaration edge. */
  private void insertFunctionDeclarationEdge(
      ImmutableMap<String, CFAEdge> pDeclarationEdges, FunctionEntryNode pEntryNode) {

    CFAEdge functionDeclarationEdge = pDeclarationEdges.get(pEntryNode.getFunctionName());
    builder
        .node(
            NodeType.ENTRY,
            Optional.of(pEntryNode.getFunction()),
            Optional.empty(),
            Optional.empty())
        .depends(EdgeType.DECLARATION_EDGE, Optional.empty())
        .on(
            NodeType.STATEMENT,
            getOptionalFunction(functionDeclarationEdge),
            Optional.of(functionDeclarationEdge),
            Optional.empty());
  }

  private void insertDefSummaryUseCallEdges(
      Optional<AFunctionDeclaration> pDefFunction,
      Optional<AFunctionDeclaration> pUseFunction,
      Optional<CFAEdge> pDefEdge,
      MemoryLocation pCause) {

    Optional<AFunctionDeclaration> callerFunction = pDefFunction;
    Optional<AFunctionDeclaration> calleeFunction = pUseFunction;
    Optional<CFAEdge> summaryEdge = pDefEdge;

    // actual-in ----(PARAMETER_EDGE)---> formal-in
    builder
        .node(NodeType.FORMAL_IN, calleeFunction, Optional.empty(), Optional.of(pCause))
        .depends(EdgeType.PARAMETER_EDGE, Optional.of(pCause))
        .on(NodeType.ACTUAL_IN, callerFunction, summaryEdge, Optional.of(pCause));

    // summary edge ----(CONTROL_DEPENDENCY)---> actual-in
    builder
        .node(NodeType.ACTUAL_IN, callerFunction, summaryEdge, Optional.of(pCause))
        .depends(EdgeType.CONTROL_DEPENDENCY, Optional.empty())
        .on(NodeType.STATEMENT, callerFunction, summaryEdge, Optional.empty());
  }

  private void insertDefReturnUseSummaryEdges(
      Optional<AFunctionDeclaration> pDefFunction,
      Optional<AFunctionDeclaration> pUseFunction,
      Optional<CFAEdge> pUseEdge,
      MemoryLocation pCause) {

    Optional<AFunctionDeclaration> callerFunction = pUseFunction;
    Optional<AFunctionDeclaration> calleeFunction = pDefFunction;
    Optional<CFAEdge> summaryEdge = pUseEdge;

    // formal-out ----(PARAMETER_EDGE)---> actual-out
    builder
        .node(NodeType.ACTUAL_OUT, callerFunction, summaryEdge, Optional.of(pCause))
        .depends(EdgeType.PARAMETER_EDGE, Optional.of(pCause))
        .on(NodeType.FORMAL_OUT, calleeFunction, Optional.empty(), Optional.of(pCause));

    // summary edge ----(CONTROL_DEPENDENCY)---> actual-in
    builder
        .node(NodeType.ACTUAL_OUT, callerFunction, summaryEdge, Optional.of(pCause))
        .depends(EdgeType.CONTROL_DEPENDENCY, Optional.empty())
        .on(NodeType.STATEMENT, callerFunction, summaryEdge, Optional.empty());
  }

  private void insertUseSummaryEdges(
      GlobalPointerState pPointerState,
      ForeignDefUseData pForeignDefUseData,
      CFAEdge pDefEdge,
      CFAEdge pUseEdge,
      MemoryLocation pCause,
      EdgeType pEdgeType,
      NodeType pDefNodeType,
      Optional<MemoryLocation> pDefNodeVariable) {

    Optional<AFunctionDeclaration> defFunction = getOptionalFunction(pDefEdge);
    Optional<AFunctionDeclaration> useFunction = getOptionalFunction(pUseEdge);
    Optional<CFAEdge> defEdge = Optional.of(pDefEdge);
    Optional<CFAEdge> useEdge = Optional.of(pUseEdge);

    CFunctionSummaryEdge summaryEdge = (CFunctionSummaryEdge) pUseEdge;
    Optional<MemoryLocation> returnVariable = getReturnVariable(summaryEdge);

    // add dependencies for actual-out nodes that use pCause
    EdgeDefUseData defUseDataWithoutParams =
        defUseExtractor.extract(getFunctionCallWithoutParameters(summaryEdge));
    if (defUseDataWithoutParams.getUses().contains(pCause)) {
      builder
          .node(NodeType.ACTUAL_OUT, defFunction, useEdge, returnVariable)
          .depends(pEdgeType, Optional.of(pCause))
          .on(pDefNodeType, defFunction, defEdge, pDefNodeVariable);
    } else {
      for (CExpression pointeeExpression : defUseDataWithoutParams.getPointeeUses()) {
        if (pPointerState
            .getPossiblePointees(useEdge.orElseThrow(), pointeeExpression)
            .contains(pCause)) {
          builder
              .node(NodeType.ACTUAL_OUT, useFunction, useEdge, returnVariable)
              .depends(pEdgeType, Optional.of(pCause))
              .on(pDefNodeType, defFunction, defEdge, pDefNodeVariable);
        }
      }
    }

    // add dependency for foreign use of pCause
    if (pForeignDefUseData
        .getForeignUses(summaryEdge.getFunctionEntry().getFunction())
        .contains(pCause)) {
      builder
          .node(NodeType.ACTUAL_IN, useFunction, useEdge, Optional.of(pCause))
          .depends(pEdgeType, Optional.of(pCause))
          .on(pDefNodeType, defFunction, defEdge, pDefNodeVariable);
    }

    // add dependencies for parameters that use pCause
    List<CParameterDeclaration> params = summaryEdge.getFunctionEntry().getFunctionParameters();
    CFunctionCallExpression funcCallExpr = summaryEdge.getExpression().getFunctionCallExpression();
    List<CExpression> expressions = funcCallExpr.getParameterExpressions();
    // params.size() != expressions.size() for varargs
    for (int index = 0; index < Math.min(params.size(), expressions.size()); index++) {

      EdgeDefUseData argDefUseData = defUseExtractor.extract(expressions.get(index));
      MemoryLocation paramMemLoc = MemoryLocation.valueOf(params.get(index).getQualifiedName());
      Optional<MemoryLocation> paramVariable = Optional.of(paramMemLoc);

      if (argDefUseData.getUses().contains(pCause)) {
        builder
            .node(NodeType.ACTUAL_IN, useFunction, useEdge, paramVariable)
            .depends(pEdgeType, Optional.of(pCause))
            .on(pDefNodeType, defFunction, defEdge, pDefNodeVariable);
      } else {
        for (CExpression pointeeExpression : argDefUseData.getPointeeUses()) {
          if (pPointerState.getPossiblePointees(pUseEdge, pointeeExpression).contains(pCause)) {
            builder
                .node(NodeType.ACTUAL_IN, useFunction, useEdge, paramVariable)
                .depends(pEdgeType, Optional.of(pCause))
                .on(pDefNodeType, defFunction, defEdge, pDefNodeVariable);
          }
        }
      }
    }
  }

  private void insertFlowDependencies(ImmutableSet<AFunctionDeclaration> pReachableFunctions)
      throws CPAException {

    GlobalPointerState pointerState = createGlobalPointerState();
    if (pointerState != null) {
      usedGlobalPointerState = pointerState.getClass().getSimpleName();
    } else {
      return;
    }

    ForeignDefUseData foreignDefUseData =
        ForeignDefUseData.extract(cfa, defUseExtractor, pointerState);

    ImmutableList<CFAEdge> globalEdges = getGlobalDeclarationEdges(cfa);
    ImmutableMap<String, CFAEdge> declarationEdges = getDeclarationEdges(globalEdges);

    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {

      if (onlyReachableFunctions && !pReachableFunctions.contains(entryNode.getFunction())) {
        continue;
      }

      DomTree<CFANode> domTree = DominanceUtils.createFunctionDomTree(entryNode);

      insertFunctionDeclarationEdge(declarationEdges, entryNode);

      DependenceConsumer dependenceConsumer =
          (pDefEdge, pUseEdge, pCause, pDeclaration) -> {
            Optional<AFunctionDeclaration> defFunction = getOptionalFunction(pDefEdge);
            Optional<AFunctionDeclaration> useFunction = getOptionalFunction(pUseEdge);
            Optional<CFAEdge> defEdge = Optional.of(pDefEdge);
            Optional<CFAEdge> useEdge = Optional.of(pUseEdge);

            if (pDefEdge instanceof CFunctionSummaryEdge && pUseEdge instanceof CFunctionCallEdge) {
              insertDefSummaryUseCallEdges(defFunction, useFunction, defEdge, pCause);
            } else if (pDefEdge instanceof CFunctionReturnEdge
                && pUseEdge instanceof CFunctionSummaryEdge) {
              insertDefReturnUseSummaryEdges(defFunction, useFunction, useEdge, pCause);
            } else {

              EdgeType edgeType =
                  pDeclaration ? EdgeType.DECLARATION_EDGE : EdgeType.FLOW_DEPENDENCY;
              NodeType defNodeType;
              Optional<MemoryLocation> defNodeVariable = Optional.empty();

              if (pDefEdge instanceof CFunctionCallEdge) {
                defEdge = Optional.empty();
                defNodeType = NodeType.FORMAL_IN;
                defNodeVariable = Optional.of(pCause);
              } else if (pDefEdge instanceof CFunctionReturnEdge) {
                defEdge = Optional.empty();
                defNodeType = NodeType.FORMAL_OUT;
                defNodeVariable = Optional.of(pCause);
              } else if (pDefEdge instanceof CFunctionSummaryEdge) {

                defNodeType = NodeType.ACTUAL_OUT;
                defNodeVariable = Optional.of(pCause);

                CFunctionSummaryEdge summaryEdge = (CFunctionSummaryEdge) pDefEdge;
                CFunctionCall functionCall = getFunctionCallWithoutParameters(summaryEdge);
                EdgeDefUseData defUseData = defUseExtractor.extract(functionCall);

                if (defUseData.getDefs().contains(pCause)) {
                  defNodeVariable = getReturnVariable(summaryEdge);
                } else {
                  for (CExpression pointeeExpression : defUseData.getPointeeDefs()) {
                    if (pointerState
                        .getPossiblePointees(pDefEdge, pointeeExpression)
                        .contains(pCause)) {
                      defNodeVariable = getReturnVariable(summaryEdge);
                    }
                  }
                }
              } else {
                defNodeType = NodeType.STATEMENT;
              }

              if (pUseEdge instanceof CFunctionCallEdge) {
                builder
                    .node(NodeType.FORMAL_IN, useFunction, Optional.empty(), Optional.of(pCause))
                    .depends(edgeType, Optional.of(pCause))
                    .on(defNodeType, defFunction, defEdge, defNodeVariable);
              } else if (pUseEdge instanceof CFunctionReturnEdge) {
                builder
                    .node(NodeType.FORMAL_OUT, useFunction, Optional.empty(), Optional.of(pCause))
                    .depends(edgeType, Optional.of(pCause))
                    .on(defNodeType, defFunction, defEdge, defNodeVariable);
              } else if (pUseEdge instanceof CFunctionSummaryEdge) {

                insertUseSummaryEdges(
                    pointerState,
                    foreignDefUseData,
                    pDefEdge,
                    pUseEdge,
                    pCause,
                    edgeType,
                    defNodeType,
                    defNodeVariable);

              } else {
                builder
                    .node(NodeType.STATEMENT, useFunction, useEdge, Optional.empty())
                    .depends(edgeType, Optional.of(pCause))
                    .on(defNodeType, defFunction, defEdge, defNodeVariable);
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
    }
  }

  private void insertControlDependencies(ImmutableSet<AFunctionDeclaration> pReachableFunctions) {

    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {

      if (onlyReachableFunctions && !pReachableFunctions.contains(entryNode.getFunction())) {
        continue;
      }

      ControlDependenceBuilder.insertControlDependencies(
          builder, entryNode, controlDepsTakeBothAssumptions);

      Optional<AFunctionDeclaration> procedure = Optional.of(entryNode.getFunction());

      for (CFAEdge edge : CFAUtils.allEnteringEdges(entryNode)) {
        if (edge instanceof CFunctionCallEdge) {

          CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;

          builder
              .node(NodeType.ENTRY, procedure, Optional.empty(), Optional.empty())
              .depends(EdgeType.CONTROL_DEPENDENCY, Optional.empty())
              .on(NodeType.STATEMENT, procedure, Optional.of(callEdge), Optional.empty());

          CFunctionSummaryEdge summaryEdge = callEdge.getSummaryEdge();
          Optional<AFunctionDeclaration> summaryEdgeProcedure =
              Optional.of(summaryEdge.getPredecessor().getFunction());

          builder
              .node(NodeType.STATEMENT, procedure, Optional.of(callEdge), Optional.empty())
              .depends(EdgeType.CALL_EDGE, Optional.empty())
              .on(
                  NodeType.STATEMENT,
                  summaryEdgeProcedure,
                  Optional.of(summaryEdge),
                  Optional.empty());
        }
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {

            if (dependenceGraphConstructionTimer.getUpdateCount() > 0) {

              put(pOut, 3, dependenceGraphConstructionTimer);
              put(pOut, 4, flowDependenceTimer);
              put(pOut, 4, controlDependenceTimer);
              put(pOut, 4, summaryEdgeTimer);

              int entryNodeCount = systemDependenceGraph.getNodeCount(NodeType.ENTRY);
              put(pOut, 4, "Number of entry nodes", String.valueOf(entryNodeCount));

              int statementCount = systemDependenceGraph.getNodeCount(NodeType.STATEMENT);
              put(pOut, 4, "Number of statement nodes", String.valueOf(statementCount));

              int formalInCount = systemDependenceGraph.getNodeCount(NodeType.FORMAL_IN);
              put(pOut, 4, "Number of formal-in nodes", String.valueOf(formalInCount));

              int formalOutCount = systemDependenceGraph.getNodeCount(NodeType.FORMAL_OUT);
              put(pOut, 4, "Number of formal-out nodes", String.valueOf(formalOutCount));

              int actualInCount = systemDependenceGraph.getNodeCount(NodeType.ACTUAL_IN);
              put(pOut, 4, "Number of actual-in nodes", String.valueOf(actualInCount));

              int actualOutCount = systemDependenceGraph.getNodeCount(NodeType.ACTUAL_OUT);
              put(pOut, 4, "Number of actual-out nodes", String.valueOf(actualOutCount));

              int flowDepCount = systemDependenceGraph.getEdgeCount(EdgeType.FLOW_DEPENDENCY);
              put(pOut, 4, "Number of flow dependencies", String.valueOf(flowDepCount));

              int controlDepCount = systemDependenceGraph.getEdgeCount(EdgeType.CONTROL_DEPENDENCY);
              put(pOut, 4, "Number of control dependencies", String.valueOf(controlDepCount));

              int declEdgeCount = systemDependenceGraph.getEdgeCount(EdgeType.DECLARATION_EDGE);
              put(pOut, 4, "Number of declaration edges", String.valueOf(declEdgeCount));

              int callEdgeCount = systemDependenceGraph.getEdgeCount(EdgeType.CALL_EDGE);
              put(pOut, 4, "Number of call edges", String.valueOf(callEdgeCount));

              int paramEdgeCount = systemDependenceGraph.getEdgeCount(EdgeType.PARAMETER_EDGE);
              put(pOut, 4, "Number of parameter edges", String.valueOf(paramEdgeCount));

              int summaryEdgeCount = systemDependenceGraph.getEdgeCount(EdgeType.SUMMARY_EDGE);
              put(pOut, 4, "Number of summary edges", String.valueOf(summaryEdgeCount));

              put(pOut, 4, "Used GlobalPointerState", usedGlobalPointerState);
            }
          }

          @Override
          public String getName() {
            return ""; // empty name for nice output under CFACreator statistics
          }
        });
  }

  private enum PointerStateComputationMethod {
    FLOW_SENSITIVE,
    FLOW_INSENSITIVE;
  }

  private static final class CSdgDotExporter
      extends SdgDotExporter<AFunctionDeclaration, CFAEdge, MemoryLocation> {

    @Override
    protected String getProcedureLabel(AFunctionDeclaration pContext) {
      return pContext.toString();
    }

    @Override
    protected String getNodeStyle(Node<AFunctionDeclaration, CFAEdge, MemoryLocation> pNode) {

      Optional<CFAEdge> optCfaEdge = pNode.getStatement();

      if (optCfaEdge.isPresent()) {
        switch (optCfaEdge.orElseThrow().getEdgeType()) {
          case AssumeEdge:
            return "shape=\"diamond\",color=\"{color}\"";
          case FunctionCallEdge:
            return "shape=\"ellipse\",peripheries=\"2\",color=\"{color}\"";
          case BlankEdge:
            return "shape=\"box\",color=\"{color}\"";
          default:
            return "shape=\"ellipse\",color=\"{color}\"";
        }
      }

      return "";
    }

    @Override
    protected String getNodeLabel(Node<AFunctionDeclaration, CFAEdge, MemoryLocation> pNode) {

      StringBuilder sb = new StringBuilder();

      if (pNode.getType() != NodeType.STATEMENT) {
        
        sb.append(pNode.getType());
        sb.append(" of ");

        if (pNode.getType() == NodeType.ENTRY) {
          sb.append(String.valueOf(pNode.getProcedure().orElse(null)));
        } else {
          sb.append(String.valueOf(pNode.getVariable().orElse(null)));
        }

        sb.append("\\n");
      }

      Optional<CFAEdge> optCfaEdge = pNode.getStatement();

      if (optCfaEdge.isPresent()) {
        CFAEdge cfaEdge = optCfaEdge.orElseThrow();
        sb.append(cfaEdge.getPredecessor());
        sb.append(" ---> ");
        sb.append(cfaEdge.getSuccessor());
        sb.append(", ");
        sb.append(cfaEdge.getFileLocation());
        sb.append(":\\n");
        sb.append(cfaEdge.getDescription());
      }

      return sb.toString();
    }

    @Override
    protected boolean isHighlighted(Node<AFunctionDeclaration, CFAEdge, MemoryLocation> pNode) {
      return false;
    }

    @Override
    protected boolean isHighlighted(
        EdgeType pEdgeType,
        Node<AFunctionDeclaration, CFAEdge, MemoryLocation> pPredecessor,
        Node<AFunctionDeclaration, CFAEdge, MemoryLocation> pSuccessor) {
      return false;
    }
  }
}
