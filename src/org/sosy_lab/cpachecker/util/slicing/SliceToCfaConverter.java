// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.AbstractTransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.TransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFASimplifier;
import org.sosy_lab.cpachecker.cfa.transformer.CfaFactory;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaEdgeAstSubstitution;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaFactory;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaNodeAstSubstitution;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaNodeTransformer;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** A {@link CfaTransformer} that creates a CFA for a {@link Slice program slice}. */
final class SliceToCfaConverter implements CfaTransformer {

  private static final String IRRELEVANT_EDGE_DESCRIPTION = "edge irrelevant to program slice";

  private final Slice slice;
  // relevant functions are functions that contain at least one relevant edge
  private final ImmutableMap<AFunctionDeclaration, FunctionEntryNode> relevantFunctions;

  private SliceToCfaConverter(
      Slice pSlice, ImmutableMap<AFunctionDeclaration, FunctionEntryNode> pRelevantFunctions) {
    slice = pSlice;
    relevantFunctions = pRelevantFunctions;
  }

  static CfaTransformer forProgramSlice(Slice pSlice) {
    // determine functions that are part of the specified slice
    ImmutableMap<AFunctionDeclaration, FunctionEntryNode> relevantFunctions =
        pSlice.getRelevantEdges().stream()
            .flatMap(edge -> ImmutableSet.of(edge.getPredecessor(), edge.getSuccessor()).stream())
            .filter(node -> node instanceof FunctionEntryNode)
            .map(node -> (FunctionEntryNode) node)
            .collect(
                ImmutableMap.toImmutableMap(
                    entryNode -> entryNode.getFunction(), entryNode -> entryNode));
    return new SliceToCfaConverter(pSlice, relevantFunctions);
  }

  /**
   * Returns a no-operation blank edge with the same file location and endpoints as the specified
   * edge.
   */
  private static CFAEdge createNoopBlankEdge(CFAEdge pEdge) {
    return new BlankEdge(
        IRRELEVANT_EDGE_DESCRIPTION,
        pEdge.getFileLocation(),
        pEdge.getPredecessor(),
        pEdge.getSuccessor(),
        IRRELEVANT_EDGE_DESCRIPTION);
  }

  /**
   * Returns whether the specified edge should be replaced with a no-op blank edge if the edge is
   * not part of the slice.
   */
  private static boolean isReplaceableEdge(CFAEdge pEdge) {
    // Replacing function call/return edges leads to invalid CFAs.
    // Irrelevant assume edges are replaced during CFA simplification, which requires assume edges
    // instead of blank edges to work properly.
    return !(pEdge instanceof FunctionCallEdge)
        && !(pEdge instanceof FunctionReturnEdge)
        && !(pEdge instanceof AssumeEdge);
  }

  /**
   * Returns whether the specified edge has at least one endpoint in an irrelevant function (i.e., a
   * function that contains no edges that are part of the slice).
   */
  private boolean isIrrelevantFunctionEdge(CFAEdge pEdge) {
    return !relevantFunctions.containsKey(pEdge.getPredecessor().getFunction())
        || !relevantFunctions.containsKey(pEdge.getSuccessor().getFunction());
  }

  /**
   * Returns whether the specified CFA node should be removed because it doesn't serve any
   * meaningful purpose in the specified {@link CfaNetwork}.
   */
  private boolean isIrrelevantNode(CfaNetwork pGraph, CFANode pNode) {
    if (pNode instanceof FunctionExitNode) {
      return !relevantFunctions.containsKey(pNode.getFunction());
    }
    return pGraph.adjacentNodes(pNode).isEmpty();
  }

  @Override
  public CFA transform(
      CfaNetwork pCfaNetwork,
      CfaMetadata pCfaMetadata,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    ImmutableSet<CFAEdge> relevantEdges = slice.getRelevantEdges();
    FlexCfaNetwork graph = FlexCfaNetwork.copy(pCfaNetwork);

    ImmutableList<CFAEdge> irrelevantFunctionEdges =
        graph.edges().stream()
            .filter(edge -> isIrrelevantFunctionEdge(edge))
            .collect(ImmutableList.toImmutableList());
    irrelevantFunctionEdges.forEach(graph::removeEdge);

    ImmutableList<CFAEdge> irrelevantEdges =
        graph.edges().stream()
            .filter(edge -> !relevantEdges.contains(edge) && isReplaceableEdge(edge))
            .collect(ImmutableList.toImmutableList());
    irrelevantEdges.forEach(edge -> graph.replaceEdge(edge, createNoopBlankEdge(edge)));

    ImmutableList<CFANode> irrelevantNodes =
        graph.nodes().stream()
            .filter(node -> isIrrelevantNode(graph, node))
            .collect(ImmutableList.toImmutableList());
    irrelevantNodes.forEach(graph::removeNode);

    // if the program slice is empty, return a CFA containing an empty main function
    if (relevantEdges.isEmpty()) {
      FunctionEntryNode mainEntryNode = pCfaMetadata.getMainFunctionEntry();
      graph.addNode(mainEntryNode);
      mainEntryNode.getExitNode().ifPresent(graph::addNode);

      return CCfaFactory.CLONER.createCfa(graph, pCfaMetadata, pLogger, pShutdownNotifier);
    }

    CCfaNodeTransformer nodeTransformer =
        CCfaNodeTransformer.forSubstitutions(new RelevantNodeAstSubstitution());
    CCfaEdgeTransformer edgeTransformer =
        CCfaEdgeTransformer.forSubstitutions(new RelevantEdgeAstSubstitution());
    CfaFactory cfaFactory =
        CCfaFactory.toUnconnectedFunctions()
            .transformNodes(nodeTransformer)
            .transformEdges(edgeTransformer)
            .executePostProcessor(new CFASimplifier())
            .toSupergraph();
    CFA sliceCfa = cfaFactory.createCfa(graph, pCfaMetadata, pLogger, pShutdownNotifier);
    return sliceCfa;
  }

  /**
   * A substitution that maps CFA nodes and their contained AST nodes to AST nodes that only contain
   * parts relevant to the specified program slice.
   */
  private final class RelevantNodeAstSubstitution implements CCfaNodeAstSubstitution {

    private final RelevantFunctionDeclarationTransformingVisitor functionTransformingVisitor;

    private RelevantNodeAstSubstitution() {
      functionTransformingVisitor = new RelevantFunctionDeclarationTransformingVisitor();
    }

    @Override
    public CFunctionDeclaration apply(CFANode pNode, CFunctionDeclaration pFunction) {
      return (CFunctionDeclaration) pFunction.accept(functionTransformingVisitor);
    }

    @Override
    public Optional<CVariableDeclaration> apply(
        CFunctionEntryNode pFunctionEntryNode, Optional<CVariableDeclaration> pReturnVariable) {
      CFunctionDeclaration functionDeclaration =
          (CFunctionDeclaration) pFunctionEntryNode.getFunction();
      CFunctionDeclaration relevantFunctionDeclaration =
          (CFunctionDeclaration) functionDeclaration.accept(functionTransformingVisitor);
      if (relevantFunctionDeclaration.getType().getReturnType() != CVoidType.VOID) {
        return pFunctionEntryNode.getReturnVariable();
      }
      // return type of function is `void`, so no return variable exists
      return Optional.empty();
    }
  }

  /**
   * A substitution that maps CFA edges and their contained AST nodes to AST nodes that only contain
   * parts relevant to the specified program slice.
   */
  private final class RelevantEdgeAstSubstitution implements CCfaEdgeAstSubstitution {

    @Override
    public CAstNode apply(CFAEdge pEdge, CAstNode pAstNode) {
      // The transforming visitor only works for relevant edges.
      // Irrelevant edges are going to be removed in CFA post-processing (irrelevant assume edges
      // are kept for CFA simplification), so we just keep their original AST nodes.
      if (slice.getRelevantEdges().contains(pEdge)) {
        return pAstNode.accept(new RelevantCAstNodeTransformingVisitor(pEdge));
      }
      return pAstNode;
    }
  }

  /**
   * {@link TransformingCAstNodeVisitor} for creating function declarations that only contain
   * parameters and return values relevant to the specified slice.
   */
  private class RelevantFunctionDeclarationTransformingVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    @Override
    public CAstNode visit(CFunctionDeclaration pFunctionDeclaration) {
      @Nullable FunctionEntryNode entryNode = relevantFunctions.get(pFunctionDeclaration);
      // If a function lacks a corresponding function entry node, the function is defined somewhere
      // else and we cannot change the declaration.
      if (entryNode == null) {
        return pFunctionDeclaration;
      }

      ImmutableSet<ASimpleDeclaration> relevantDeclarations = slice.getRelevantDeclarations();
      List<CParameterDeclaration> parameters = pFunctionDeclaration.getParameters();
      ImmutableList<CParameterDeclaration> relevantParameters =
          parameters.stream()
              .filter(relevantDeclarations::contains)
              .map(parameter -> (CParameterDeclaration) parameter.accept(this))
              .collect(ImmutableList.toImmutableList());

      CType relevantReturnType =
          entryNode
              .getReturnVariable()
              .map(returnVariable -> (CVariableDeclaration) returnVariable)
              .filter(relevantDeclarations::contains)
              .map(variable -> variable.getType())
              .orElse(CVoidType.VOID);

      CFunctionType functionType = pFunctionDeclaration.getType();
      boolean relevantTakesVarargs =
          functionType.takesVarArgs()
              && !parameters.isEmpty()
              && relevantDeclarations.contains(Iterables.getLast(parameters));
      CFunctionType relevantFunctionType =
          new CFunctionTypeWithNames(relevantReturnType, relevantParameters, relevantTakesVarargs);

      return new CFunctionDeclaration(
          pFunctionDeclaration.getFileLocation(),
          relevantFunctionType,
          pFunctionDeclaration.getName(),
          pFunctionDeclaration.getOrigName(),
          relevantParameters,
          pFunctionDeclaration.getAttributes());
    }
  }

  /**
   * {@link TransformingCAstNodeVisitor} for creating AST nodes that only contain parts that are
   * relevant to the specified slice at the specified CFA edge.
   */
  private final class RelevantCAstNodeTransformingVisitor
      extends RelevantFunctionDeclarationTransformingVisitor {

    private static final String IRRELEVANT_VALUE_PREFIX = "__IRRELEVANT_VALUE_";

    private final CFAEdge edge;

    private RelevantCAstNodeTransformingVisitor(CFAEdge pEdge) {
      edge = pEdge;
    }

    @Override
    public CAstNode visit(CVariableDeclaration pVariableDeclaration) {
      MemoryLocation memoryLocation = MemoryLocation.forDeclaration(pVariableDeclaration);
      CInitializer initializer = pVariableDeclaration.getInitializer();
      if (initializer != null && slice.isRelevantDef(edge, memoryLocation)) {
        // The variable declaration's initializer expression must be visited during transformation.
        // Only visitors that prevent infinite recursive visit-calls due to cyclic references
        // between variable declarations and their initializer expressions should be used.
        // The superclass implementation prevents these infinite recursive visit-calls.
        return super.visit(pVariableDeclaration);
      } else {
        return new CVariableDeclaration(
            pVariableDeclaration.getFileLocation(),
            pVariableDeclaration.isGlobal(),
            pVariableDeclaration.getCStorageClass(),
            pVariableDeclaration.getType(),
            pVariableDeclaration.getName(),
            pVariableDeclaration.getOrigName(),
            pVariableDeclaration.getQualifiedName(),
            null);
      }
    }

    @Override
    public CAstNode visit(CFunctionCallExpression pFunctionCallExpression) {
      ImmutableSet<ASimpleDeclaration> relevantDeclarations = slice.getRelevantDeclarations();
      CFunctionDeclaration functionDeclaration = pFunctionCallExpression.getDeclaration();

      List<CParameterDeclaration> parameters = functionDeclaration.getParameters();
      List<CExpression> arguments = pFunctionCallExpression.getParameterExpressions();
      ImmutableList.Builder<CExpression> relevantArgumentsBuilder = ImmutableList.builder();

      for (int index = 0; index < arguments.size(); index++) {
        CExpression argument = arguments.get(index);
        // varargs can lead to more arguments than parameters
        if (index >= parameters.size()
            && relevantDeclarations.contains(Iterables.getLast(parameters))) {
          assert functionDeclaration.getType().takesVarArgs();
          relevantArgumentsBuilder.add((CExpression) argument.accept(this));
          continue;
        }

        CParameterDeclaration parameter = parameters.get(index);
        MemoryLocation parameterMemLoc = MemoryLocation.forDeclaration(parameter);
        if (relevantDeclarations.contains(parameter)) {
          if (slice.isRelevantDef(edge, parameterMemLoc)) { // parameter and argument relevant
            relevantArgumentsBuilder.add((CExpression) argument.accept(this));
          } else { // parameter relevant and argument not relevant
            String irrelevantValueVariableName =
                String.format(
                    "%s%d_%d_%d",
                    IRRELEVANT_VALUE_PREFIX,
                    edge.getPredecessor().getNodeNumber(),
                    edge.getSuccessor().getNodeNumber(),
                    index);
            var irrelevantValueVariableDeclaration =
                new CVariableDeclaration(
                    argument.getFileLocation(),
                    false,
                    CStorageClass.AUTO,
                    argument.getExpressionType(),
                    irrelevantValueVariableName,
                    irrelevantValueVariableName,
                    irrelevantValueVariableName,
                    null);
            var irrelevantValueExpression =
                new CIdExpression(argument.getFileLocation(), irrelevantValueVariableDeclaration);
            relevantArgumentsBuilder.add(irrelevantValueExpression);
          }
        } else { // parameter not relevant
          assert !slice.isRelevantDef(edge, parameterMemLoc)
              : "Argument is relevant but corresponding parameter is not";
        }
      }

      CFunctionDeclaration relevantFunctionDeclaration =
          (CFunctionDeclaration) functionDeclaration.accept(this);

      return new CFunctionCallExpression(
          pFunctionCallExpression.getFileLocation(),
          relevantFunctionDeclaration.getType().getReturnType(),
          (CExpression) pFunctionCallExpression.getFunctionNameExpression().accept(this),
          relevantArgumentsBuilder.build(),
          relevantFunctionDeclaration);
    }

    @Override
    public CAstNode visit(CFunctionCallAssignmentStatement pFunctionCallAssignmentStatement) {
      CFunctionCallExpression relevantFunctionCallExpression =
          (CFunctionCallExpression)
              pFunctionCallAssignmentStatement.getRightHandSide().accept(this);
      CFunctionDeclaration relevantFunctionDeclaration =
          relevantFunctionCallExpression.getDeclaration();

      if (relevantFunctionDeclaration.getType().getReturnType() == CVoidType.VOID) {
        return new CFunctionCallStatement(
            pFunctionCallAssignmentStatement.getFileLocation(), relevantFunctionCallExpression);
      }

      return new CFunctionCallAssignmentStatement(
          pFunctionCallAssignmentStatement.getFileLocation(),
          (CLeftHandSide) pFunctionCallAssignmentStatement.getLeftHandSide().accept(this),
          relevantFunctionCallExpression);
    }
  }
}
