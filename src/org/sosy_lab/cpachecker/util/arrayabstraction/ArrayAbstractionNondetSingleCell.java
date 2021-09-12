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
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaTransformer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.AbstractTransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.TransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaNodeTransformer;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ArrayAbstractionNondetSingleCell {

  private ArrayAbstractionNondetSingleCell() {}

  private static CIdExpression createVariableCIdExpression(TransformableArray pTransformableArray) {

    CType type = pTransformableArray.getArrayType().getType();
    String arrayName = pTransformableArray.getMemoryLocation().getIdentifier();
    String name = "__array_witness_variable_" + arrayName;

    return ArrayAbstractionUtils.createCIdExpression(type, MemoryLocation.forIdentifier(name));
  }

  private static CIdExpression createIndexCIdExpression(TransformableArray pTransformableArray) {

    CType type =
        new CSimpleType(
            true, false, CBasicType.UNSPECIFIED, false, false, false, true, false, false, true);
    String arrayName = pTransformableArray.getMemoryLocation().getIdentifier();
    String name = "__array_witness_index_" + arrayName;

    return ArrayAbstractionUtils.createCIdExpression(type, MemoryLocation.forIdentifier(name));
  }

  private static ImmutableSet<CExpression> getConditionExpressions(
      ImmutableSet<TransformableArray> pTransformableArrays, TransformableLoop pTransformableLoop) {

    CBinaryExpression loopCondition =
        (CBinaryExpression) pTransformableLoop.getEnterLoopCfaEdge().getExpression();

    CExpressionAssignmentStatement updateStatement =
        (CExpressionAssignmentStatement)
            pTransformableLoop.getUpdateLoopIndexCfaEdge().getStatement();
    CBinaryExpression.BinaryOperator updateOperator =
        ((CBinaryExpression) updateStatement.getRightHandSide()).getOperator();
    CBinaryExpression.BinaryOperator initConditionOperator =
        updateOperator == CBinaryExpression.BinaryOperator.PLUS
            ? CBinaryExpression.BinaryOperator.GREATER_EQUAL
            : CBinaryExpression.BinaryOperator.LESS_EQUAL;
    CExpression initCondition =
        new CBinaryExpression(
            FileLocation.DUMMY,
            loopCondition.getExpressionType(),
            loopCondition.getCalculationType(),
            pTransformableLoop.getLoopIndexExpression(),
            pTransformableLoop.getLoopIndexInitExpression(),
            initConditionOperator);

    CIntegerLiteralExpression zeroLiteral =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            pTransformableLoop.getLoopIndexExpression().getExpressionType(),
            BigInteger.ZERO);
    CExpression indexGreaterEqualZeroCondition =
        new CBinaryExpression(
            FileLocation.DUMMY,
            zeroLiteral.getExpressionType(),
            zeroLiteral.getExpressionType(),
            pTransformableLoop.getLoopIndexExpression(),
            zeroLiteral,
            CBinaryExpression.BinaryOperator.GREATER_EQUAL);

    ImmutableSet.Builder<CExpression> conditionsBuilder = ImmutableSet.builder();

    for (TransformableArray transformableArray : pTransformableArrays) {
      TransformingCAstNodeVisitor<NoException> astTransformer =
          new ReplaceLoopIndexAstTransformingVisitor(transformableArray, pTransformableLoop);
      conditionsBuilder.add((CExpression) astTransformer.transform(indexGreaterEqualZeroCondition));
      conditionsBuilder.add((CExpression) astTransformer.transform(initCondition));
      conditionsBuilder.add((CExpression) astTransformer.transform(loopCondition));
    }

    return conditionsBuilder.build();
  }

  private static void replaceLoopWithBranching(
      CfaTransformer<?, ?> pTransformer,
      ImmutableSet<TransformableArray> pTransformableArrays,
      TransformableLoop pTransformableLoop) {

    CfaTransformer.Node loopNode =
        pTransformer.get(pTransformableLoop.getLoopCfaNode()).orElseThrow();

    CfaTransformer.Edge initEdge = null;
    CfaTransformer.Edge updateEdge = null;
    CfaTransformer.Edge continueEdge = null;
    CfaTransformer.Edge breakEdge = null;

    for (CfaTransformer.Edge edge :
        Iterables.concat(loopNode.iterateEntering(), loopNode.iterateLeaving())) {
      CFAEdge oldCfaEdge = edge.getOriginalCfaEdge();

      // skip blank edges, required for while loops
      if (oldCfaEdge.getEdgeType() == CFAEdgeType.BlankEdge
          && oldCfaEdge.getPredecessor().getNumEnteringEdges() == 1) {
        oldCfaEdge = oldCfaEdge.getPredecessor().getEnteringEdge(0);
      }

      if (oldCfaEdge.equals(pTransformableLoop.getInitLoopIndexCfaEdge())) {
        initEdge = edge;
      } else if (oldCfaEdge.equals(pTransformableLoop.getUpdateLoopIndexCfaEdge())) {
        updateEdge = edge;
      } else if (oldCfaEdge.equals(pTransformableLoop.getEnterLoopCfaEdge())) {
        continueEdge = edge;
      } else if (oldCfaEdge.equals(pTransformableLoop.getExitLoopCfaEdge())) {
        breakEdge = edge;
      }
    }

    assert initEdge != null;
    assert updateEdge != null;
    assert continueEdge != null;
    assert breakEdge != null;

    CfaTransformer.Node outerBeforeLoop = initEdge.getPredecessor().orElseThrow();
    CfaTransformer.Node outerAfterLoop = breakEdge.getSuccessor().orElseThrow();
    CfaTransformer.Node loopBodyFirst = continueEdge.getSuccessor().orElseThrow();
    CfaTransformer.Node loopBodyLast = updateEdge.getPredecessor().orElseThrow();

    initEdge.detachAll();
    updateEdge.detachAll();
    continueEdge.detachAll();
    breakEdge.detachAll();

    CfaTransformer.Edge enterUnrolledLoopEdge =
        CfaTransformer.Edge.forOriginal(
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                CFANode.newDummyCFANode("dummy-predecessor"),
                CFANode.newDummyCFANode("dummy-successor"),
                "enter-loop-body"));
    outerBeforeLoop.attachLeaving(enterUnrolledLoopEdge);
    loopBodyFirst.attachEntering(enterUnrolledLoopEdge);

    CfaTransformer.Edge exitUnrolledLoopEdge =
        CfaTransformer.Edge.forOriginal(
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                CFANode.newDummyCFANode("dummy-predecessor"),
                CFANode.newDummyCFANode("dummy-successor"),
                "exit-loop-body"));
    loopBodyLast.attachLeaving(exitUnrolledLoopEdge);
    outerAfterLoop.attachEntering(exitUnrolledLoopEdge);

    for (CExpression conditionExpression :
        getConditionExpressions(pTransformableArrays, pTransformableLoop)) {

      outerBeforeLoop.insertSuccessor(
          CfaTransformer.Edge.forOriginal(
              new CAssumeEdge(
                  "",
                  FileLocation.DUMMY,
                  CFANode.newDummyCFANode("dummy-predecessor"),
                  CFANode.newDummyCFANode("dummy-successor"),
                  conditionExpression,
                  true)),
          CfaTransformer.Node.forOriginal(outerBeforeLoop.getOriginalCfaNode()));

      CfaTransformer.Edge skipLoopBody =
          CfaTransformer.Edge.forOriginal(
              new CAssumeEdge(
                  "",
                  FileLocation.DUMMY,
                  CFANode.newDummyCFANode("dummy-predecessor"),
                  CFANode.newDummyCFANode("dummy-successor"),
                  conditionExpression,
                  false));
      outerBeforeLoop.attachLeaving(skipLoopBody);
      outerAfterLoop.attachEntering(skipLoopBody);
    }
  }

  private static ImmutableSet<CIdExpression> getLoopDefs(TransformableLoop pTransformableLoop) {

    ImmutableSet.Builder<CIdExpression> loopDefsBuilder = ImmutableSet.builder();

    for (CFAEdge edge : pTransformableLoop.getLoopEdges()) {
      if (edge instanceof CStatementEdge
          && !edge.equals(pTransformableLoop.getUpdateLoopIndexCfaEdge())) {
        CStatement statement = ((CStatementEdge) edge).getStatement();
        if (statement instanceof CExpressionAssignmentStatement) {
          CLeftHandSide lhs = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
          if (lhs instanceof CIdExpression) {
            loopDefsBuilder.add((CIdExpression) lhs);
          }
        }
      }
    }

    return loopDefsBuilder.build();
  }

  public static CFA transformCfa(Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    Objects.requireNonNull(pConfiguration, "pConfiguration must not be null");
    Objects.requireNonNull(pLogger, "pLogger must not be null");
    Objects.requireNonNull(pCfa, "pCfa must not be null");

    ImmutableSet<TransformableArray> transformableArrays =
        TransformableArray.getTransformableArrays(pCfa);
    Map<MemoryLocation, TransformableArray> arrayMemoryLocationToTransformableArray =
        new HashMap<>();
    for (TransformableArray transformableArray : transformableArrays) {
      arrayMemoryLocationToTransformableArray.put(
          transformableArray.getMemoryLocation(), transformableArray);
    }

    ImmutableSet<TransformableLoop> transformableLoops =
        TransformableLoop.getTransformableLoops(pCfa);
    CCfaTransformer cfaTransformer =
        CCfaTransformer.createTransformer(pConfiguration, pLogger, pCfa);
    VariableGenerator variableGenerator = new VariableGenerator("__nondet_variable_");
    ArrayOperationReplacementMap arrayOperationReplacementMap = new ArrayOperationReplacementMap();

    for (TransformableLoop transformableLoop : transformableLoops) {
      if (!transformableLoop.areLoopIterationsIndependent()) {
        return pCfa;
      }
    }

    for (TransformableLoop transformableLoop : transformableLoops) {
      ImmutableSet<CIdExpression> loopDefs = getLoopDefs(transformableLoop);

      String functionName = transformableLoop.getLoopCfaNode().getFunctionName();

      CfaTransformer.Node firstLoopBodyNode =
          cfaTransformer.get(transformableLoop.getEnterLoopCfaEdge().getSuccessor()).orElseThrow();
      CfaTransformer.Node lastLoopBodyNode =
          cfaTransformer
              .get(transformableLoop.getUpdateLoopIndexCfaEdge().getPredecessor())
              .orElseThrow();

      for (CIdExpression loopDef : loopDefs) {

        Optional<TransformableArray> someTransformableArray =
            transformableArrays.stream().findAny();
        if (someTransformableArray.isPresent()) {
          TransformableArray transformableArray = someTransformableArray.orElseThrow();
          CIdExpression loopIndexIdExpression = transformableLoop.getLoopIndexExpression();
          CIdExpression arrayIndexWitnessIdExpression =
              createIndexCIdExpression(transformableArray);
          CFAEdge indexAssignCfaEdge =
              ArrayAbstractionUtils.createAssignEdge(
                  loopIndexIdExpression, arrayIndexWitnessIdExpression);
          CfaTransformer.Edge indexAssignEdge = CfaTransformer.Edge.forOriginal(indexAssignCfaEdge);
          firstLoopBodyNode.insertSuccessor(
              indexAssignEdge,
              CfaTransformer.Node.forOriginal(firstLoopBodyNode.getOriginalCfaNode()));
        }

        CType type = loopDef.getExpressionType();
        String nondetVariableName = variableGenerator.createNewVariableName();
        CFAEdge nondetVariableCfaEdge =
            VariableGenerator.createNondetVariableEdge(
                type, nondetVariableName, Optional.of(functionName));
        CIdExpression nondetVariableIdExpression =
            ArrayAbstractionUtils.createCIdExpression(
                type, MemoryLocation.forLocalVariable(functionName, nondetVariableName));
        CFAEdge assignNondetVariableCfaEdge =
            ArrayAbstractionUtils.createAssignEdge(loopDef, nondetVariableIdExpression);

        CfaTransformer.Edge assignNondetVariableEdgeStart =
            CfaTransformer.Edge.forOriginal(assignNondetVariableCfaEdge);
        firstLoopBodyNode.insertSuccessor(
            assignNondetVariableEdgeStart,
            CfaTransformer.Node.forOriginal(firstLoopBodyNode.getOriginalCfaNode()));
        CfaTransformer.Edge nondetVariableEdgeStart =
            CfaTransformer.Edge.forOriginal(nondetVariableCfaEdge);
        firstLoopBodyNode.insertSuccessor(
            nondetVariableEdgeStart,
            CfaTransformer.Node.forOriginal(firstLoopBodyNode.getOriginalCfaNode()));

        CfaTransformer.Edge assignNondetVariableEdgeEnd =
            CfaTransformer.Edge.forOriginal(assignNondetVariableCfaEdge);
        lastLoopBodyNode.insertSuccessor(
            assignNondetVariableEdgeEnd,
            CfaTransformer.Node.forOriginal(lastLoopBodyNode.getOriginalCfaNode()));
        CfaTransformer.Edge nondetVariableEdgeEnd =
            CfaTransformer.Edge.forOriginal(nondetVariableCfaEdge);
        lastLoopBodyNode.insertSuccessor(
            nondetVariableEdgeEnd,
            CfaTransformer.Node.forOriginal(lastLoopBodyNode.getOriginalCfaNode()));
      }

      for (CFAEdge edge : transformableLoop.getLoopEdges()) {
        ImmutableSet<TransformableArray.ArrayOperation> arrayOperations =
            TransformableArray.getArrayOperations(edge);
        for (TransformableArray.ArrayOperation arrayOperation : arrayOperations) {

          TransformableArray transformableArray =
              arrayMemoryLocationToTransformableArray.get(arrayOperation.getArrayMemoryLocation());
          assert transformableArray != null
              : "Missing TransformableArray for ArrayOperation: " + arrayOperation;

          CExpression arrayIndexExpression = arrayOperation.getIndexExpression();
          MemoryLocation replacement = null;

          if (arrayIndexExpression instanceof CIdExpression) {

            CIdExpression loopIndexIdExpression = transformableLoop.getLoopIndexExpression();
            CIdExpression arrayIndexIdExpression = (CIdExpression) arrayIndexExpression;

            MemoryLocation loopIndexMemoryLocation =
                ArrayAbstractionUtils.getMemoryLocation(loopIndexIdExpression);
            MemoryLocation arrayIndexMemoryLocation =
                ArrayAbstractionUtils.getMemoryLocation(arrayIndexIdExpression);

            if (loopIndexMemoryLocation.equals(arrayIndexMemoryLocation)) {
              CIdExpression arrayVariableWitnessIdExpression =
                  createVariableCIdExpression(transformableArray);
              replacement =
                  ArrayAbstractionUtils.getMemoryLocation(arrayVariableWitnessIdExpression);
            }
          }

          if (replacement == null) {
            String newVariableName = variableGenerator.createNewVariableName();
            replacement = MemoryLocation.forIdentifier(newVariableName);
          }

          arrayOperationReplacementMap.insertReplacement(edge, arrayOperation, replacement);
        }
      }

      replaceLoopWithBranching(cfaTransformer, transformableArrays, transformableLoop);
    }

    CCfaEdgeTransformer edgeTransformer =
        CCfaEdgeTransformer.forAstTransformer(
            (originalCfaEdge, originalAstNode) ->
                arrayOperationReplacementMap
                    .getAstTransformer(originalCfaEdge)
                    .transform(originalAstNode));

    return cfaTransformer.createCfa(CCfaNodeTransformer.DEFAULT, edgeTransformer);
  }

  private static final class ArrayOperationReplacementMap {

    private final Map<CFAEdge, Map<TransformableArray.ArrayOperation, MemoryLocation>>
        replacementsPerEdge;

    private ArrayOperationReplacementMap() {
      replacementsPerEdge = new HashMap<>();
    }

    private void insertReplacement(
        CFAEdge pEdge,
        TransformableArray.ArrayOperation pArrayOperation,
        MemoryLocation pReplacementVariableMemoryLocation) {

      Map<TransformableArray.ArrayOperation, MemoryLocation> replacements =
          replacementsPerEdge.computeIfAbsent(pEdge, key -> new HashMap<>());
      replacements.put(pArrayOperation, pReplacementVariableMemoryLocation);
    }

    private TransformingCAstNodeVisitor<NoException> getAstTransformer(CFAEdge pEdge) {

      Map<TransformableArray.ArrayOperation, MemoryLocation> replacements =
          replacementsPerEdge.computeIfAbsent(pEdge, key -> ImmutableMap.of());
      return new AstTransformingVisitor(replacements);
    }
  }

  private static final class ReplaceLoopIndexAstTransformingVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    private final TransformableArray transformableArray;
    private final TransformableLoop transformableLoop;

    private ReplaceLoopIndexAstTransformingVisitor(
        TransformableArray pTransformableArray, TransformableLoop pTransformableLoop) {

      transformableArray = pTransformableArray;
      transformableLoop = pTransformableLoop;
    }

    @Override
    public CAstNode visit(CIdExpression pCIdExpression) {

      if (ArrayAbstractionUtils.getMemoryLocation(pCIdExpression)
          .equals(transformableLoop.getLoopIndexMemoryLocation())) {
        return createIndexCIdExpression(transformableArray);
      }

      return super.visit(pCIdExpression);
    }
  }

  private static final class AstTransformingVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    private final Map<TransformableArray.ArrayOperation, MemoryLocation> arrayOperationReplacements;

    private AstTransformingVisitor(
        Map<TransformableArray.ArrayOperation, MemoryLocation> pArrayOperationToNondetVariable) {
      arrayOperationReplacements = pArrayOperationToNondetVariable;
    }

    @Override
    public CAstNode visit(CArraySubscriptExpression pCArraySubscriptExpression) {

      if (!arrayOperationReplacements.isEmpty()) {

        ImmutableSet<TransformableArray.ArrayOperation> arrayOperations =
            TransformableArray.getArrayOperations(pCArraySubscriptExpression);

        if (arrayOperations.size() == 1) {

          TransformableArray.ArrayOperation arrayOperation =
              arrayOperations.stream().findAny().orElseThrow();
          MemoryLocation nondetVariableMemoryLocation =
              arrayOperationReplacements.get(arrayOperation);

          if (nondetVariableMemoryLocation == null) {
            // an isolated CArraySubscriptExpression is always seen as a read, so the array
            // operation is transformed into a write if the map doesn't contain the read operation
            TransformableArray.ArrayOperation writeArrayOperation =
                arrayOperation.toWriteOperation();
            nondetVariableMemoryLocation = arrayOperationReplacements.get(writeArrayOperation);
          }

          if (nondetVariableMemoryLocation != null) {
            CType type = pCArraySubscriptExpression.getExpressionType();
            return ArrayAbstractionUtils.createCIdExpression(type, nondetVariableMemoryLocation);
          }
        }
      }

      return super.visit(pCArraySubscriptExpression);
    }

    @Override
    public CAstNode visit(CVariableDeclaration pCVariableDeclaration) {

      if (pCVariableDeclaration.getType() instanceof CArrayType) {
        return ArrayAbstractionUtils.createNonArrayVariableDeclaration(pCVariableDeclaration);
      }

      return super.visit(pCVariableDeclaration);
    }
  }
}
