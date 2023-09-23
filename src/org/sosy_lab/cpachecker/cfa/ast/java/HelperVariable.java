// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

// TODO better variable names and check variable visibility
public class HelperVariable {

  private static HelperVariable instance = null;
  private JClassType currentType = null;
  private JFieldDeclaration helperFieldDeclaration;
  private JClassType throwableInstance = null;

  private HelperVariable() {
    helperFieldDeclaration =
        new JFieldDeclaration(
            FileLocation.DUMMY,
            getThrowableInstance(),
            "MainApp_helper",
            "helper",
            false,
            true,
            false,
            false,
            VisibilityModifier.PUBLIC);
  }

  private HelperVariable(JClassType jct) {
    helperFieldDeclaration =
        new JFieldDeclaration(
            FileLocation.DUMMY,
            jct,
            "MainApp_helper",
            "helper",
            false,
            true,
            false,
            false,
            VisibilityModifier.PUBLIC);

    throwableInstance = jct;
  }

  public static HelperVariable getInstance() {
    if (instance == null) {
      instance = new HelperVariable();
    }
    return instance;
  }

  public static HelperVariable getInstance(JClassType throwable) {
    if (instance == null) {
      instance = new HelperVariable(throwable);
    }
    return instance;
  }

  private JClassType getThrowableInstance() {
    if (throwableInstance == null) {
      Set<JInterfaceType> extendsSerializable = new HashSet<>();

      JInterfaceType serializable =
          JInterfaceType.valueOf(
              "java.io.Serializable",
              "Serializable",
              VisibilityModifier.PUBLIC,
              extendsSerializable);

      Set<JInterfaceType> throwableInterfaces = new HashSet<>();

      throwableInterfaces.add(serializable);

      throwableInstance =
          JClassType.valueOf(
              "java.lang.Throwable",
              "Throwable",
              VisibilityModifier.PUBLIC,
              false,
              false,
              false,
              JClassType.getTypeOfObject(),
              throwableInterfaces);
    }
    return throwableInstance;
  }

  public JExpression helperNotEqualsExpression() {

    JIdExpression helperIdExpression =
        new JIdExpression(
            FileLocation.DUMMY, currentType, "MainApp_helper", helperFieldDeclaration);

    JExpression helperFieldAccess =
        new JFieldAccess(
            FileLocation.DUMMY,
            currentType,
            "MainApp_helper",
            helperFieldDeclaration,
            helperIdExpression);

    JExpression helperNotEqualsExpression =
        new JBinaryExpression(
            FileLocation.DUMMY,
            JSimpleType.getBoolean(),
            helperFieldAccess,
            new JNullLiteralExpression(FileLocation.DUMMY),
            BinaryOperator.NOT_EQUALS);

    return helperNotEqualsExpression;
  }

  public JStatement helperNotEqualsStatement() {

    JExpression helperNotEqualsExpression = helperNotEqualsExpression();

    JStatement helperNotEqualsTemp =
        new JExpressionStatement(FileLocation.DUMMY, helperNotEqualsExpression);

    return helperNotEqualsTemp;
  }

  public JIdExpression getCurrentHelperIdExpression() {
    JIdExpression helperIdExpression =
        new JIdExpression(
            FileLocation.DUMMY, currentType, "MainApp_helper", helperFieldDeclaration);

    return helperIdExpression;
  }

  public JExpression getRunTimeTypeEqualsExpression(JClassType exception) {
    JIdExpression helperIdExpression = getCurrentHelperIdExpression();

    JRunTimeTypeExpression helperRunTimeType =
        new JVariableRunTimeType(FileLocation.DUMMY, helperIdExpression);

    return new JRunTimeTypeEqualsType(FileLocation.DUMMY, helperRunTimeType, exception);
  }

  public JStatement getRunTimeTypeEqualsStatement(JClassType exception) {

    JExpression runTimeTypeEquals = getRunTimeTypeEqualsExpression(exception);
    return new JExpressionStatement(FileLocation.DUMMY, runTimeTypeEquals);
  }

  public JExpressionAssignmentStatement getHelperIsNull() {
    JNullLiteralExpression nullExpression = new JNullLiteralExpression(FileLocation.DUMMY);

    JLeftHandSide helperLeft =
        new JIdExpression(
            FileLocation.DUMMY, getThrowableInstance(), "MainApp_helper", helperFieldDeclaration);

    JExpressionAssignmentStatement helperNull =
        new JExpressionAssignmentStatement(FileLocation.DUMMY, helperLeft, nullExpression);

    return helperNull;
  }

  public JExpressionAssignmentStatement setHelperRightSideExpression(JExpression expression) {
    JLeftHandSide helperLeft = getCurrentHelperIdExpression();

    JExpressionAssignmentStatement helperExpression =
        new JExpressionAssignmentStatement(FileLocation.DUMMY, helperLeft, expression);

    return helperExpression;
  }

  public JFieldDeclaration getHelperFieldDeclaration() {
    return helperFieldDeclaration;
  }

  public void setCurrentJClassType(JClassType type) {
    currentType = type;
  }

  public void setThrowableInstance(JClassType type) {
    throwableInstance = type;
  }
}
