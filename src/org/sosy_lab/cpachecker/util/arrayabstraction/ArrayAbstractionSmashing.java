// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.TransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaNodeTransformer;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Utility class for array abstraction by array smashing.
 *
 * <p>A smashed array is collapsed into a single element. All array writes become weak updates of
 * this element.
 */
public class ArrayAbstractionSmashing {

  private ArrayAbstractionSmashing() {}

  private static CType getIntType() {
    return new CSimpleType(
        false, false, CBasicType.INT, false, false, true, false, false, false, false);
  }

  private static CExpression createVariableLessThanZeroCondition(CExpression pCIdExpression) {

    CType type = pCIdExpression.getExpressionType();

    CIntegerLiteralExpression zeroLiteral =
        new CIntegerLiteralExpression(FileLocation.DUMMY, type, BigInteger.ZERO);

    CBinaryExpression.BinaryOperator binaryOperator = CBinaryExpression.BinaryOperator.LESS_THAN;
    CBinaryExpression conditionExpression =
        new CBinaryExpression(
            FileLocation.DUMMY, type, type, pCIdExpression, zeroLiteral, binaryOperator);

    return conditionExpression;
  }

  private static void transformArrayWriteEdge(
      CfaTransformer pCfaTransformer, VariableGenerator pVariableGenerator, CFAEdge pEdge) {

    String nondetVariableName = pVariableGenerator.createNewVariableName();
    MemoryLocation nondetVariable = MemoryLocation.forIdentifier(nondetVariableName);
    CType intType = getIntType();

    Optional<String> functionName = Optional.of(pEdge.getPredecessor().getFunctionName());
    CFAEdge nondetVariableCfaEdge =
        VariableGenerator.createNondetVariableEdge(
            intType, nondetVariable.getIdentifier(), functionName);
    CExpression nondetVariableExpression =
        ArrayAbstractionUtils.createCIdExpression(intType, nondetVariable);
    CExpression nondetVariableLessThanZeroExpression =
        createVariableLessThanZeroCondition(nondetVariableExpression);

    CFAEdge conditionCfaEdgeTrue =
        ArrayAbstractionUtils.createAssumeEdge(nondetVariableLessThanZeroExpression, true);
    CFAEdge conditionCfaEdgeFalse =
        ArrayAbstractionUtils.createAssumeEdge(nondetVariableLessThanZeroExpression, false);

    CfaTransformer.Node predecessorNode = pCfaTransformer.get(pEdge.getPredecessor()).orElseThrow();
    CfaTransformer.Node successorNode = pCfaTransformer.get(pEdge.getSuccessor()).orElseThrow();
    // --- a ---> [predecessorNode]
    // --- <array-write> ---> [successorNode]
    // --- b --->

    CfaTransformer.Edge nondetVariableEdge = CfaTransformer.Edge.forOriginal(nondetVariableCfaEdge);
    predecessorNode.insertPredecessor(
        nondetVariableEdge, CfaTransformer.Node.forOriginal(predecessorNode.getOriginalCfaNode()));
    // --- a ---> [newNodeFst]
    // --- <new-nondet-var> ---> [predecessorNode]
    // --- <array-write> ---> [successorNode]
    // --- b --->

    CfaTransformer.Node newNodeSnd =
        CfaTransformer.Node.forOriginal(predecessorNode.getOriginalCfaNode());
    CfaTransformer.Edge conditionEdgeTrue = CfaTransformer.Edge.forOriginal(conditionCfaEdgeTrue);
    predecessorNode.insertPredecessor(conditionEdgeTrue, newNodeSnd);
    // --- a ---> [newNodeFst]
    // --- <new-nondet-var> ---> [newNodeSnd]
    // --- <nondet-var-condition> ---> [predecessorNode]
    // --- <array-write> ---> [successorNode]
    // --- b --->

    CfaTransformer.Edge conditionEdgeFalse = CfaTransformer.Edge.forOriginal(conditionCfaEdgeFalse);
    newNodeSnd.attachLeaving(conditionEdgeFalse);
    successorNode.attachEntering(conditionEdgeFalse);
  }

  /**
   * Returns the transformed CFA with smashed arrays.
   *
   * <p>A smashed array is collapsed into a single element. All array writes become weak updates of
   * this element.
   *
   * @param pConfiguration the configuration to use
   * @param pLogger the logger to use
   * @param pCfa the original array-containing CFA to transform
   * @return the transformed CFA with smashed arrays
   * @throws NullPointerException if any of the parameters is {@code null}
   */
  public static CFA transformCfa(Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    Objects.requireNonNull(pConfiguration, "pConfiguration must not be null");
    Objects.requireNonNull(pLogger, "pLogger must not be null");
    Objects.requireNonNull(pCfa, "pCfa must not be null");

    CCfaTransformer cfaTransformer =
        CCfaTransformer.createTransformer(pConfiguration, pLogger, pCfa);
    VariableGenerator variableGenerator = new VariableGenerator("__nondet_variable_");
    Set<MemoryLocation> arrayMemoryLocations = new HashSet<>();

    for (CFANode node : pCfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        ImmutableSet<TransformableArray.ArrayOperation> arrayOperations =
            TransformableArray.getArrayOperations(edge);
        for (TransformableArray.ArrayOperation arrayOperation : arrayOperations) {
          arrayMemoryLocations.add(arrayOperation.getArrayMemoryLocation());
          if (arrayOperation.getType() == TransformableArray.ArrayOperationType.WRITE) {
            transformArrayWriteEdge(cfaTransformer, variableGenerator, edge);
          }
        }
      }
    }

    TransformingCAstNodeVisitor<NoException> astTransformer =
        new AstTransformingVisitor(ImmutableSet.copyOf(arrayMemoryLocations));
    CCfaEdgeTransformer edgeTransformer =
        CCfaEdgeTransformer.forAstTransformer(
            (originalCfaEdge, originalAstNode) -> astTransformer.transform(originalAstNode));

    return cfaTransformer.createCfa(CCfaNodeTransformer.DEFAULT, edgeTransformer);
  }

  private static final class AstTransformingVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    private final ImmutableSet<MemoryLocation> arrayMemoryLocations;

    private AstTransformingVisitor(ImmutableSet<MemoryLocation> pArrayMemoryLocations) {
      arrayMemoryLocations = pArrayMemoryLocations;
    }

    private boolean isArrayIdExpression(CExpression pExpression) {

      if (pExpression instanceof CIdExpression) {
        CIdExpression idExpression = (CIdExpression) pExpression;
        MemoryLocation memoryLocation = ArrayAbstractionUtils.getMemoryLocation(idExpression);
        if (arrayMemoryLocations.contains(memoryLocation)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public CAstNode visit(CArraySubscriptExpression pCArraySubscriptExpression) {

      ImmutableSet<TransformableArray.ArrayOperation> arrayOperations =
          TransformableArray.getArrayOperations(pCArraySubscriptExpression);

      if (arrayOperations.size() == 1) {

        TransformableArray.ArrayOperation arrayOperation =
            arrayOperations.stream().findAny().orElseThrow();
        MemoryLocation arrayMemoryLocation = arrayOperation.getArrayMemoryLocation();
        CType type = pCArraySubscriptExpression.getExpressionType();

        return ArrayAbstractionUtils.createCIdExpression(type, arrayMemoryLocation);
      }

      return super.visit(pCArraySubscriptExpression);
    }

    @Override
    public CAstNode visit(CVariableDeclaration pCVariableDeclaration) {

      CType type = pCVariableDeclaration.getType();
      if (type instanceof CArrayType) {
        return ArrayAbstractionUtils.createNonArrayVariableDeclaration(pCVariableDeclaration);
      } else if (type instanceof CPointerType) {
        MemoryLocation memoryLocation = MemoryLocation.forDeclaration(pCVariableDeclaration);
        if (arrayMemoryLocations.contains(memoryLocation)) {
          CType newType = ((CPointerType) type).getType();
          return ArrayAbstractionUtils.createVariableDeclarationWithType(
              pCVariableDeclaration, newType);
        }
      }

      return super.visit(pCVariableDeclaration);
    }

    @Override
    public CAstNode visit(CIdExpression pCIdExpression) {

      if (isArrayIdExpression(pCIdExpression)) {
        CDeclaration declaration = (CDeclaration) pCIdExpression.getDeclaration().accept(this);
        return new CIdExpression(pCIdExpression.getFileLocation(), declaration);
      } else {
        return super.visit(pCIdExpression);
      }
    }

    @Override
    public CFunctionCallAssignmentStatement visit(
        CFunctionCallAssignmentStatement pCFunctionCallAssignmentStatement) {

      CLeftHandSide lhs =
          (CLeftHandSide) pCFunctionCallAssignmentStatement.getLeftHandSide().accept(this);
      CFunctionCallExpression rhs =
          (CFunctionCallExpression)
              pCFunctionCallAssignmentStatement.getFunctionCallExpression().accept(this);

      if (isArrayIdExpression(lhs)) {
        rhs = VariableGenerator.createNondetFunctionCallExpression(lhs.getExpressionType());
      }

      return new CFunctionCallAssignmentStatement(
          pCFunctionCallAssignmentStatement.getFileLocation(), lhs, rhs);
    }
  }
}
