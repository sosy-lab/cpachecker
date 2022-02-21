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
import com.google.common.collect.TreeMultimap;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMutableNetwork;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
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
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.CFASimplifier;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Utility class for turning {@link Slice} instances into {@link CFA} instances. */
final class SliceToCfaConversion {

  private static final String IRRELEVANT_EDGE_DESCRIPTION = "edge irrelevant to program slice";

  private SliceToCfaConversion() {}

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
   * Returns whether the specified edge has at least one endpoint in an irrelevant function (i.e., a
   * function that contains no edges that are part of the slice).
   */
  private static boolean isIrrelevantFunctionEdge(
      ImmutableSet<AFunctionDeclaration> pRelevantFunctions, CFAEdge pEdge) {

    return !pRelevantFunctions.contains(pEdge.getPredecessor().getFunction())
        || !pRelevantFunctions.contains(pEdge.getSuccessor().getFunction());
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
   * Returns a substitution that maps CFA nodes and their contained AST nodes to AST nodes that only
   * contain parts relevant to the specified slice.
   */
  private static BiFunction<CFANode, CAstNode, @Nullable CAstNode>
      createAstNodeSubstitutionForCfaNodes(
          Slice pSlice,
          Function<AFunctionDeclaration, Optional<CVariableDeclaration>>
              pFunctionToReturnVariable) {

    var transformingVisitor =
        new RelevantFunctionDeclarationTransformingVisitor(pSlice, pFunctionToReturnVariable);

    return (cfaNode, astNode) -> {
      CFunctionDeclaration functionDeclaration = (CFunctionDeclaration) cfaNode.getFunction();
      CFunctionDeclaration relevantFunctionDeclaration =
          (CFunctionDeclaration) functionDeclaration.accept(transformingVisitor);

      if (astNode instanceof AFunctionDeclaration) {
        return relevantFunctionDeclaration;
      }

      if (cfaNode instanceof CFunctionEntryNode && astNode instanceof CVariableDeclaration) {

        if (relevantFunctionDeclaration.getType().getReturnType() != CVoidType.VOID) {
          return ((CFunctionEntryNode) cfaNode).getReturnVariable().orElseThrow();
        }

        // return type of function is void, so no return variable exists
        return null;
      }

      return astNode;
    };
  }

  /**
   * Returns a substitution that maps CFA edges and their contained AST nodes to AST nodes that only
   * contain parts relevant to the specified slice.
   */
  private static BiFunction<CFAEdge, CAstNode, CAstNode> createAstNodeSubstitutionForCfaEdges(
      Slice pSlice,
      Function<AFunctionDeclaration, Optional<CVariableDeclaration>> pFunctionToReturnVariable) {

    return (edge, astNode) -> {
      var transformingVisitor =
          new RelevantCAstNodeTransformingVisitor(pSlice, pFunctionToReturnVariable, edge);

      return astNode.accept(transformingVisitor);
    };
  }

  /**
   * Returns the optional return variable (void functions don't have return variables) for the
   * specified function.
   */
  private static Optional<CVariableDeclaration> getReturnVariable(FunctionEntryNode pEntryNode) {

    return pEntryNode
        .getReturnVariable()
        .map(returnVariable -> (CVariableDeclaration) returnVariable);
  }

  /**
   * Creates a simplified CFA for the specified CFA using {@link
   * CFASimplifier#simplifyCFA(MutableCFA)}.
   */
  private static CFA createSimplifiedCfa(CFA pCfa) {

    NavigableMap<String, FunctionEntryNode> functionEntryNodes = new TreeMap<>();
    TreeMultimap<String, CFANode> allNodes = TreeMultimap.create();

    for (CFANode node : pCfa.getAllNodes()) {

      String functionName = node.getFunction().getQualifiedName();
      allNodes.put(functionName, node);

      if (node instanceof FunctionEntryNode) {
        functionEntryNodes.put(functionName, (FunctionEntryNode) node);
      }
    }

    MutableCFA mutableSliceCfa =
        new MutableCFA(
            pCfa.getMachineModel(),
            functionEntryNodes,
            allNodes,
            pCfa.getMainFunction(),
            pCfa.getFileNames(),
            pCfa.getLanguage());

    CFASimplifier.simplifyCFA(mutableSliceCfa);

    return mutableSliceCfa.makeImmutableCFA(mutableSliceCfa.getVarClassification());
  }

  /**
   * Creates a {@link CFA} that matches the specified {@link Slice} as closely as possible.
   *
   * @param pConfig the configuration to use
   * @param pLogger the logger to use during conversion
   * @param pSlice the slice to create a CFA for
   * @return the CFA created for the specified slice
   * @throws NullPointerException if any parameter is {@code null}
   */
  public static CFA convert(Configuration pConfig, LogManager pLogger, Slice pSlice) {

    ImmutableSet<CFAEdge> relevantEdges = pSlice.getRelevantEdges();

    // relevant functions are functions that contain at least one relevant edge
    ImmutableSet<AFunctionDeclaration> relevantFunctions =
        Collections3.transformedImmutableSetCopy(
            relevantEdges, edge -> edge.getSuccessor().getFunction());

    CfaMutableNetwork graph = CfaMutableNetwork.of(pSlice.getOriginalCfa());

    ImmutableList<CFAEdge> irrelevantFunctionEdges =
        graph.edges().stream()
            .filter(edge -> isIrrelevantFunctionEdge(relevantFunctions, edge))
            .collect(ImmutableList.toImmutableList());
    irrelevantFunctionEdges.forEach(graph::removeEdge);

    ImmutableList<CFAEdge> irrelevantEdges =
        graph.edges().stream()
            .filter(edge -> !relevantEdges.contains(edge) && isReplaceableEdge(edge))
            .collect(ImmutableList.toImmutableList());
    irrelevantEdges.forEach(edge -> graph.replace(edge, createNoopBlankEdge(edge)));

    ImmutableList<CFANode> irrelevantNodes =
        graph.nodes().stream()
            .filter(node -> graph.adjacentNodes(node).isEmpty())
            .collect(ImmutableList.toImmutableList());
    irrelevantNodes.forEach(graph::removeNode);

    ImmutableMap<AFunctionDeclaration, Optional<CVariableDeclaration>> functionToReturnVariableMap =
        pSlice.getOriginalCfa().getAllFunctionHeads().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    entryNode -> entryNode.getFunction(), SliceToCfaConversion::getReturnVariable));

    CFA sliceCfa =
        CCfaTransformer.createCfa(
            pConfig,
            pLogger,
            pSlice.getOriginalCfa(),
            graph,
            createAstNodeSubstitutionForCfaEdges(pSlice, functionToReturnVariableMap::get),
            createAstNodeSubstitutionForCfaNodes(pSlice, functionToReturnVariableMap::get));

    return createSimplifiedCfa(sliceCfa);
  }

  /**
   * {@link TransformingCAstNodeVisitor} for creating function declarations that only contain
   * parameters and return values relevant to the specified slice.
   */
  private static class RelevantFunctionDeclarationTransformingVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    private final Slice slice;
    private final Function<AFunctionDeclaration, Optional<CVariableDeclaration>>
        functionToReturnVariable;

    private RelevantFunctionDeclarationTransformingVisitor(
        Slice pSlice,
        Function<AFunctionDeclaration, Optional<CVariableDeclaration>> pFunctionToReturnVariable) {

      slice = pSlice;
      functionToReturnVariable = pFunctionToReturnVariable;
    }

    @Override
    public CAstNode visit(CFunctionDeclaration pFunctionDeclaration) {

      ImmutableSet<ASimpleDeclaration> relevantDeclarations = slice.getRelevantDeclarations();
      List<CParameterDeclaration> parameters = pFunctionDeclaration.getParameters();

      ImmutableList<CParameterDeclaration> relevantParameters =
          parameters.stream()
              .filter(relevantDeclarations::contains)
              .map(parameter -> (CParameterDeclaration) parameter.accept(this))
              .collect(ImmutableList.toImmutableList());

      Optional<CVariableDeclaration> returnVariable =
          functionToReturnVariable.apply(pFunctionDeclaration);
      CType relevantReturnType =
          returnVariable
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
          relevantParameters);
    }
  }

  /**
   * {@link TransformingCAstNodeVisitor} for creating AST nodes that only contain parts that are
   * relevant to the specified slice at the specified CFA edge.
   */
  private static final class RelevantCAstNodeTransformingVisitor
      extends RelevantFunctionDeclarationTransformingVisitor {

    private static final String IRRELEVANT_VALUE_PREFIX = "__IRRELEVANT_VALUE_";

    private final Slice slice;
    private final CFAEdge edge;

    private RelevantCAstNodeTransformingVisitor(
        Slice pSlice,
        Function<AFunctionDeclaration, Optional<CVariableDeclaration>> pFunctionToReturnVariable,
        CFAEdge pEdge) {
      super(pSlice, pFunctionToReturnVariable);

      slice = pSlice;
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
