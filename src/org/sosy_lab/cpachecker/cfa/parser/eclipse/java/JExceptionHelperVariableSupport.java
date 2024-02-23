// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

/**
 * Adds methods to support the exception variable, including setting the variable to zero or
 * assigning a value to the variable.
 */
class JExceptionHelperVariableSupport {

  private final JFieldDeclaration helperFieldDeclaration;
  private final JClassType throwableClassType;

  JExceptionHelperVariableSupport(TypeHierarchy typeHierarchy) {
    throwableClassType = getThrowable(typeHierarchy);
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

  private JClassType getThrowable(TypeHierarchy typeHierarchy) {
    if (typeHierarchy.containsClassType(Throwable.class.getCanonicalName())) {
      return typeHierarchy.getClassType(Throwable.class.getCanonicalName());
    }

    JInterfaceType serializable;
    if (typeHierarchy.containsInterfaceType(Serializable.class.getCanonicalName())) {
      serializable = typeHierarchy.getInterfaceType(Serializable.class.getCanonicalName());
    } else {
      serializable =
          JInterfaceType.valueOf(
              "java.lang.Serializable",
              "Serializable",
              VisibilityModifier.PUBLIC,
              ImmutableSet.of());
    }

    JClassType throwableTemp =
        JClassType.valueOf(
            "java.lang.Throwable",
            "Throwable",
            VisibilityModifier.PUBLIC,
            false,
            false,
            false,
            JClassType.getTypeOfObject(),
            ImmutableSet.of(serializable));
    typeHierarchy.updateTypeHierarchy(throwableTemp);
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
            helperFieldDeclaration.getType(),
            helperFieldDeclaration.getName(),
            helperFieldDeclaration);

    JExpression helperFieldAccess =
        new JFieldAccess(
            FileLocation.DUMMY,
            helperFieldDeclaration.getType(),
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
    return new JExpressionStatement(FileLocation.DUMMY, helperNotEqualsExpression());
  }

  /**
   * Get current JIdExpression of helper variable
   *
   * @return JIdExpression of helper variable with current type
   */
  public JIdExpression getCurrentHelperIdExpression() {
    return new JIdExpression(
        FileLocation.DUMMY,
        helperFieldDeclaration.getType(),
        helperFieldDeclaration.getName(),
        helperFieldDeclaration);
  }

  /**
   * Set helper to null
   *
   * @return JExpressionAssignmentStatement that represent the assignment of the null value to the
   *     helper variable
   */
  public JExpressionAssignmentStatement setExceptionHelperVariableToNull() {
    JNullLiteralExpression nullExpression = new JNullLiteralExpression(FileLocation.DUMMY);

    JLeftHandSide helperLeft =
        new JIdExpression(
            FileLocation.DUMMY,
            helperFieldDeclaration.getType(),
            helperFieldDeclaration.getName(),
            helperFieldDeclaration);

    return new JExpressionAssignmentStatement(FileLocation.DUMMY, helperLeft, nullExpression);
  }

  /**
   * Assign value to helper variable
   *
   * @param expression expression of throw statement
   * @return JExpressionAssignmentStatement with helpervariable after value got assigned to it
   */
  public JExpressionAssignmentStatement setHelperRightSideExpression(JExpression expression) {
    return new JExpressionAssignmentStatement(
        FileLocation.DUMMY, getCurrentHelperIdExpression(), expression);
  }

  public JFieldDeclaration getHelperFieldDeclaration() {
    return helperFieldDeclaration;
  }
}
