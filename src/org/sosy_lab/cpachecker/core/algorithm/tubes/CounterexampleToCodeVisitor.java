// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CounterexampleToCodeVisitor
    extends AAstNodeVisitor<Void, UnsupportedOperationException> {

  private static final String GLOBAL_SCOPE = "__global__";
  private static final String UNIQUE_SEPARATOR = "_tube_separator_";
  private final Deque<String> functionStack;
  private final Map<String, Integer> variableIds;
  private final StringBuilder cCode;
  private CFAEdge currentEdge;

  public CounterexampleToCodeVisitor() {
    functionStack = new ArrayDeque<>();
    functionStack.push(GLOBAL_SCOPE);
    variableIds = new HashMap<>();
    cCode = new StringBuilder();
  }

  public void setCurrentEdge(CFAEdge pCurrentEdge) {
    currentEdge = pCurrentEdge;
  }

  private String getVariableId(String name, boolean increase) {
    return name;
    /*String functionVariableName = functionStack.peek() + UNIQUE_SEPARATOR + name;
    if (!variableIds.containsKey(functionVariableName)) {
      variableIds.put(functionVariableName, 0);
    }
    int id =
        increase
            ? variableIds.merge(functionVariableName, 1, Integer::sum)
            : variableIds.get(functionVariableName);
    return functionVariableName + UNIQUE_SEPARATOR + id;*/
  }

  private void assign() {
    cCode.append(" = ");
  }

  private void endStatement() {
    cCode.append(";\n");
  }

  @Override
  protected Void visit(AFunctionCallExpression exp) throws UnsupportedOperationException {
    String name = exp.getDeclaration().getName();
    return null;
  }

  @Override
  protected Void visit(AInitializerExpression exp) throws UnsupportedOperationException {
    exp.getExpression().accept_(this);
    return null;
  }

  @Override
  protected Void visit(AFunctionCallStatement stmt) throws UnsupportedOperationException {
    List<? extends AParameterDeclaration> originalParameters =
        stmt.getFunctionCallExpression().getDeclaration().getParameters();
    List<? extends AExpression> inputParameters =
        stmt.getFunctionCallExpression().getParameterExpressions();
    Preconditions.checkArgument(inputParameters.size() == originalParameters.size(), "Argument list has to be equally long");
      for (int i = 0; i < inputParameters.size(); i++) {
        AExpression inputParameter = inputParameters.get(i);
        AParameterDeclaration originalParameter = originalParameters.get(i);
        originalParameter.accept_(this);
        assign();
        inputParameter.accept_(this);
        endStatement();
      }
    stmt.getFunctionCallExpression().accept_(this);
    return null;
  }

  @Override
  protected Void visit(AFunctionCallAssignmentStatement stmt) throws UnsupportedOperationException {
    stmt.getLeftHandSide().accept_(this);
    assign();
    stmt.getRightHandSide().accept_(this);
    endStatement();
    return null;
  }

  @Override
  protected Void visit(AExpressionStatement stmt) throws UnsupportedOperationException {
    return null;
  }

  @Override
  protected Void visit(AExpressionAssignmentStatement stmt) throws UnsupportedOperationException {
    stmt.getLeftHandSide().accept_(this);
    assign();
    stmt.getRightHandSide().accept_(this);
    endStatement();
    return null;
  }

  @Override
  protected Void visit(AReturnStatement stmt) throws UnsupportedOperationException {
    return null;
  }

  @Override
  protected Void visit(AFunctionDeclaration decl) throws UnsupportedOperationException {
    functionStack.push(decl.getName());
    if (decl.getName().equals("main")) {
      cCode.append(decl.toASTString().replace(";", " {\n"));
    } else {
      cCode.append(decl.toASTString()).append("\n");
    }
    return null;
  }

  @Override
  protected Void visit(AParameterDeclaration decl) throws UnsupportedOperationException {
    return null;
  }

  @Override
  protected Void visit(AVariableDeclaration decl) throws UnsupportedOperationException {
    cCode.append(decl.getType().toASTString(getVariableId(decl.getName(), false)));
    if (decl.getInitializer() != null) {
      assign();
      decl.getInitializer().accept_(this);
    }
    endStatement();
    return null;
  }

  @Override
  public Void visit(AArraySubscriptExpression exp) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(AIdExpression exp) throws UnsupportedOperationException {
    cCode.append(getVariableId(exp.getName(), false));
    return null;
  }

  @Override
  public Void visit(ABinaryExpression exp) throws UnsupportedOperationException {
    boolean isLogicalOperator = false;
    if (exp.getOperator() instanceof BinaryOperator binaryOp) {
      isLogicalOperator = binaryOp.isLogicalOperator();
    }
    if (isLogicalOperator) {
      cCode.append("if (!(");
      exp.getOperand1().accept_(this);
      cCode.append(" ").append(exp.getOperator().getOperator()).append(" ");
      exp.getOperand2().accept_(this);
      cCode.append(")) { return 0;}\n");
    } else {
      exp.getOperand1().accept_(this);
      cCode.append(" ").append(exp.getOperator().getOperator()).append(" ");
      exp.getOperand2().accept_(this);
      endStatement();
    }
    return null;
  }

  @Override
  public Void visit(ACastExpression exp) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(ACharLiteralExpression exp) throws UnsupportedOperationException {
    cCode.append(exp.toASTString());
    return null;
  }

  @Override
  public Void visit(AFloatLiteralExpression exp) throws UnsupportedOperationException {
    cCode.append(exp.toASTString());
    return null;
  }

  @Override
  public Void visit(AIntegerLiteralExpression exp) throws UnsupportedOperationException {
    cCode.append(exp.toASTString());
    return null;
  }

  @Override
  public Void visit(AStringLiteralExpression exp) throws UnsupportedOperationException {
    cCode.append(exp.toASTString());
    return null;
  }

  @Override
  public Void visit(AUnaryExpression exp) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CArrayDesignator pArrayDesignator) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CArrayRangeDesignator pArrayRangeDesignator)
      throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CFieldDesignator pFieldDesignator) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CTypeIdExpression pIastTypeIdExpression) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CInitializerList pInitializerList) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CDesignatedInitializer pCStructInitializerPart)
      throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CFieldReference pIastFieldReference) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CPointerExpression pointerExpression) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CComplexCastExpression complexCastExpression)
      throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CComplexTypeDeclaration pDecl) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CTypeDefDeclaration pDecl) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CEnumerator pDecl) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JArrayCreationExpression pJArrayCreationExpression)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JArrayInitializer pJArrayInitializer) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JArrayLengthExpression pJArrayLengthExpression)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JVariableRunTimeType pJThisRunTimeType) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JNullLiteralExpression pJNullLiteralExpression)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JEnumConstantExpression pJEnumConstantExpression)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JThisExpression pThisExpression) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JClassLiteralExpression pJClassLiteralExpression)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  @Override
  public Void visit(JClassInstanceCreation pJClassInstanceCreation)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Java is not supported");
  }

  public String finish() {
    return cCode.append("\n}\n").toString();
  }
}
