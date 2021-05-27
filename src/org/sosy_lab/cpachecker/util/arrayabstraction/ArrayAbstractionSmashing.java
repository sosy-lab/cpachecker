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
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.CAstNodeTransformer;
import org.sosy_lab.cpachecker.util.CCfaTransformer;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CfaTransformer;
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
    MemoryLocation nondetVariable = MemoryLocation.valueOf(nondetVariableName);
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

    CfaTransformer.Node predecessorNode =
        pCfaTransformer.getNode(pEdge.getPredecessor()).orElseThrow();
    CfaTransformer.Node successorNode = pCfaTransformer.getNode(pEdge.getSuccessor()).orElseThrow();
    // --- a ---> [predecessorNode]
    // --- <array-write> ---> [successorNode]
    // --- b --->

    CfaTransformer.Edge nondetVariableEdge = CfaTransformer.Edge.createFrom(nondetVariableCfaEdge);
    predecessorNode.splitAndInsertEntering(
        nondetVariableEdge, CfaTransformer.Node.createFrom(predecessorNode.getOldCfaNode()));
    // --- a ---> [newNodeFst]
    // --- <new-nondet-var> ---> [predecessorNode]
    // --- <array-write> ---> [successorNode]
    // --- b --->

    CfaTransformer.Node newNodeSnd =
        CfaTransformer.Node.createFrom(predecessorNode.getOldCfaNode());
    CfaTransformer.Edge conditionEdgeTrue = CfaTransformer.Edge.createFrom(conditionCfaEdgeTrue);
    predecessorNode.splitAndInsertEntering(conditionEdgeTrue, newNodeSnd);
    // --- a ---> [newNodeFst]
    // --- <new-nondet-var> ---> [newNodeSnd]
    // --- <nondet-var-condition> ---> [predecessorNode]
    // --- <array-write> ---> [successorNode]
    // --- b --->

    CfaTransformer.Edge conditionEdgeFalse = CfaTransformer.Edge.createFrom(conditionCfaEdgeFalse);
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

    for (CFANode node : pCfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        ImmutableSet<TransformableArray.ArrayOperation> arrayOperations =
            TransformableArray.getArrayOperations(edge);
        for (TransformableArray.ArrayOperation arrayOperation : arrayOperations) {
          if (arrayOperation.getType() == TransformableArray.ArrayOperationType.WRITE) {
            transformArrayWriteEdge(cfaTransformer, variableGenerator, edge);
          }
        }
      }
    }

    AstTransformer astTransformer = new AstTransformer();
    SimpleNodeTransformer nodeTransformer = new SimpleNodeTransformer();
    SimpleEdgeTransformer<DummyException> edgeTransformer =
        new SimpleEdgeTransformer<>(edge -> astTransformer);

    return cfaTransformer.createCfa(nodeTransformer, edgeTransformer);
  }

  /** Dummy exception that is never thrown. */
  private static final class DummyException extends RuntimeException {
    private static final long serialVersionUID = -2116660848954687565L;
  }

  private static final class AstTransformer extends CAstNodeTransformer<DummyException> {

    @Override
    public CExpression visit(CArraySubscriptExpression pCArraySubscriptExpression) {

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
    public CVariableDeclaration visit(CVariableDeclaration pCVariableDeclaration) {

      if (pCVariableDeclaration.getType() instanceof CArrayType) {
        return ArrayAbstractionUtils.createNonArrayVariableDeclaration(pCVariableDeclaration);
      }

      return super.visit(pCVariableDeclaration);
    }
  }
}
