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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

public class InputOutputValues {

  private Set<String> inputVariables;
  private Set<String> outputVariables;

  private class Variable {
    private String name;
    private String arrayIndex;

    public String getName() {
      return name;
    }

    public String getArrayIndex() {
      return arrayIndex;
    }

    public String getFullName() {

      if (arrayIndex != null) {
        return name + "[" + arrayIndex + "]";
      } else {
        return name;
      }
    }

    public Variable(String name, String arrayIndex) {
      this.name = name;
      this.arrayIndex = arrayIndex;
    }

  }

  public InputOutputValues(String inputInterface, String outputInterface) {
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
            CIntegerLiteralExpression rightSide = (CIntegerLiteralExpression) rightHand;
            return rightSide.getValue();
          }
          if (assignment instanceof CExpressionAssignmentStatement) {
            return getValueFromComment(edge);
          }
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
          /*
           * boolean setNewTestStep = createAndAddVariableAssignment(functionCall, index, path,
           * AssignmentType.OUTPUT, outputVariables, outputAssignments, "", false, false);
           */
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


  private boolean isArray(String operand) {
    return operand.endsWith("]");
  }

  private String getArrayName(String operand) {
    return operand.split("\\[")[0];
  }

  private String getCurrentArrayIndex(
      String operand,
      CFAEdgeWithAssumptions edge,
      CFAPathWithAssumptions path) {
    // String arrayIndexVariable = operand.sub
    String indexVariable = operand.split("\\[")[1].split("\\]")[0];
    //check if the used index is a constant
    try {
      return String.valueOf(Integer.parseInt(indexVariable));
    } catch (NumberFormatException ex) {
    }

    throw new UnsupportedOperationException();
  }

  private Variable
      getVariable(String fullName, CFAEdgeWithAssumptions edge, CFAPathWithAssumptions path) {
    if (isArray(fullName)) {
      return new Variable(getArrayName(fullName), getCurrentArrayIndex(fullName, edge, path));
    } else {
      return new Variable(fullName, null);
    }
  }

  private void tryGetInputValueFromStatements(
      Set<String> tempInputs,
      Map<String, BigInteger> variableToValueAssignments,
      CFAEdgeWithAssumptions edge,
      CFAPathWithAssumptions path) {

    if (!edge.getCFAEdge().getCode().contains("__VERIFIER_nondet_")) {
      return;
    }

    Collection<AExpressionStatement> expStmts = edge.getExpStmts();
    for (AExpressionStatement expStmt : expStmts) {
      if (expStmt.getExpression() instanceof CBinaryExpression) {
        CBinaryExpression exp = (CBinaryExpression) expStmt.getExpression();

        Variable var = getVariable(exp.getOperand1().toString(), edge, path);

        if (tempInputs.isEmpty() || tempInputs.contains(var.getName())) {
          BigInteger value = new BigInteger(exp.getOperand2().toString());
          variableToValueAssignments.put(var.getFullName(), value);
        }
      }
    }
  }

  public void tryGetInputValueFromFunctionCallEdge(
      CFunctionCallEdge fEdge,
      Set<String> tempInputs,
      Map<String, BigInteger> variableToValueAssignments,
      CFAPathWithAssumptions path,
      int index) {
      if (fEdge.getRawAST().get() instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement functionCall =
            (CFunctionCallAssignmentStatement) fEdge.getRawAST().get();
        /*
         * boolean setNewTestStep = createAndAddVariableAssignment(functionCall, index, path,
         * AssignmentType.INPUT, remaining_inputVariables, inputAssignments, "relevant: ", false,
         * true);
         */
        CLeftHandSide cLeft = functionCall.getLeftHandSide();
        CIdExpression cld = cLeft instanceof CIdExpression ? (CIdExpression) cLeft : null;

        if (cld != null) {
          String operand1 = cld.getName();
          String arrayOperand = "";
          if (operand1.endsWith("]")) {
            String split[] = operand1.split("\\[");
            operand1 = split[0];
            arrayOperand = split[1];
          }

        if (tempInputs.isEmpty() || tempInputs.contains(operand1)) {
            tempInputs.remove(cld.getName());

            BigInteger value;

            value = getVariableValueFromFunctionCall(index, path);

            if (value != null) {
              variableToValueAssignments.put("relevant: " + cld.getName(), value);
            }
          }
        }
      }
  }

  public void tryGetInputValueFromStatementEdge(
      CStatementEdge statementEdge,
      Set<String> tempInputs,
      Map<String, BigInteger> variableToValueAssignments) {
    if (statementEdge.getRawAST().get() instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement functionCall =
          (CFunctionCallAssignmentStatement) statementEdge.getRawAST().get();
      /*
       * boolean setNewTestStep = createAndAddVariableAssignment(functionCall, index, path,
       * AssignmentType.INPUT, remaining_inputVariables, inputAssignments, "irrelevant: ", true,
       * false);
       */
      CLeftHandSide cLeft = functionCall.getLeftHandSide();
      CIdExpression cld = cLeft instanceof CIdExpression ? (CIdExpression) cLeft : null;

      if (cld != null) {
        String operand1 = cld.getName();
        String arrayOperand = "";
        if (operand1.endsWith("]")) {
          String split[] = operand1.split("\\[");
          operand1 = split[0];
          arrayOperand = split[1];
        }

        if (tempInputs.isEmpty() || tempInputs.contains(operand1)) {

          tempInputs.remove(cld.getName());

          BigInteger value;

          value = new BigInteger("0");
          variableToValueAssignments.put("irrelevant: " + cld.getName(), value);
        }
      }
    }
  }

  private class Value {
    String name;
    public Value(String name) {
      this.name = name;
    }
  }
  private class PrimitiveValue extends Value {
    List<String> ssaValues;

    public PrimitiveValue(String name) {
      super(name);
    }

    public void addValue(String value) {
      if (ssaValues == null) {
        ssaValues = new ArrayList<String>();
      }
      ssaValues.add(value);
    }

    @Override
    public String toString() {
      String str = "";
      for (int i = 0; i < ssaValues.size(); i++) {
        str += name + "_" + String.valueOf(i) + " : " + ssaValues.get(i);
      }
      return str;
    }
  }

  private class ArrayValue extends Value {
    Map<Integer, List<String>> values;

    public ArrayValue(String name) {
      super(name);
    }

    public void addValue(int index, String value) {
      if (values == null) {
        values = new HashMap<Integer, List<String>>();
      }
      if (values.containsKey(index)) {
        values.get(index).add(value);
      } else {
        List<String> valueList = new ArrayList<String>();
        valueList.add(value);
        values.put(index, valueList);
      }
    }

    @Override
    public String toString() {
      String str = "";
      for(Integer key: values.keySet()) {
        for(int i = 0; i < values.get(key).size(); i++) {
          str += name +"["+key+ "]"+ "_" + String.valueOf(i) + " : " + values.get(key).get(i);
        }
      }
      return str;
    }

  }

  public Map<String, BigInteger> extractInputValues(CounterexampleInfo cex) {
    // List<Value> values = new ArrayList<InputOutputValues.Value>();
//    CFAPathWithAssumptions path = cex.getCFAPathWithAssignments();
    // for (CFAEdgeWithAssumptions edge : path) {
    // Collection<AExpressionStatement> expStmts = edge.getExpStmts();
    // for (AExpressionStatement expStmt : expStmts) {
    // if (expStmt.getExpression() instanceof CBinaryExpression) {
    // CBinaryExpression exp = (CBinaryExpression) expStmt.getExpression();
//
    // String fullName = exp.getOperand1().toString();
    // if (isArray(fullName)) {
    // String name = getArrayName(fullName);
    // String index = getCurrentArrayIndex(fullName, edge, path);
    // String value = exp.getOperand2().toString();
    // for(ArrayValue val: values) {
    //
    // }
    // }
    // else {
    // return new Variable(fullName, null);
    // }
    //
    // Variable var = getVariable(exp.getOperand1().toString(), edge, path);
    //
//
    // if (tempInputs.contains(var.getName())) {
    // BigInteger value = new BigInteger(exp.getOperand2().toString());
    // variableToValueAssignments.put(var.getFullName(), value);
    // }
//        }
//    }

    Map<String, BigInteger> variableToValueAssignments = new LinkedHashMap<>();
    Set<String> tempInputs;
    if (inputVariables.isEmpty()) {
      tempInputs = new LinkedHashSet<>();
    } else {
      boolean isEmpty = true;
      for (String var : inputVariables) {
        if (var.trim().isEmpty()) {
          isEmpty = true;
          break;
        }
      }
      if (isEmpty) {
        tempInputs = new LinkedHashSet<>();
      } else {
        tempInputs = new LinkedHashSet<>(inputVariables);
      }
    }
    CFAPathWithAssumptions path = cex.getCFAPathWithAssignments();

    for (CFAEdgeWithAssumptions edge : path) {
      tryGetInputValueFromStatements(tempInputs, variableToValueAssignments, edge, path);
    }

    int index = 0;
    for (CFAEdgeWithAssumptions edge : path) {
      if (!edge.getCFAEdge().getCode().contains("__VERIFIER_nondet_")) {
        if (edge.getCFAEdge() instanceof CFunctionCallEdge) {
          CFunctionCallEdge fEdge = (CFunctionCallEdge) edge.getCFAEdge();
          tryGetInputValueFromFunctionCallEdge(
              fEdge,
              tempInputs,
              variableToValueAssignments,
              path,
              index);
        }
        index++;
        continue;
      } /*
         * Omitted for testcomp, needs rework anyway if (edge.getCFAEdge() instanceof
         * CStatementEdge) { CStatementEdge statementEdge = (CStatementEdge) edge.getCFAEdge();
         * tryGetInputValueFromStatementEdge(statementEdge, tempInputs, variableToValueAssignments);
         * }
         */
      index++;
    }
    return variableToValueAssignments;
  }

}
