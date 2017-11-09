/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

public class Values {



  private Set<String> inputVariables;
  private Set<String> outputVariables;

  public Values(String inputInterface, String outputInterface) {
    inputVariables = new TreeSet<>();
    for (String variable : inputInterface.split(",")) {
      inputVariables.add(variable.trim());
    }
    outputVariables = new TreeSet<>();
    for (String variable : outputInterface.split(",")) {
      outputVariables.add(variable.trim());
    }
  }

  private BigInteger getValueFromComment(CFAEdgeWithAssumptions edge) {
    String comment = edge.getComment().replaceAll("\\s+", "");
    String[] resArray = comment.split("=");
    return new BigInteger(resArray[resArray.length - 1]);
  }

  private BigInteger getVariableValueFromFunctionCall(int index, CFAPathWithAssumptions path) {
    Set<AExpressionStatement> expStmts = new HashSet<>();
    int nesting = -1;
    for (int i = index; i < path.size(); i++) {
      CFAEdgeWithAssumptions edge = path.get(i);
      CFAEdge cfaEdge = edge.getCFAEdge();
      expStmts.addAll(edge.getExpStmts());
      if (cfaEdge instanceof CFunctionCallEdge) {
        nesting++;
      }
      if (cfaEdge instanceof CReturnStatementEdge) {
        if (nesting == 0) {
          CReturnStatementEdge returnEdge = (CReturnStatementEdge) cfaEdge;
          CReturnStatement returnStatement = returnEdge.getRawAST().get();
          CAssignment assignment = returnStatement.asAssignment().get();
          CRightHandSide rightHand = assignment.getRightHandSide();
          if (rightHand instanceof CIntegerLiteralExpression) {
            CIntegerLiteralExpression rightSide =
                (CIntegerLiteralExpression) rightHand;
            return rightSide.getValue();
          }
          if (assignment instanceof CExpressionAssignmentStatement) { return getValueFromComment(
              edge); }
        }
        nesting--;
      }
    }
    return null;
  }

  public Map<String, BigInteger> extractOutputValues(CounterexampleInfo cex) {
    Set<String> tmpOutputVariables = new LinkedHashSet<>(outputVariables);
    Map<String, BigInteger> variableToValueAssignments = new LinkedHashMap<>();
    CFAPathWithAssumptions path = cex.getCFAPathWithAssignments();
    int index = 0;
    for (CFAEdgeWithAssumptions edge : path) {
      if (edge.getCFAEdge() instanceof CFunctionCallEdge) {
        CFunctionCallEdge fEdge = (CFunctionCallEdge) edge.getCFAEdge();
        if (fEdge.getRawAST().get() instanceof CFunctionCallAssignmentStatement) {
          CFunctionCallAssignmentStatement functionCall =
              (CFunctionCallAssignmentStatement) fEdge.getRawAST().get();
          /*boolean setNewTestStep = createAndAddVariableAssignment(functionCall, index, path,
              AssignmentType.OUTPUT, outputVariables, outputAssignments, "", false, false);*/
          CLeftHandSide cLeft = functionCall.getLeftHandSide();
          CIdExpression cld = cLeft instanceof CIdExpression ? (CIdExpression) cLeft : null;

          if (cld != null && tmpOutputVariables.contains(cld.getName())) {

            tmpOutputVariables.remove(cld.getName());

            BigInteger value;

            value = getVariableValueFromFunctionCall(index, path);

            if (value != null) {
              variableToValueAssignments.put(cld.getName(), value);
            }
          }
        }
      }
      index++;
    }
    return variableToValueAssignments;
  }

  public Map<String, BigInteger> extractInputValues(CounterexampleInfo cex) {
    Map<String, BigInteger> variableToValueAssignments = new LinkedHashMap<>();
    Set<String> tempInputs = new LinkedHashSet<>(inputVariables);
    CFAPathWithAssumptions path = cex.getCFAPathWithAssignments();
    for (CFAEdgeWithAssumptions edge : path) {
      Collection<AExpressionStatement> expStmts = edge.getExpStmts();
      for (AExpressionStatement expStmt : expStmts) {
        if (expStmt.getExpression() instanceof CBinaryExpression) {
          CBinaryExpression exp = (CBinaryExpression) expStmt.getExpression();
          if (tempInputs.contains(exp.getOperand1().toString())
              && (edge.getCFAEdge().getCode().contains("__VERIFIER_nondet_"))) {
            String variableName = exp.getOperand1().toString();
            tempInputs.remove(variableName);
            variableName = "relevant: " + variableName;
            BigInteger value = new BigInteger(exp.getOperand2().toString());
            variableToValueAssignments.put(variableName, value);
          }
        }
      }
    }

    int index = 0;
    for (CFAEdgeWithAssumptions edge : path) {
      if (!edge.getCFAEdge().getCode().contains("__VERIFIER_nondet_")) {
        if (edge.getCFAEdge() instanceof CFunctionCallEdge) {
          CFunctionCallEdge fEdge = (CFunctionCallEdge) edge.getCFAEdge();
          if (fEdge.getRawAST().get() instanceof CFunctionCallAssignmentStatement) {
            CFunctionCallAssignmentStatement functionCall =
                (CFunctionCallAssignmentStatement) fEdge.getRawAST().get();
            /*boolean setNewTestStep =
                createAndAddVariableAssignment(functionCall, index, path, AssignmentType.INPUT,
                    remaining_inputVariables, inputAssignments, "relevant: ", false, true);*/
            CLeftHandSide cLeft = functionCall.getLeftHandSide();
            CIdExpression cld = cLeft instanceof CIdExpression ? (CIdExpression) cLeft : null;

            if (cld != null && tempInputs.contains(cld.getName())) {

              tempInputs.remove(cld.getName());

              BigInteger value;

              value = getVariableValueFromFunctionCall(index, path);

              if (value != null) {
                variableToValueAssignments.put("relevant: " + cld.getName(), value);
              }
            }
          }

        }
        index++;
        continue;
      }
      if (edge.getCFAEdge() instanceof CStatementEdge) {
        CStatementEdge statementEdge = (CStatementEdge) edge.getCFAEdge();
        if (statementEdge.getRawAST().get() instanceof CFunctionCallAssignmentStatement) {
          CFunctionCallAssignmentStatement functionCall =
              (CFunctionCallAssignmentStatement) statementEdge.getRawAST().get();
          /* boolean setNewTestStep =
              createAndAddVariableAssignment(functionCall, index, path, AssignmentType.INPUT,
                  remaining_inputVariables, inputAssignments, "irrelevant: ", true, false);*/
          CLeftHandSide cLeft = functionCall.getLeftHandSide();
          CIdExpression cld = cLeft instanceof CIdExpression ? (CIdExpression) cLeft : null;

          if (cld != null && tempInputs.contains(cld.getName())) {

            tempInputs.remove(cld.getName());

            BigInteger value;

            value = new BigInteger("0");
            variableToValueAssignments.put("irrelevant: " + cld.getName(), value);
          }
        }
      }
      index++;
    }
    return variableToValueAssignments;
  }

}
