// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.error_condition;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

public class CounterexampleToCodeVisitor
    extends AAstNodeVisitor<Void, UnsupportedOperationException> {

  private static final String UNIQUE_SEPARATOR = "_";
  private final Deque<String> returnVariables;
  private final Map<String, Integer> variableIds;
  private final StringBuilder cCode;
  private boolean inAssignment = false;
  private boolean inBinaryExpression = false;

  private final Deque<Class<?>> callStack;
  private boolean isLastAssume = false;

  public CounterexampleToCodeVisitor() {
    variableIds = new HashMap<>();
    cCode = new StringBuilder();
    returnVariables = new ArrayDeque<>();
    callStack = new ArrayDeque<>();
  }

  public String visit(CounterexampleInfo pCounterexample) {
    CFAEdge lastAssume = null;
    List<CFAEdge> fullPath = pCounterexample.getTargetPath().getFullPath();
    for (int i = fullPath.size() - 1; i >= 0; i--) {
      if (fullPath.get(i).getEdgeType() == CFAEdgeType.AssumeEdge) {
        lastAssume = fullPath.get(i);
        break;
      }
    }
    for (CFAEdge cfaEdge : fullPath) {
      isLastAssume = cfaEdge.equals(lastAssume);
      cfaEdge.getRawAST().ifPresent(astNode -> astNode.accept_(this));
    }
    String result = cCode.append("ERROR: return 1;\n}").toString();
    return FluentIterable.from(Splitter.on("\n").splitToList(result))
        .filter(s -> !s.equals(";"))
        .join(Joiner.on("\n"));
  }

  private String getVariableId(String name, boolean increase) {
    name = name.replaceAll("::", "__");
    if (!variableIds.containsKey(name)) {
      variableIds.put(name, 0);
    }
    int id = increase ? variableIds.merge(name, 1, Integer::sum) : variableIds.get(name);
    return name + UNIQUE_SEPARATOR + id;
  }

  private void assign() {
    cCode.append(" = ");
  }

  private void endStatement() {
    if (callStack.size() == 1) {
      cCode.append(";\n");
    }
  }

  @Override
  protected Void visit(AFunctionCallExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    List<? extends AParameterDeclaration> originalParameters = exp.getDeclaration().getParameters();
    List<? extends AExpression> inputParameters = exp.getParameterExpressions();
    Preconditions.checkArgument(
        inputParameters.size() == originalParameters.size(),
        "Argument list has to be equally long");
    if (originalParameters.isEmpty()
        && exp.getDeclaration().getName().contains("__VERIFIER_nondet")) {
      if (!returnVariables.isEmpty()) {
        String assign = returnVariables.pop();
        cCode.append(assign);
        assign();
      }
      cCode.append(exp.getDeclaration().getName()).append("();\n");
      callStack.pop();
      return null;
    }
    inAssignment = true;
    for (int i = 0; i < inputParameters.size(); i++) {
      AExpression inputParameter = inputParameters.get(i);
      AParameterDeclaration originalParameter = originalParameters.get(i);
      originalParameter.accept_(this);
      assign();
      inputParameter.accept_(this);
      endStatement();
    }
    inAssignment = false;
    callStack.pop();
    return null;
  }

  @Override
  protected Void visit(AInitializerExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    exp.getExpression().accept_(this);
    callStack.pop();
    return null;
  }

  @Override
  protected Void visit(AFunctionCallStatement stmt) throws UnsupportedOperationException {
    callStack.push(stmt.getClass());
    stmt.getFunctionCallExpression().accept_(this);
    endStatement();
    callStack.pop();
    return null;
  }

  @Override
  protected Void visit(AFunctionCallAssignmentStatement stmt) throws UnsupportedOperationException {
    callStack.push(stmt.getClass());
    if (stmt.getLeftHandSide() instanceof CIdExpression id) {
      inAssignment = true;
      returnVariables.push(getVariableId(id.toQualifiedASTString(), false));
      stmt.getRightHandSide().accept_(this);
      inAssignment = false;
      callStack.pop();
      return null;
    }
    if (stmt.getLeftHandSide() instanceof CFieldReference field) {
      inAssignment = true;
      returnVariables.push(getVariableId(field.toQualifiedASTString(), false));
      stmt.getRightHandSide().accept_(this);
      inAssignment = false;
      callStack.pop();
      return null;
    }
    throw new AssertionError("Only IdExpressions are supported");
  }

  @Override
  protected Void visit(AExpressionStatement stmt) throws UnsupportedOperationException {
    callStack.push(stmt.getClass());
    stmt.getExpression().accept_(this);
    callStack.pop();
    return null;
  }

  @Override
  protected Void visit(AExpressionAssignmentStatement stmt) throws UnsupportedOperationException {
    callStack.push(stmt.getClass());
    inAssignment = true;
    stmt.getLeftHandSide().accept_(this);
    assign();
    stmt.getRightHandSide().accept_(this);
    endStatement();
    callStack.pop();
    inAssignment = false;
    return null;
  }

  @Override
  protected Void visit(AReturnStatement stmt) throws UnsupportedOperationException {
    callStack.push(stmt.getClass());
    if (stmt.asAssignment().isPresent() && !returnVariables.isEmpty()) {
      String variable = returnVariables.pop();
      cCode.append(variable);
      assign();
      inAssignment = true;
      stmt.asAssignment().orElseThrow().getRightHandSide().accept_(this);
      inAssignment = false;
      endStatement();
    }
    callStack.pop();
    return null;
  }

  @Override
  protected Void visit(AFunctionDeclaration decl) throws UnsupportedOperationException {
    callStack.push(decl.getClass());
    if (decl.getName().equals("main")) {
      cCode.append(decl.toASTString().replace(";", " {\n"));
    } else if (decl.getName().contains("__VERIFIER_nondet")) {
      cCode.append(decl.toASTString()).append("\n");
    }
    callStack.pop();
    return null;
  }

  @Override
  protected Void visit(AParameterDeclaration decl) throws UnsupportedOperationException {
    callStack.push(decl.getClass());
    cCode.append(decl.getType().toASTString(getVariableId(decl.getQualifiedName(), true)));
    callStack.pop();
    return null;
  }

  @Override
  protected Void visit(AVariableDeclaration decl) throws UnsupportedOperationException {
    callStack.push(decl.getClass());
    cCode.append(decl.getType().toASTString(getVariableId(decl.getQualifiedName(), false)));
    if (decl.getInitializer() != null) {
      assign();
      decl.getInitializer().accept_(this);
    }
    endStatement();
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(AArraySubscriptExpression exp) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(AIdExpression exp) throws UnsupportedOperationException {
    if (exp.getName().startsWith("__CPAchecker_TMP_") && !inAssignment && !inBinaryExpression) {
      return null;
    }
    cCode.append(getVariableId(exp.getDeclaration().getQualifiedName(), false));
    return null;
  }

  @Override
  public Void visit(ABinaryExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    inBinaryExpression = true;
    boolean isLogicalOperator = false;
    if (exp.getOperator() instanceof BinaryOperator binaryOp) {
      isLogicalOperator = binaryOp.isLogicalOperator();
    }
    if (isLogicalOperator && !inAssignment) {
      if (isLastAssume) {
        cCode.append("klee_assume(");
      } else {
        cCode.append("if (!(");
      }
      exp.getOperand1().accept_(this);
      cCode.append(" ").append(exp.getOperator().getOperator()).append(" ");
      exp.getOperand2().accept_(this);
      if (isLastAssume) {
        cCode.append(");\n");
      } else {
        cCode.append(")) return 0;\n");
      }
    } else {
      exp.getOperand1().accept_(this);
      cCode.append(" ").append(exp.getOperator().getOperator()).append(" ");
      exp.getOperand2().accept_(this);
      endStatement();
    }
    inBinaryExpression = false;
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(ACastExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    cCode.append("(").append(exp.getCastType().toASTString("")).append(") ");
    exp.getOperand().accept_(this);
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(ACharLiteralExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    cCode.append(exp.toASTString());
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(AFloatLiteralExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    cCode.append(exp.toASTString());
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(AIntegerLiteralExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    cCode.append(exp.toASTString());
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(AStringLiteralExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    cCode.append(exp.toASTString());
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(AUnaryExpression exp) throws UnsupportedOperationException {
    callStack.push(exp.getClass());
    cCode.append(exp.getOperator().getOperator());
    exp.getOperand().accept_(this);
    callStack.pop();
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
    cCode.append(pInitializerList.toQualifiedASTString());
    return null;
  }

  @Override
  public Void visit(CDesignatedInitializer pCStructInitializerPart)
      throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CFieldReference pIastFieldReference) throws UnsupportedOperationException {
    callStack.push(pIastFieldReference.getClass());
    cCode.append(getVariableId(pIastFieldReference.toQualifiedASTString(), false));
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(CPointerExpression pointerExpression) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public Void visit(CComplexCastExpression complexCastExpression)
      throws UnsupportedOperationException {
    callStack.push(complexCastExpression.getClass());
    cCode
        .append("(")
        .append(complexCastExpression.getExpressionType().toASTString(""))
        .append(") ");
    complexCastExpression.getOperand().accept(this);
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(CComplexTypeDeclaration pDecl) throws UnsupportedOperationException {
    callStack.push(pDecl.getClass());
    cCode.append(pDecl.toASTString()).append("\n");
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(CTypeDefDeclaration pDecl) throws UnsupportedOperationException {
    callStack.push(pDecl.getClass());
    cCode.append(pDecl.toASTString()).append("\n");
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(CEnumerator pDecl) throws UnsupportedOperationException {
    callStack.push(pDecl.getClass());
    cCode.append(pDecl.toASTString()).append("\n");
    callStack.pop();
    return null;
  }

  @Override
  public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnsupportedOperationException {
    callStack.push(pIastArraySubscriptExpression.getClass());
    pIastArraySubscriptExpression.getArrayExpression().accept(this);
    cCode.append("[");
    pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    cCode.append("]");
    callStack.pop();
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
}
