// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.AbstractTransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.LoopStructurePostProcessor;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.ReversePostorderPostProcessor;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.VariableClassificationPostProcessor;
import org.sosy_lab.cpachecker.cfa.transformer.CfaFactory;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaFactory;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

final class CfaSimplifications {

  private CfaSimplifications() {}

  /**
   * Returns a simplified CFA with only a single array operation per CFA-edge.
   *
   * @param pConfiguration the configuration to use
   * @param pLogger the logger to use
   * @param pShutdownNotifier the shutdown notifier to use
   * @param pCfa the CFA to simplify
   * @param pVariableGenerator the variable generator to use
   * @return the simplified CFA
   */
  static CFA simplifyArrayAccesses(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      VariableGenerator pVariableGenerator) {

    FlexCfaNetwork graph = FlexCfaNetwork.copy(pCfa);
    Map<CFAEdge, Map<ArrayAccess, CAstNode>> substitution = new HashMap<>();

    // copy of edges to prevent concurrent modification of graph
    for (CFAEdge edge : ImmutableSet.copyOf(graph.edges())) {

      // skip function summary edges here, modify them when the corresponding call edge is modified
      // (to prevent doing the same modification twice)
      if (edge instanceof FunctionSummaryEdge) {
        continue;
      }

      Set<ArrayAccess> remainingArrayAccesses =
          new LinkedHashSet<>(ArrayAccess.findArrayAccesses(edge));

      // finished array access ---> substitute for finished array access
      Map<ArrayAccess, CExpression> finished = new HashMap<>();

      // only a single writing array access per edge is supported
      assert remainingArrayAccesses.stream().filter(ArrayAccess::isWrite).count() <= 1;

      // a single array access is allowed to remain
      while (remainingArrayAccesses.size() > 1) {

        Iterator<ArrayAccess> remainingArrayAccessesIterator = remainingArrayAccesses.iterator();
        while (remainingArrayAccessesIterator.hasNext()) {

          ArrayAccess current = remainingArrayAccessesIterator.next();
          ImmutableSet<ArrayAccess> currentArrayAccesses =
              ArrayAccess.findArrayAccesses(current.getExpression());

          // array accesses can only be processes after all the array accesses they rely on are
          // finished (e.g., a[b[10]] -> process b[10] -> b[10] finished -> process a[b[10]])
          if (Sets.difference(currentArrayAccesses, finished.keySet()).size() == 1) {
            remainingArrayAccessesIterator.remove();

            if (current.isRead()) {

              FileLocation fileLocation = edge.getFileLocation();
              CFANode predecessor = graph.incidentNodes(edge).nodeU();

              CInitializerExpression initializerExpression =
                  new CInitializerExpression(fileLocation, current.getExpression());
              String newVarName = pVariableGenerator.createNewVariableName();
              String newVarQualifiedName =
                  MemoryLocation.forLocalVariable(predecessor.getFunctionName(), newVarName)
                      .getExtendedQualifiedName();

              CVariableDeclaration declaration =
                  new CVariableDeclaration(
                      fileLocation,
                      false,
                      CStorageClass.AUTO,
                      current.getExpression().getExpressionType(),
                      newVarName,
                      newVarName,
                      newVarQualifiedName,
                      initializerExpression);

              CFAEdge newDeclarationEdge =
                  new CDeclarationEdge(
                      "",
                      fileLocation,
                      CFANode.newDummyCFANode(),
                      CFANode.newDummyCFANode(),
                      declaration);

              for (Map.Entry<ArrayAccess, CExpression> finishedEntry : finished.entrySet()) {
                substitution
                    .computeIfAbsent(newDeclarationEdge, key -> new HashMap<>())
                    .put(finishedEntry.getKey(), finishedEntry.getValue());
              }

              graph.insertPredecessor(
                  new CFANode(predecessor.getFunction()), newDeclarationEdge, predecessor);

              CIdExpression substituteExpression = new CIdExpression(fileLocation, declaration);
              substitution
                  .computeIfAbsent(edge, key -> new HashMap<>())
                  .put(current, substituteExpression);
              if (edge instanceof FunctionCallEdge) {
                FunctionSummaryEdge summaryEdge = edge.getPredecessor().getLeavingSummaryEdge();
                assert summaryEdge != null : "Missing summary edge for call edge";
                substitution
                    .computeIfAbsent(summaryEdge, key -> new HashMap<>())
                    .put(current, substituteExpression);
              }

              finished.put(current, substituteExpression);
            }
          }
        }
      }
    }

    BiFunction<CFAEdge, CAstNode, CAstNode> substitutionFunction =
        (edge, originalAstNode) -> {
          Map<ArrayAccess, CAstNode> arrayAccessSubstitution = substitution.get(edge);

          if (arrayAccessSubstitution == null) {
            return originalAstNode;
          }

          // there should be no writing accesses, we only replace reading accesses
          assert arrayAccessSubstitution.keySet().stream()
              .filter(ArrayAccess::isWrite)
              .findAny()
              .isEmpty();

          Function<Map.Entry<ArrayAccess, ?>, CAstNode> extractExpression =
              entry -> entry.getKey().getExpression();
          ImmutableMap<CAstNode, CAstNode> astNodeSubstitution =
              arrayAccessSubstitution.entrySet().stream()
                  .collect(ImmutableMap.toImmutableMap(extractExpression, Map.Entry::getValue));

          // TODO: add support for more edges/statements that can contain writing array accesses
          if (edge instanceof CStatementEdge) {
            CStatement statement = ((CStatementEdge) edge).getStatement();
            if (statement instanceof CExpressionAssignmentStatement) {
              var assignStatement = (CExpressionAssignmentStatement) statement;
              CAstNode rhs =
                  assignStatement
                      .getRightHandSide()
                      .accept(new SubstitutingCAstNodeVisitor(astNodeSubstitution::get));
              return new CExpressionAssignmentStatement(
                  assignStatement.getFileLocation(),
                  assignStatement.getLeftHandSide(),
                  (CExpression) rhs);
            }
          }

          return originalAstNode.accept(new SubstitutingCAstNodeVisitor(astNodeSubstitution::get));
        };

    CfaFactory cfaFactory =
        CCfaFactory.toUnconnectedFunctions()
            .transformEdges(CCfaEdgeTransformer.forSubstitutions(substitutionFunction::apply))
            .executePostProcessor(new ReversePostorderPostProcessor())
            .executePostProcessor(new LoopStructurePostProcessor())
            .toSupergraph()
            .executePostProcessor(new VariableClassificationPostProcessor(pConfiguration));

    return cfaFactory.createCfa(graph, pCfa.getMetadata(), pLogger, pShutdownNotifier);
  }

  /**
   * Returns a simplified CFA where certain loop carried dependencies are eliminated.
   *
   * <p>Example simplification:
   *
   * <pre>
   * {@code int j = 0; for (int i = 0; i < 100; i++) { print(j); j = j + 5; } }
   * </pre>
   *
   * <p>is turned into (output is a simplified depiction)
   *
   * <pre>
   * {@code int j = 0; for (int i = 0; i < 100; i++) { print(i * 5); } }
   * </pre>
   *
   * @param pConfiguration the configuration to use
   * @param pLogger the logger to use
   * @param pShutdownNotifier the shutdown notifier to use
   * @param pCfa the CFA to simplify
   * @return the simplified CFA
   */
  static CFA simplifyIncDecLoopEdges(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa) {

    FlexCfaNetwork graph = FlexCfaNetwork.copy(pCfa);
    Map<CFAEdge, Map<CSimpleDeclaration, CExpression>> substitution = new HashMap<>();

    VariableClassification variableClassification = pCfa.getVarClassification().orElseThrow();
    MachineModel machineModel = pCfa.getMachineModel();
    ValueAnalysisState emptyValueAnalysisState = new ValueAnalysisState(machineModel);

    for (TransformableLoop loop : TransformableLoop.findTransformableLoops(pCfa, pLogger)) {

      String functionName = loop.getLoopNode().getFunctionName();

      for (CFAEdge innerLoopEdge : loop.getInnerLoopEdges()) {
        Optional<SpecialOperation.UpdateAssign> optTargetUpdateOperation =
            SpecialOperation.UpdateAssign.forEdge(
                innerLoopEdge, functionName, machineModel, pLogger, emptyValueAnalysisState);
        if (optTargetUpdateOperation.isPresent()) {

          SpecialOperation.UpdateAssign targetUpdateOperation =
              optTargetUpdateOperation.orElseThrow();
          CSimpleDeclaration targetDeclaration = targetUpdateOperation.getDeclaration();

          // the index cannot cannot be eliminated
          if (targetDeclaration.equals(loop.getIndex().getVariableDeclaration())) {
            continue;
          }

          // we don't remember the value when leaving the loop
          // TODO: this can be further optimized if necessary
          if (loop.hasOutgoingUses(targetDeclaration)) {
            continue;
          }

          String targetQualifiedName = targetDeclaration.getQualifiedName();
          if (variableClassification.getAddressedVariables().contains(targetQualifiedName)) {
            continue;
          }

          if (loop.countInnerLoopDefs(targetDeclaration) > 1) {
            continue;
          }

          if (!loop.isExecutedEveryIteration(innerLoopEdge)) {
            continue;
          }

          CFAEdge updateIndexEdge = loop.getIndex().getUpdateEdge();
          CIdExpression indexIdExpression =
              new CIdExpression(
                  innerLoopEdge.getFileLocation(), loop.getIndex().getVariableDeclaration());

          ImmutableSet<CFAEdge> indexDominated = loop.getDominatedInnerLoopEdges(updateIndexEdge);
          ImmutableSet<CFAEdge> indexPostDominated =
              loop.getPostDominatedInnerLoopEdges(updateIndexEdge);

          ImmutableSet<CFAEdge> targetDominated = loop.getDominatedInnerLoopEdges(innerLoopEdge);
          ImmutableSet<CFAEdge> targetPostDominated =
              loop.getPostDominatedInnerLoopEdges(innerLoopEdge);

          for (CFAEdge edge : Iterables.concat(targetDominated, targetPostDominated)) {

            // formula:
            //   ix_initial : initial index value before entering the loop
            //   ix_step    : index update step
            //   ix_adjust  : index adjustment due to edge order
            //   t_initial  : initial target value before entering the loop
            //   t_step     : target value update step
            //
            //               |-----------  calculate loop iteration  -----------|
            //               |                                                  |
            // t_initial + ( ( ( ( index - ix_initial ) / ix_step ) + ix_adjust ) * t_step )

            // edge permutations => ix_adjust:
            //   indexUpdate targetUpdate edge =>  0
            //   indexUpdate edge targetUpdate => -1
            //   targetUpdate indexUpdate edge =>  0
            //   targetUpdate edge indexUpdate => +1
            //   edge indexUpdate targetUpdate =>  0
            //   edge targetUpdate indexUpdate =>  0

            int indexAdjustValue;
            if (indexDominated.contains(edge) && targetPostDominated.contains(edge)) {
              indexAdjustValue = -1;
            } else if (indexPostDominated.contains(edge) && targetDominated.contains(edge)) {
              indexAdjustValue = 1;
            } else {
              indexAdjustValue = 0;
            }

            // find constant start value for target variable (if it exists)
            Optional<SpecialOperation.ConstantAssign> targetInitialOperation = Optional.empty();
            ImmutableSet<CFAEdge> targetIncomingDefEdges = loop.getIncomingDefs(targetDeclaration);
            if (targetIncomingDefEdges.size() == 1) {
              CFAEdge targetDefEdge = targetIncomingDefEdges.stream().findAny().orElseThrow();
              targetInitialOperation =
                  SpecialOperation.ConstantAssign.forEdge(
                      targetDefEdge, functionName, machineModel, pLogger, emptyValueAnalysisState);
            }

            CExpression substituteExpression;
            if (indexAdjustValue == 0
                && targetUpdateOperation.getStepValue().equals(BigInteger.ONE)
                && targetInitialOperation.isPresent()
                && targetInitialOperation.orElseThrow().getValue().equals(BigInteger.ZERO)
                && loop.getIndex().getUpdateOperation().getStepValue().equals(BigInteger.ONE)
                && loop.getIndex().getInitializeOperation().getValue().equals(BigInteger.ZERO)) {
              substituteExpression = indexIdExpression;
            } else {

              FileLocation fileLocation = edge.getFileLocation();
              TransformableLoop.Index index = loop.getIndex();
              CType indexType = index.getVariableDeclaration().getType();

              // variable expressions and constant expressions

              CExpression indexExpression = indexIdExpression;

              CExpression indexInitialExpression =
                  new CIntegerLiteralExpression(
                      fileLocation, indexType, index.getInitializeOperation().getValue());

              CExpression indexStepExpression =
                  new CIntegerLiteralExpression(
                      fileLocation, indexType, index.getUpdateOperation().getStepValue());

              CExpression indexAdjustExpression =
                  new CIntegerLiteralExpression(
                      fileLocation, indexType, BigInteger.valueOf(indexAdjustValue));

              CExpression targetInitialValue;
              if (targetInitialOperation.isPresent()) {
                targetInitialValue =
                    new CIntegerLiteralExpression(
                        fileLocation, indexType, targetInitialOperation.orElseThrow().getValue());
              } else {
                targetInitialValue = new CIdExpression(fileLocation, targetDeclaration);
              }

              CExpression targetStepValue =
                  new CIntegerLiteralExpression(
                      fileLocation, indexType, targetUpdateOperation.getStepValue());

              // formula expressions

              // index - ix_initial
              CExpression subformula1 =
                  new CBinaryExpression(
                      fileLocation,
                      indexType,
                      indexType,
                      indexExpression,
                      indexInitialExpression,
                      BinaryOperator.MINUS);

              // ( index - ix_initial ) / ix_step
              CExpression subformula2 =
                  new CBinaryExpression(
                      fileLocation,
                      indexType,
                      indexType,
                      subformula1,
                      indexStepExpression,
                      BinaryOperator.DIVIDE);

              // ( ( index - ix_initial ) / ix_step ) + ix_adjust
              CExpression subformula3 =
                  new CBinaryExpression(
                      fileLocation,
                      indexType,
                      indexType,
                      subformula2,
                      indexAdjustExpression,
                      BinaryOperator.PLUS);

              // ( ( ( index - ix_initial ) / ix_step ) + ix_adjust ) * t_step
              CExpression subformula4 =
                  new CBinaryExpression(
                      fileLocation,
                      indexType,
                      indexType,
                      subformula3,
                      targetStepValue,
                      BinaryOperator.MULTIPLY);

              // t_initial + ( ( ( ( index - ix_initial ) / ix_step ) + ix_adjust ) * t_step )
              substituteExpression =
                  new CBinaryExpression(
                      fileLocation,
                      indexType,
                      indexType,
                      targetInitialValue,
                      subformula4,
                      BinaryOperator.PLUS);
            }

            Map<CSimpleDeclaration, CExpression> declarationSubstitution =
                substitution.computeIfAbsent(edge, key -> new HashMap<>());
            declarationSubstitution.put(targetDeclaration, substituteExpression);
          }

          // replace target update edge with empty placeholder
          var endpoints = graph.incidentNodes(innerLoopEdge);
          graph.removeEdge(innerLoopEdge);
          graph.addEdge(
              endpoints,
              new BlankEdge(
                  "",
                  innerLoopEdge.getFileLocation(),
                  innerLoopEdge.getPredecessor(),
                  innerLoopEdge.getSuccessor(),
                  ""));
        }
      }
    }

    BiFunction<CFAEdge, CAstNode, CAstNode> edgeAstSubstitution =
        (edge, originalAstNode) ->
            IdExpressionSubstitutingCAstNodeVisitor.substitute(substitution, edge, originalAstNode);

    CfaFactory cfaFactory =
        CCfaFactory.toUnconnectedFunctions()
            .transformEdges(CCfaEdgeTransformer.forSubstitutions(edgeAstSubstitution::apply))
            .executePostProcessor(new ReversePostorderPostProcessor())
            .executePostProcessor(new LoopStructurePostProcessor())
            .toSupergraph()
            .executePostProcessor(new VariableClassificationPostProcessor(pConfiguration));

    return cfaFactory.createCfa(graph, pCfa.getMetadata(), pLogger, pShutdownNotifier);
  }

  private static final class IdExpressionSubstitutingCAstNodeVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    private final Map<CSimpleDeclaration, CExpression> substitution;

    public IdExpressionSubstitutingCAstNodeVisitor(
        Map<CSimpleDeclaration, CExpression> pSubstitution) {
      substitution = pSubstitution;
    }

    private static CAstNode substitute(
        Map<CFAEdge, Map<CSimpleDeclaration, CExpression>> pSubstitution,
        CFAEdge pEdge,
        CAstNode pOriginalAstNode) {

      Map<CSimpleDeclaration, CExpression> declarationSubstitution = pSubstitution.get(pEdge);

      if (declarationSubstitution != null) {

        IdExpressionSubstitutingCAstNodeVisitor transformingVisitor =
            new IdExpressionSubstitutingCAstNodeVisitor(declarationSubstitution);

        return pOriginalAstNode.accept(transformingVisitor);
      }

      return pOriginalAstNode;
    }

    @Override
    public CAstNode visit(CIdExpression pCIdExpression) throws NoException {

      CSimpleDeclaration declaration = pCIdExpression.getDeclaration();
      CExpression substitute = substitution.get(declaration);

      if (substitute != null) {
        return substitute;
      }

      return super.visit(pCIdExpression);
    }
  }
}
