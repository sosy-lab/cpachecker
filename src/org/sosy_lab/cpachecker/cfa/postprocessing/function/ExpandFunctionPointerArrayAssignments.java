// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class goes through the CFA and replace all assignments of the form <code>
 * array[i] = ...
 * </code> by <code>
 * if (i == 0) array[0] = ...
 * else if (i == 1) array[1] = ...
 * ...
 * </code> if
 *
 * <ul>
 *   <li>{@code array} is an array of function pointers
 *   <li>{code array} has a statically known length
 *   <li>{code i} is a simple variable ({@link CIdExpression})
 * </ul>
 */
public class ExpandFunctionPointerArrayAssignments {

  private final LogManager logger;

  public ExpandFunctionPointerArrayAssignments(LogManager pLogger) {
    logger = pLogger;
  }

  public void replaceFunctionPointerArrayAssignments(final MutableCFA cfa) {

    CBinaryExpressionBuilder binBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);

    for (final String function : cfa.getAllFunctionNames()) {

      for (CFANode node : ImmutableList.copyOf(cfa.getFunctionNodes(function))) {
        switch (node.getNumLeavingEdges()) {
        case 0:
          break;
        case 1:
          handleEdge(node.getLeavingEdge(0), cfa, binBuilder);
          break;
        case 2:
          break;
        default:
          throw new AssertionError("Too many leaving edges on CFANode");
        }
      }
    }
  }

  private static void handleEdge(CFAEdge edge, MutableCFA cfa, CBinaryExpressionBuilder builder) {
    if (!(edge instanceof CStatementEdge)) {
      return;
    }

    CStatement stmt = ((CStatementEdge)edge).getStatement();
    if (!(stmt instanceof CExpressionAssignmentStatement)) {
      return;
    }

    CLeftHandSide lhs = ((CExpressionAssignmentStatement)stmt).getLeftHandSide();
    CExpression rhs = ((CExpressionAssignmentStatement)stmt).getRightHandSide();
    if (!isFunctionPointerType(lhs.getExpressionType())) {
      return;
    }

    if (!(lhs instanceof CArraySubscriptExpression)) {
      return;
    }
    CArraySubscriptExpression array = ((CArraySubscriptExpression)lhs);
    if (!(array.getSubscriptExpression() instanceof CIdExpression)) {
      return;
    }
    final CExpression subscript = array.getSubscriptExpression();

    CType arrayType = array.getArrayExpression().getExpressionType().getCanonicalType();
    if (!(arrayType instanceof CArrayType)
        || !(((CArrayType)arrayType).getLength() instanceof CIntegerLiteralExpression)) {
      return;
    }
    final long length = ((CIntegerLiteralExpression)((CArrayType)arrayType).getLength()).asLong();

    final CFANode startNode = edge.getPredecessor();
    final CFANode endNode = edge.getSuccessor();

    CFACreationUtils.removeEdgeFromNodes(edge);

    CFANode predecessor = startNode;
    for (long i = 0; i < length; i++) {

      CFANode trueNode = new CFANode(startNode.getFunction());
      CFANode falseNode = new CFANode(startNode.getFunction());
      cfa.addNode(trueNode);
      cfa.addNode(falseNode);

      CExpression index = new CIntegerLiteralExpression(subscript.getFileLocation(),
                                                        CNumericTypes.INT,
                                                        BigInteger.valueOf(i));
      CExpression assumeExp = builder.buildBinaryExpressionUnchecked(
          subscript, index, BinaryOperator.EQUALS);
      CAssumeEdge trueEdge = new CAssumeEdge(edge.getRawStatement(),
                                             edge.getFileLocation(),
                                             predecessor,
                                             trueNode, assumeExp, true);

      CAssumeEdge falseEdge = new CAssumeEdge(edge.getRawStatement(),
                                              edge.getFileLocation(),
                                              predecessor,
                                              falseNode, assumeExp, false);

      CFACreationUtils.addEdgeUnconditionallyToCFA(trueEdge);
      CFACreationUtils.addEdgeUnconditionallyToCFA(falseEdge);

      CLeftHandSide arrayAccess = new CArraySubscriptExpression(array.getFileLocation(),
                                                                array.getExpressionType(),
                                                                array.getArrayExpression(),
                                                                index);
      CStatement assignment = new CExpressionAssignmentStatement(stmt.getFileLocation(),
                                                                 arrayAccess,
                                                                 rhs);
      CStatementEdge assignmentEdge = new CStatementEdge(edge.getRawStatement(),
                                                         assignment,
                                                         edge.getFileLocation(),
                                                         trueNode, endNode);
      CFACreationUtils.addEdgeUnconditionallyToCFA(assignmentEdge);
      predecessor = falseNode;
    }

    // TODO The following code creates the last "else" branch.
    // Usually we would want to put the original edge there for all cases
    // of invalid indices, but this makes FunctionPointerCPA too imprecise
    // (it merges the state from the else branch with the states from the other branches,
    // loosing all information it gained in the latter).
/*
    CStatementEdge elseEdge = new CStatementEdge(edge.getRawStatement(),
                                                 stmt,
                                                 edge.getLineNumber(),
                                                 predecessor, endNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(elseEdge);
*/
  }

  private static boolean isFunctionPointerType(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      type = ((CPointerType)type).getType();
    }
    return type instanceof CFunctionType;
  }
}