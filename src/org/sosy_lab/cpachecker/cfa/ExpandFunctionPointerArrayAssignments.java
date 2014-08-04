/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa;

import java.math.BigInteger;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CParserException;

import com.google.common.collect.ImmutableList;

/*
 * This class goes through the CFA and replace all assignments of the form
 * <code>
 * array[i] = ...
 * </code>
 * by
 * <code>
 * if (i == 0) array[0] = ...
 * else if (i == 1) array[1] = ...
 * ...
 * </code>
 * if
 * - "array" is an array of function pointers
 * - "array" has a statically known length
 * - "i" is a simple variable (CIdExpression)
 */
class ExpandFunctionPointerArrayAssignments {

  private final LogManager logger;

  ExpandFunctionPointerArrayAssignments(LogManager pLogger, Configuration config) throws InvalidConfigurationException {
    logger = pLogger;
  }

  public void replaceFunctionPointerArrayAssignments(final MutableCFA cfa) throws CParserException {

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
          throw new CFAGenerationRuntimeException("Too much leaving Edges on CFANode");
        }
      }
    }
  }

  private static void handleEdge(CFAEdge edge, MutableCFA cfa, CBinaryExpressionBuilder builder) throws CParserException {
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

      CFANode trueNode = new CFANode(startNode.getFunctionName());
      CFANode falseNode = new CFANode(startNode.getFunctionName());
      cfa.addNode(trueNode);
      cfa.addNode(falseNode);

      CExpression index = new CIntegerLiteralExpression(subscript.getFileLocation(),
                                                        CNumericTypes.INT,
                                                        BigInteger.valueOf(i));
      CExpression assumeExp = builder.buildBinaryExpression(subscript, index, BinaryOperator.EQUALS);
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