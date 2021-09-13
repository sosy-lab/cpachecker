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
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.MutableGraph;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ArrayAbstraction {

  private ArrayAbstraction() {}

  private static CIdExpression createCIdExpression(CType pType, MemoryLocation pMemoryLocation) {

    String variableName = pMemoryLocation.getIdentifier();
    String qualifiedName = pMemoryLocation.getExtendedQualifiedName();

    CVariableDeclaration variableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            pType,
            variableName,
            variableName,
            qualifiedName,
            null);

    return new CIdExpression(FileLocation.DUMMY, variableDeclaration);
  }

  private static CIdExpression createVariableCIdExpression(TransformableArray pTransformableArray) {

    CType type = pTransformableArray.getArrayType().getType();
    String arrayName = pTransformableArray.getMemoryLocation().getIdentifier();
    String name = "__array_witness_variable_" + arrayName;

    return createCIdExpression(type, MemoryLocation.forIdentifier(name));
  }

  private static CIdExpression createIndexCIdExpression(TransformableArray pTransformableArray) {

    CType type =
        new CSimpleType(
            true, false, CBasicType.UNSPECIFIED, false, false, false, true, false, false, true);
    String arrayName = pTransformableArray.getMemoryLocation().getIdentifier();
    String name = "__array_witness_index_" + arrayName;

    return createCIdExpression(type, MemoryLocation.forIdentifier(name));
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

  private static CFANode createDummyPredecessor() {
    return CFANode.newDummyCFANode("dummy-predecessor");
  }

  private static CFANode createDummySuccessor() {
    return CFANode.newDummyCFANode("dummy-successor");
  }

  private static BlankEdge createBlankEdge(String pDescription) {
    return new BlankEdge(
        "", FileLocation.DUMMY, createDummyPredecessor(), createDummySuccessor(), pDescription);
  }

  private static CAssumeEdge createAssumeEdge(CExpression pCondition, boolean pTruthAssumption) {
    return new CAssumeEdge(
        "",
        FileLocation.DUMMY,
        createDummyPredecessor(),
        createDummySuccessor(),
        pCondition,
        pTruthAssumption);
  }

  private static CStatementEdge createAssignEdge(CLeftHandSide pLhs, CIdExpression pRhs) {

    CExpressionAssignmentStatement assignmentStatement =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, pLhs, pRhs);

    return new CStatementEdge(
        "",
        assignmentStatement,
        FileLocation.DUMMY,
        createDummyPredecessor(),
        createDummySuccessor());
  }

  private static void replaceLoopWithBranching(
      MutableGraph<CFANode, CFAEdge> pMutableGraph,
      ImmutableSet<TransformableArray> pTransformableArrays,
      TransformableLoop pTransformableLoop) {

    MutableGraph.Node<CFANode, CFAEdge> loopNode =
        pMutableGraph.getNode(pTransformableLoop.getLoopCfaNode()).orElseThrow();

    MutableGraph.Edge<CFANode, CFAEdge> initEdge = null;
    MutableGraph.Edge<CFANode, CFAEdge> updateEdge = null;
    MutableGraph.Edge<CFANode, CFAEdge> continueEdge = null;
    MutableGraph.Edge<CFANode, CFAEdge> breakEdge = null;

    for (MutableGraph.Edge<CFANode, CFAEdge> edge :
        Iterables.concat(
            pMutableGraph.iterateEntering(loopNode), pMutableGraph.iterateLeaving(loopNode))) {
      CFAEdge oldCfaEdge = edge.getWrappedEdge();

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

    assert initEdge != null && breakEdge != null && continueEdge != null && updateEdge != null;

    MutableGraph.Node<CFANode, CFAEdge> outerBeforeLoop = initEdge.getPredecessorOrElseThrow();
    MutableGraph.Node<CFANode, CFAEdge> outerAfterLoop = breakEdge.getSuccessorOrElseThrow();
    MutableGraph.Node<CFANode, CFAEdge> loopBodyFirst = continueEdge.getSuccessorOrElseThrow();
    MutableGraph.Node<CFANode, CFAEdge> loopBodyLast = updateEdge.getPredecessorOrElseThrow();

    pMutableGraph.detachBoth(initEdge);
    pMutableGraph.detachBoth(updateEdge);
    pMutableGraph.detachBoth(continueEdge);
    pMutableGraph.detachBoth(breakEdge);

    MutableGraph.Edge<CFANode, CFAEdge> enterUnrolledLoopEdge =
        pMutableGraph.wrapEdge(createBlankEdge("enter-loop-body"));
    pMutableGraph.attachLeaving(outerBeforeLoop, enterUnrolledLoopEdge);
    pMutableGraph.attachEntering(loopBodyFirst, enterUnrolledLoopEdge);

    MutableGraph.Edge<CFANode, CFAEdge> exitUnrolledLoopEdge =
        pMutableGraph.wrapEdge(createBlankEdge("exit-loop-body"));
    pMutableGraph.attachLeaving(loopBodyLast, exitUnrolledLoopEdge);
    pMutableGraph.attachEntering(outerAfterLoop, exitUnrolledLoopEdge);

    for (CExpression conditionExpression :
        getConditionExpressions(pTransformableArrays, pTransformableLoop)) {

      pMutableGraph.insertSuccessor(
          outerBeforeLoop,
          pMutableGraph.wrapEdge(createAssumeEdge(conditionExpression, true)),
          pMutableGraph.wrapNode(outerBeforeLoop.getWrappedNode()));

      MutableGraph.Edge<CFANode, CFAEdge> skipLoopBody =
          pMutableGraph.wrapEdge(createAssumeEdge(conditionExpression, false));

      pMutableGraph.attachLeaving(outerBeforeLoop, skipLoopBody);
      pMutableGraph.attachEntering(outerAfterLoop, skipLoopBody);
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
    MutableGraph<CFANode, CFAEdge> mutableGraph = CCfaTransformer.createMutableGraph(pCfa);
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

      MutableGraph.Node<CFANode, CFAEdge> firstLoopBodyNode =
          mutableGraph
              .getNode(transformableLoop.getEnterLoopCfaEdge().getSuccessor())
              .orElseThrow();
      MutableGraph.Node<CFANode, CFAEdge> lastLoopBodyNode =
          mutableGraph
              .getNode(transformableLoop.getUpdateLoopIndexCfaEdge().getPredecessor())
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
              createAssignEdge(loopIndexIdExpression, arrayIndexWitnessIdExpression);
          MutableGraph.Edge<CFANode, CFAEdge> indexAssignEdge =
              mutableGraph.wrapEdge(indexAssignCfaEdge);

          mutableGraph.insertSuccessor(
              firstLoopBodyNode,
              indexAssignEdge,
              mutableGraph.wrapNode(firstLoopBodyNode.getWrappedNode()));
        }

        CType type = loopDef.getExpressionType();
        String nondetVariableName = variableGenerator.createNewVariableName();
        CFAEdge nondetVariableCfaEdge =
            VariableGenerator.createNondetVariableEdge(
                type, nondetVariableName, Optional.of(functionName));
        CIdExpression nondetVariableIdExpression =
            createCIdExpression(
                type, MemoryLocation.forLocalVariable(functionName, nondetVariableName));
        CFAEdge assignNondetVariableCfaEdge = createAssignEdge(loopDef, nondetVariableIdExpression);

        MutableGraph.Edge<CFANode, CFAEdge> assignNondetVariableEdgeStart =
            mutableGraph.wrapEdge(assignNondetVariableCfaEdge);
        mutableGraph.insertSuccessor(
            firstLoopBodyNode,
            assignNondetVariableEdgeStart,
            mutableGraph.wrapNode(firstLoopBodyNode.getWrappedNode()));
        MutableGraph.Edge<CFANode, CFAEdge> nondetVariableEdgeStart =
            mutableGraph.wrapEdge(nondetVariableCfaEdge);
        mutableGraph.insertSuccessor(
            firstLoopBodyNode,
            nondetVariableEdgeStart,
            mutableGraph.wrapNode(firstLoopBodyNode.getWrappedNode()));

        MutableGraph.Edge<CFANode, CFAEdge> assignNondetVariableEdgeEnd =
            mutableGraph.wrapEdge(assignNondetVariableCfaEdge);
        mutableGraph.insertSuccessor(
            lastLoopBodyNode,
            assignNondetVariableEdgeEnd,
            mutableGraph.wrapNode(lastLoopBodyNode.getWrappedNode()));
        MutableGraph.Edge<CFANode, CFAEdge> nondetVariableEdgeEnd =
            mutableGraph.wrapEdge(nondetVariableCfaEdge);
        mutableGraph.insertSuccessor(
            lastLoopBodyNode,
            nondetVariableEdgeEnd,
            mutableGraph.wrapNode(lastLoopBodyNode.getWrappedNode()));
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
                MemoryLocation.forDeclaration(loopIndexIdExpression.getDeclaration());
            MemoryLocation arrayIndexMemoryLocation =
                MemoryLocation.forDeclaration(arrayIndexIdExpression.getDeclaration());

            if (loopIndexMemoryLocation.equals(arrayIndexMemoryLocation)) {
              CIdExpression arrayVariableWitnessIdExpression =
                  createVariableCIdExpression(transformableArray);
              replacement =
                  MemoryLocation.forDeclaration(arrayVariableWitnessIdExpression.getDeclaration());
            }
          }

          if (replacement == null) {
            String newVariableName = variableGenerator.createNewVariableName();
            replacement = MemoryLocation.forIdentifier(newVariableName);
          }

          arrayOperationReplacementMap.insertReplacement(edge, arrayOperation, replacement);
        }
      }

      replaceLoopWithBranching(mutableGraph, transformableArrays, transformableLoop);
    }

    CCfaEdgeTransformer edgeTransformer =
        CCfaEdgeTransformer.forAstTransformer(
            (originalCfaEdge, originalAstNode) ->
                arrayOperationReplacementMap
                    .getAstTransformer(originalCfaEdge)
                    .transform(originalAstNode));

    return CCfaTransformer.createCfa(
        pConfiguration, pLogger, pCfa, mutableGraph, CCfaNodeTransformer.DEFAULT, edgeTransformer);
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

      if (MemoryLocation.forDeclaration(pCIdExpression.getDeclaration())
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
            return createCIdExpression(type, nondetVariableMemoryLocation);
          }
        }
      }

      return super.visit(pCArraySubscriptExpression);
    }

    @Override
    public CAstNode visit(CVariableDeclaration pCVariableDeclaration) {

      CType type = pCVariableDeclaration.getType();

      if (type instanceof CArrayType) {
        CType newType = ((CArrayType) type).getType();
        return new CVariableDeclaration(
            pCVariableDeclaration.getFileLocation(),
            pCVariableDeclaration.isGlobal(),
            pCVariableDeclaration.getCStorageClass(),
            newType,
            pCVariableDeclaration.getName(),
            pCVariableDeclaration.getOrigName(),
            pCVariableDeclaration.getQualifiedName(),
            null);
      }

      return super.visit(pCVariableDeclaration);
    }
  }
}
