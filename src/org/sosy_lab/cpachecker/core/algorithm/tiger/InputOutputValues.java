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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCaseVariable;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.harness.PredefinedTypes;

public class InputOutputValues {

  private Set<String> inputVariables;
  private Set<String> outputVariables;


  public InputOutputValues(String inputInterface, String outputInterface) {
    inputVariables = new TreeSet<>();
    for (String variable : inputInterface.split(",")) {
      String var = variable.trim();
      if (!var.isEmpty()) {
        inputVariables.add(var);
      }
    }
    outputVariables = new TreeSet<>();
    for (String variable : outputInterface.split(",")) {
      String var = variable.trim();
      if (!var.isEmpty()) {
        outputVariables.add(var);
      }
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

  public List<TestCaseVariable> extractOutputValues(CounterexampleInfo cex) {
    if (outputVariables == null || outputVariables.isEmpty()) {
      return Collections.emptyList();
    }
    Set<String> tmpOutputVariables = new LinkedHashSet<>(outputVariables);
    List<TestCaseVariable> variableToValueAssignments = new ArrayList<>();
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
              variableToValueAssignments.add(new TestCaseVariable(cld.getName(), value.toString()));
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

  private void tryGetInputValueFromStatements(
      Set<String> tempInputs,
      List<TestCaseVariable> variableToValueAssignments,
      CFAEdgeWithAssumptions edge,
      CFAPathWithAssumptions path) {

    if (!edge.getCFAEdge().getCode().contains("__VERIFIER_nondet_")) {
      return;
    }

    Collection<AExpressionStatement> expStmts = edge.getExpStmts();
    for (AExpressionStatement expStmt : expStmts) {
      if (expStmt.getExpression() instanceof CBinaryExpression) {
        CBinaryExpression exp = (CBinaryExpression) expStmt.getExpression();

        String name = exp.getOperand1().toString();

        if (tempInputs.isEmpty() || tempInputs.contains(name)) {
          BigInteger value = new BigInteger(exp.getOperand2().toString());
          variableToValueAssignments.add(new TestCaseVariable(name, value.toString()));
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

  private CFAEdge getEdge(CFANode parent, CFANode child) {
    for (int i = 0; i < parent.getNumLeavingEdges(); i++) {
      if (parent.getLeavingEdge(i).getSuccessor() == child) {
        return parent.getLeavingEdge(i);
      }
    }

    for (int i = 0; i < child.getNumEnteringEdges(); i++) {
      if (child.getEnteringEdge(i).getPredecessor() == parent) {
        return child.getEnteringEdge(i);
      }
    }
    return null;
  }

  public List<TestCaseVariable> extractInputValues(CounterexampleInfo cex, CFA pCFA) {
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
    Set<String> tempInputs = new LinkedHashSet<>(inputVariables);

    // CFAPathWithAssumptions path = cex.getCFAPathWithAssignments();
    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap = cex.getExactVariableValues();

    final Predicate<? super ARGState> relevantStates =
        Predicates.in(cex.getTargetPath().getStateSet());
    final Predicate<? super Pair<ARGState, ARGState>> relevantEdges =
        Predicates.in(cex.getTargetPath().getStatePairs());

    List<TestCaseVariable> values = new ArrayList<>();
    Set<ARGState> visited = Sets.newHashSet();
    Deque<ARGState> stack = Queues.newArrayDeque();
    Deque<CFAEdge> lastEdgeStack = Queues.newArrayDeque();
    stack.push(cex.getTargetPath().getFirstState());
    visited.addAll(stack);
    Optional<TestCaseVariable> value;
    while (!stack.isEmpty()) {
      ARGState previous = stack.pop();
      CFAEdge lastEdge = null;
      if (!lastEdgeStack.isEmpty()) {
        lastEdge = lastEdgeStack.pop();
      }
      if (AbstractStates.isTargetState(previous)) {
        // end of cex path reached, write test values
        assert lastEdge != null : "Expected target state to be different from root state, but was not";
      }
      ARGState parent = previous;
      Iterable<CFANode> parentLocs = AbstractStates.extractLocations(parent);
      for (ARGState child : parent.getChildren()) {
        if (relevantStates.apply(child) && relevantEdges.apply(Pair.of(parent, child))) {
          Iterable<CFANode> childLocs = AbstractStates.extractLocations(child);
          for (CFANode parentLoc : parentLocs) {
            for (CFANode childLoc : childLocs) {
              CFAEdge edge = getEdge(parentLoc, childLoc);

                // add the required values for external non-void functions
                if (edge instanceof AStatementEdge) {
                  AStatementEdge statementEdge = (AStatementEdge) edge;
                  if (statementEdge.getStatement() instanceof AFunctionCall) {
                    value =
                        getReturnValueForExternalFunction(
                            (AFunctionCall) statementEdge.getStatement(),
                            edge,
                            valueMap.get(previous),
                            pCFA);
                    if (value.isPresent()) {
                      values.add(value.get());
                    }
                  }
                }

                if (visited.add(child)) {
                  stack.push(child);
                  lastEdgeStack.push(edge);
                }

            }
          }
        }
      }
    }

    /*
     *
     * for (CFAEdgeWithAssumptions edge : path) { tryGetInputValueFromStatements(tempInputs,
     * variableToValueAssignments, edge, path); }
     *
     * if (tempInputs != null && !tempInputs.isEmpty()) { int index = 0; for (CFAEdgeWithAssumptions
     * edge : path) { if (!edge.getCFAEdge().getCode().contains("__VERIFIER_nondet_")) { if
     * (edge.getCFAEdge() instanceof CFunctionCallEdge) { CFunctionCallEdge fEdge =
     * (CFunctionCallEdge) edge.getCFAEdge(); tryGetInputValueFromFunctionCallEdge( fEdge,
     * tempInputs, variableToValueAssignments, path, index); } index++; continue; } /* Omitted for
     * testcomp, needs rework anyway if (edge.getCFAEdge() instanceof CStatementEdge) {
     * CStatementEdge statementEdge = (CStatementEdge) edge.getCFAEdge();
     * tryGetInputValueFromStatementEdge(statementEdge, tempInputs, variableToValueAssignments); }
     *
     * index++; } }
     */
    return values;
  }

  public static Optional<TestCaseVariable> getReturnValueForExternalFunction(
      final AFunctionCall functionCall,
      final CFAEdge edge,
      final @Nullable Collection<CFAEdgeWithAssumptions> pAssumptions,
      final CFA pCfa) {
    AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    AFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

    if (!PredefinedTypes.isKnownTestFunction(functionDeclaration)
        && !(functionCallExpression.getExpressionType() instanceof CVoidType)
        && (functionCallExpression.getExpressionType() != JSimpleType.getVoid())) {
      // only write if not predefined like e.g. malloc and is non-void

      AExpression nameExpression = functionCallExpression.getFunctionNameExpression();
      if (nameExpression instanceof AIdExpression) {

        ASimpleDeclaration declaration = ((AIdExpression) nameExpression).getDeclaration();
        if (declaration != null && pCfa.getFunctionHead(declaration.getQualifiedName()) == null) {
          // external function with return value

          if (functionCall instanceof AFunctionCallAssignmentStatement) {
            AFunctionCallAssignmentStatement assignment =
                (AFunctionCallAssignmentStatement) functionCall;

            if (pAssumptions != null) {
              // get return value from assumptions in counterexample
              for (AExpression assumption : FluentIterable.from(pAssumptions)
                  .filter(e -> e.getCFAEdge().equals(edge))
                  .transformAndConcat(CFAEdgeWithAssumptions::getExpStmts)
                  .transform(AExpressionStatement::getExpression)) {

                if (assumption instanceof ABinaryExpression
                    && ((ABinaryExpression) assumption).getOperator() == BinaryOperator.EQUALS) {

                  ABinaryExpression binExp = (ABinaryExpression) assumption;

                  if (binExp.getOperand2() instanceof ALiteralExpression
                      && binExp.getOperand1().equals(assignment.getLeftHandSide())) {
                    return Optional
                        .of(
                            new TestCaseVariable(
                                String.valueOf(assignment.getLeftHandSide()),
                                String.valueOf(
                                    ((ALiteralExpression) binExp.getOperand2()).getValue())));
                  }
                  if (binExp.getOperand1() instanceof ALiteralExpression
                      && binExp.getOperand2().equals(assignment.getLeftHandSide())) {
                    return Optional
                        .of(
                            new TestCaseVariable(
                                String.valueOf(assignment.getLeftHandSide()),
                                String.valueOf(
                                    ((ALiteralExpression) binExp.getOperand1()).getValue())));
                  }
                }
              }
            }

            // could not find any value
            // or value is irrelevant (case of function call statement)
            // use default value
            Type returnType = functionDeclaration.getType().getReturnType();
            if (returnType instanceof CType) {
              returnType = ((CType) returnType).getCanonicalType();

              if (!(returnType instanceof CCompositeType
                  || returnType instanceof CArrayType
                  || returnType instanceof CBitFieldType
                  || (returnType instanceof CElaboratedType
                      && ((CElaboratedType) returnType).getKind() != ComplexTypeKind.ENUM))) {

                return Optional.of(
                    new TestCaseVariable(
                        "Dummy",
                    String.valueOf(
                        ((ALiteralExpression) ((CInitializerExpression) CDefaults
                            .forType((CType) returnType, FileLocation.DUMMY)).getExpression())
                                .getValue()
                                    .toString())));
              } else {
                throw new AssertionError("Cannot write test case value (not a literal)");
              }
            } else {
              throw new AssertionError("Cannot write test case value (not a CType)");
            }
          }
        }
      }
    }
    return Optional.empty();
  }

}
