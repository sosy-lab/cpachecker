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
  // das hier alles ist anscheinend problematisch -> nach ner idee suchen wie ich das fixen kan
  // problem -> throwable ist hier schon mit dabei und kann dann anscheinend nicht als subclasse da
  // hinzugefügt werden -> schauen wie ich den JClassType von der exception bekomme
  private static Set<JInterfaceType> newSet = new HashSet<>();

  public static JClassType jct =
      new JClassType(
          "java.lang.Throwabl",
          "Throwabl",
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

  public static JExpression getRunTimeTypeEqualsExpression(JClassType exception) {
    return new JRunTimeTypeEqualsType(FileLocation.DUMMY, jre, exception);
  }

  public static JStatement getRunTimeTypeEqualsStatement(JClassType exception) {

    JExpression runTimeTypeEquals = getRunTimeTypeEqualsExpression(exception);
    return new JExpressionStatement(FileLocation.DUMMY, runTimeTypeEquals);
  }

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
