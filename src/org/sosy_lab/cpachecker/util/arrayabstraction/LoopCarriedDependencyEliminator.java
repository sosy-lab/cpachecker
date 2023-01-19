// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.AbstractTransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.LoopStructurePostProcessor;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.ReversePostorderPostProcessor;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.VariableClassificationPostProcessor;
import org.sosy_lab.cpachecker.cfa.transformer.CfaFactory;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaFactory;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * A {@link CfaTransformer} that creates CFAs where certain loop carried dependencies are
 * eliminated.
 *
 * <p>Example elimination:
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
 */
final class LoopCarriedDependencyEliminator implements CfaTransformer {

  private final Configuration config;

  LoopCarriedDependencyEliminator(Configuration pConfig) {
    config = pConfig;
  }

  @Override
  public CFA transform(
      CfaNetwork pCfaNetwork,
      CfaMetadata pCfaMetadata,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    FlexCfaNetwork graph = FlexCfaNetwork.copy(pCfaNetwork);
    Map<CFAEdge, Map<CSimpleDeclaration, CExpression>> substitution = new HashMap<>();

    VariableClassification variableClassification =
        pCfaMetadata.getVariableClassification().orElseThrow();
    MachineModel machineModel = pCfaMetadata.getMachineModel();
    ValueAnalysisState emptyValueAnalysisState = new ValueAnalysisState(machineModel);

    for (TransformableLoop loop : TransformableLoop.findTransformableLoops(pCfaMetadata, pLogger)) {

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
            .executePostProcessor(new VariableClassificationPostProcessor(config));

    return cfaFactory.createCfa(graph, pCfaMetadata, pLogger, pShutdownNotifier);
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
