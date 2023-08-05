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
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

// TODO better variable names and check variable visibility
public class HelperVariable {
  private static Set<JInterfaceType> newSet = new HashSet<>();
  public static JClassType jct =
      new JClassType(
          "java.lang.Throwable",
          "Throwable",
          VisibilityModifier.PUBLIC,
          false,
          false,
          false,
          JClassType.getTypeOfObject(),
          newSet);

  public static JNullLiteralExpression tempNull = new JNullLiteralExpression(FileLocation.DUMMY);

  public static AInitializer temp = new JInitializerExpression(FileLocation.DUMMY, tempNull);
  public static JSimpleDeclaration declaration =
      new JVariableDeclaration(
          FileLocation.DUMMY, true, jct, "MainApp_helper", "helper", "helper", temp, false);

  public static JIdExpression jie =
      new JIdExpression(FileLocation.DUMMY, jct, "MainApp_helper", declaration);

  public static JRunTimeTypeExpression jre = new JVariableRunTimeType(FileLocation.DUMMY, jie);

  public static JExpression et = new JRunTimeTypeEqualsType(FileLocation.DUMMY, jre, jct);

  public static JStatement runTimeTypeEqualsStatement =
      new JExpressionStatement(FileLocation.DUMMY, et);

  // TODO check if ADeclaration/JSimpleDeclaration can be changed to this
  public static JFieldDeclaration jfd =
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

  public static JExpression d =
      new JFieldAccess(FileLocation.DUMMY, jct, "MainApp_helper", jfd, jie);

  public static JExpression helperNotEquals =
      new JBinaryExpression(
          FileLocation.DUMMY,
          JSimpleType.getBoolean(),
          d,
          new JNullLiteralExpression(FileLocation.DUMMY),
          BinaryOperator.NOT_EQUALS);

  public static JStatement helperNotEqualsStatement =
      new JExpressionStatement(FileLocation.DUMMY, helperNotEquals);
}
