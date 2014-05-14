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
package org.sosy_lab.cpachecker.core.exactcounterexample;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.Model.Function;
import org.sosy_lab.cpachecker.core.Model.Variable;

import com.google.common.base.Joiner;


public class CFAEdgeWithAssignments {

  private final CFAEdge edge;
  private final Set<Assignment> assignments;
  private final Map<String, Object> addressMap;

  public CFAEdgeWithAssignments(CFAEdge pEdge, Set<Assignment> pAssignments,
      Map<String, Object> pAddressMap) {
    edge = pEdge;
    assignments = pAssignments;
    addressMap = pAddressMap;
  }

  public Set<Assignment> getAssignments() {
    return assignments;
  }

  public CFAEdge getCFAEdge() {
    return edge;
  }

  @Override
  public String toString() {
    return edge.toString() + " " + assignments.toString();
  }

  @Nullable
  private String getAsCode(CFAEdge pEdge) {

    if (assignments.size() < 0) {
      return null;
    }

    if (pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return handleDeclaration(((ADeclarationEdge) pEdge).getDeclaration());
    } else if (pEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return handleStatement(((AStatementEdge) pEdge).getStatement());
    } else if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      return handleFunctionCall( ((FunctionCallEdge)pEdge));
    } else if(pEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
      return handleMultiEdge((MultiEdge)pEdge);
    }

    return null;
  }

  @Nullable
  public String getAsCode() {

    return getAsCode(edge);
  }

  private String handleMultiEdge(MultiEdge pEdge) {

    Set<String> result = new HashSet<>(pEdge.getEdges().size());

    for (CFAEdge edge : pEdge) {
      String code = getAsCode(edge);

      if (code != null && !result.contains(code)) {
        result.add(code);
      }
    }

    if(result.size() < 1) {
      return null;
    } else {
      return Joiner.on(" ").join(result);
    }
  }

  private  String handleFunctionCall(FunctionCallEdge pFunctionCallEdge) {

    FunctionEntryNode functionEntryNode = pFunctionCallEdge.getSuccessor();

    String functionName = functionEntryNode.getFunctionName();

    List<? extends AParameterDeclaration> formalParameters =
        functionEntryNode.getFunctionParameters();

    List<String> formalParameterNames =
        functionEntryNode.getFunctionParameterNames();


    if (formalParameters == null) {
      return null;
    }

    //TODO Refactor, no splitting of strings!

    String[] parameterValuesAsCode = new String[formalParameters.size()];

    for (Assignment valuePair : assignments) {

      String termName = valuePair.getTerm().getName();
      String[] termFunctionAndVariableName = termName.split("::");

      if (!(termFunctionAndVariableName.length == 2)) {
        return null;
      }

      String termVariableName = termFunctionAndVariableName[1];
      String termFunctionName = termFunctionAndVariableName[0];

      if (!termFunctionName.equals(functionName)) {
        return null;
      }

      if (formalParameterNames.contains(termVariableName)) {

        int formalParameterPosition =
            formalParameterNames.indexOf(termVariableName);

        AParameterDeclaration formalParameterDeclaration =
            formalParameters.get(formalParameterPosition);

        String valueAsCode = getValueAsCode(valuePair.getValue(), formalParameterDeclaration.getType());

        if (valueAsCode == null ||
            !formalParameterDeclaration.getName().equals(termVariableName)) {
          return null;
        }

        parameterValuesAsCode[formalParameterPosition] = valueAsCode;
      } else {
        return null;
      }
    }

    if (parameterValuesAsCode.length < 1) {
      return null;
    }

    for(String value : parameterValuesAsCode) {
      if(value == null) {
        return null;
      }
    }

    Joiner joiner = Joiner.on(", ");
    String arguments = "(" + joiner.join(parameterValuesAsCode) + ")";

    return functionName + arguments + ";";
  }

  @Nullable
  private String handleAssignment(IAssignment assignment) {

    IALeftHandSide leftHandSide = assignment.getLeftHandSide();

    String functionName = edge.getPredecessor().getFunctionName();

    Object value = getValueObject(leftHandSide, functionName);

    if (value == null) {
      return null;
    }

    Type expectedType = leftHandSide.getExpressionType();
    String valueAsCode = getValueAsCode(value, expectedType);

    if(valueAsCode == null) {
      return null;
    }

    //TODO Check, what can and can't be parsed, and how to fix it

    if (!(leftHandSide instanceof CIdExpression)) {
      return null;
    }

    return leftHandSide.toASTString() + " = " + valueAsCode + ";";
  }

  private Object getValueObject(IALeftHandSide pLeftHandSide, String pFunctionName) {

    //If their is only one value, its the one we search
    if (assignments.size() == 1) {
      return assignments.iterator().next().getValue();
    }

    if (pLeftHandSide instanceof CIdExpression) {
      return getValueObject(
        ((CIdExpression) pLeftHandSide).getDeclaration(), pFunctionName);
    } else if(pLeftHandSide instanceof CPointerExpression) {
      return null;
    }

    return null;
  }

  @Nullable
  private Object getValueObject(CSimpleDeclaration pVarDcl, String functionName) {

    //If their is only one value, its the one we search
    if (assignments.size() == 1) {
      return assignments.iterator().next().getValue();
    }

    if (pVarDcl == null || functionName == null || (!(pVarDcl instanceof CVariableDeclaration)
        && !(pVarDcl instanceof CParameterDeclaration))) {
      return null;
    }

    String varName = pVarDcl.getName();
    String assignableTermVarName;

    if (pVarDcl instanceof CParameterDeclaration ||
        (!((CVariableDeclaration) pVarDcl).isGlobal())) {
      assignableTermVarName = functionName + "::" + varName;
    } else {
      assignableTermVarName = varName;
    }

    for (Assignment assignment : assignments) {

      AssignableTerm term = assignment.getTerm();

      boolean termBelongsToVariable = false;

      if (term instanceof Variable) {
        termBelongsToVariable = belongsToVariable(assignableTermVarName, (Variable) term);
      } else if (term instanceof Function) {
        termBelongsToVariable = belongsToVariable(assignableTermVarName, (Function) term);
      }

      if (termBelongsToVariable) {
        return assignment.getValue();
      }
    }

    return null;
  }

  private boolean belongsToVariable(String pAssignableTermVarName, Function pFunc) {

    if(pFunc.getArity() != 1) {
      return false;
    }

    String variableAddress = CFAPathWithAssignments.getAddressPrefix() + pAssignableTermVarName;

    if (addressMap.containsKey(variableAddress)) {
      Object addressValue = addressMap.get(variableAddress);
      Object argumentValue = pFunc.getArgument(0);
      if (addressValue.equals(argumentValue)) {
        return true;
      }
    }

    return false;
  }

  private boolean belongsToVariable(String pAssignableTermVarName, Variable pTerm) {
    return pTerm.getName().equals(pAssignableTermVarName);
  }

  @Nullable
  private String getValueAsCode(Object pValue, Type pExpectedType) {

    // TODO processing for other languages
    if(pExpectedType instanceof CType) {
      return ((CType) pExpectedType).accept(new TypeValueAsCodeVisitor(pValue));
    }

    return null;
  }

  @Nullable
  private String handleStatement(IAStatement pStatement) {

    if (pStatement instanceof AFunctionCallAssignmentStatement) {
      IAssignment assignmentStatement =
          ((AFunctionCallAssignmentStatement) pStatement);
      return handleAssignment(assignmentStatement);
    }

    if (pStatement instanceof AExpressionAssignmentStatement) {
      IAssignment assignmentStatement =
          ((AExpressionAssignmentStatement) pStatement);
      return handleAssignment(assignmentStatement);
    }

    return null;
  }

  private String handleDeclaration(IADeclaration dcl) {

    if (dcl instanceof CVariableDeclaration) {

      CVariableDeclaration varDcl = (CVariableDeclaration) dcl;

      String functionName = edge.getPredecessor().getFunctionName();

      Object value = getValueObject(varDcl, functionName);

      if (value == null) {
        return null;
      }

      Type dclType = varDcl.getType();
      String valueAsCode = getValueAsCode(value, dclType);

      if (valueAsCode == null) {
        return null;
      }

      return varDcl.getName() + " = "
          + valueAsCode + ";";
    }

    return null;
  }

  private static class TypeValueAsCodeVisitor extends DefaultCTypeVisitor<String, RuntimeException> {

    private final Object value;

    public TypeValueAsCodeVisitor(Object pValue) {
      value = pValue;
    }

    @Override
    public String visitDefault(CType pT) throws RuntimeException {
      return null;
    }

    @Override
    public String visit(CSimpleType simpleType) throws RuntimeException {
      switch (simpleType.getType()) {
      case BOOL:
      case INT:
        return handleIntegerNumbers(simpleType);
      case FLOAT:
      case DOUBLE:
        return handleFloatingPointNumbers(simpleType);
      }

      return null;
    }

    private String handleFloatingPointNumbers(CSimpleType pSimpleType) {

      //TODO Check length in given constraints.

      String value = this.value.toString();

      return value.matches("((-)?)(\\d*)|(.(\\d*))|((\\d*).)|((\\d*).(\\d*))") ? value : null;
    }

    private String handleIntegerNumbers(CSimpleType pSimpleType) {

      //TODO Check length in given constraints.
      String value = this.value.toString();

      if (value.matches("((-)?)\\d*")) {
        return value;
      } else {
        String[] numberParts = value.split("\\.");

        if (numberParts.length == 2 &&
            numberParts[1].matches("0*") &&
            numberParts[0].matches("((-)?)\\d*")) {

          return numberParts[0];
        }
      }

      value = handleFloatingPointNumbers(pSimpleType);

      if(value != null) {
        return "(int) " + value;
      }

      return null;
    }
  }
}