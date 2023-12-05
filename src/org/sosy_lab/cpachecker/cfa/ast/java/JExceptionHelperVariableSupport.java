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

/**
 * Adds methods to support the exception variable, including setting the variable to zero or
 * assigning a value to the variable.
 */
public class JExceptionHelperVariableSupport {

  private static JExceptionHelperVariableSupport instance = null;
  private JClassType currentType = JClassType.createUnresolvableType();
  private final JFieldDeclaration helperFieldDeclaration;
  private JClassType throwableClassType;

  private JExceptionHelperVariableSupport() {
    throwableClassType = getThrowable();
    helperFieldDeclaration =
        new JFieldDeclaration(
            FileLocation.DUMMY,
            throwableClassType,
            "CPAchecker_Exception_helper",
            "exception_helper",
            false,
            true,
            false,
            false,
            VisibilityModifier.PUBLIC);
  }

  public static JExceptionHelperVariableSupport getInstance() {
    if (instance == null) {
      instance = new JExceptionHelperVariableSupport();
    }
    return instance;
  }

  private JClassType getThrowable() {
      Set<JInterfaceType> extendsSerializable = new HashSet<>();

      JInterfaceType serializable =
          JInterfaceType.valueOf(
              "java.io.Serializable",
              "Serializable",
              VisibilityModifier.PUBLIC,
              extendsSerializable);

      Set<JInterfaceType> throwableInterfaces = new HashSet<>();

      throwableInterfaces.add(serializable);

    JClassType throwableTemp =
        JClassType.valueOf(
            "java.lang.Throwable",
            "Throwable",
            VisibilityModifier.PUBLIC,
            false,
            false,
            false,
            JClassType.getTypeOfObject(),
            throwableInterfaces);
    return throwableTemp;
  }

  /**
   * Returns a JBinaryExpression for the null check
   *
   * @return JBinaryExpression that compares the helper field to a null value
   */
  public JExpression helperNotEqualsExpression() {

    JIdExpression helperIdExpression =
        new JIdExpression(
            FileLocation.DUMMY,
            currentType,
            helperFieldDeclaration.getName(),
            helperFieldDeclaration);

    JExpression helperFieldAccess =
        new JFieldAccess(
            FileLocation.DUMMY,
            currentType,
            helperFieldDeclaration.getName(),
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

  /**
   * Statement that contains a check if the helper variable is null
   *
   * @return JStatement with check if helper variable is null
   */
  public JStatement helperNotEqualsNullStatement() {

    JExpression helperNotEqualsExpression = helperNotEqualsExpression();

    JStatement helperNotEqualsTemp =
        new JExpressionStatement(FileLocation.DUMMY, helperNotEqualsExpression);

    return helperNotEqualsTemp;
  }

  /**
   * Get current JIdExpression of helper variable
   *
   * @return JIdExpression of helper variable with current type
   */
  public JIdExpression getCurrentHelperIdExpression() {
    JIdExpression helperIdExpression =
        new JIdExpression(
            FileLocation.DUMMY,
            currentType,
            helperFieldDeclaration.getName(),
            helperFieldDeclaration);

    return helperIdExpression;
  }

  /**
   * Check if runtime type of exception is an instance of the classtype of another exception
   *
   * @param exception classtype of exception that is compared to the helper variable
   * @return JInstanceOfType that includes check if runtime types are equal
   */
  public JInstanceOfType getHelperRunTimeTypeEqualsExpression(JClassType exception) {
    JIdExpression helperIdExpression = getCurrentHelperIdExpression();

    JRunTimeTypeExpression helperRunTimeType =
        new JVariableRunTimeType(FileLocation.DUMMY, helperIdExpression);

    return new JInstanceOfType(FileLocation.DUMMY, helperRunTimeType, exception);
  }

  /**
   * Produce JStatement that includes the expression that checks if helper is an instance of the
   * JClassType of an exception
   *
   * @param exception exception classtype that gets compare to helper
   * @return JStatement with a JInstanceOfType that includes the helper and the classtype of another
   *     exception
   */
  public JStatement getInstanceOfStatement(JClassType exception) {

    JInstanceOfType runTimeTypeEquals = getHelperRunTimeTypeEqualsExpression(exception);
    return new JExpressionStatement(FileLocation.DUMMY, runTimeTypeEquals);
  }

  /**
   * Set helper to null
   *
   * @return JExpressionAssignmentStatement that represent the assignment of the null value to the
   *     helper variable
   */
  public JExpressionAssignmentStatement getHelperIsNull() {
    JNullLiteralExpression nullExpression = new JNullLiteralExpression(FileLocation.DUMMY);

    JLeftHandSide helperLeft =
        new JIdExpression(
            FileLocation.DUMMY,
            throwableClassType,
            helperFieldDeclaration.getName(),
            helperFieldDeclaration);

    JExpressionAssignmentStatement helperNull =
        new JExpressionAssignmentStatement(FileLocation.DUMMY, helperLeft, nullExpression);

    return helperNull;
  }

  /**
   * Assign value to helper variable
   *
   * @param expression expression of throw statement
   * @return JExpressionAssignmentStatement with helpervariable after value got assigned to it
   */
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

  public JClassType getCurrentClassType() {
    return currentType;
  }
}
