// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.builder;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public class SmtLibTheoryDeclarations {

  /* Integer stuff */

  public static SvLibVariableDeclaration INT_EQUALITY =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "=",
          "=",
          "=");

  public static SvLibVariableDeclaration INT_LESS_THAN =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "<",
          "<",
          "<");

  public static SvLibVariableDeclaration INT_LESS_EQUAL_THAN =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "<=",
          "<=",
          "<=");

  public static SvLibVariableDeclaration INT_GREATER_THAN =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          ">",
          ">",
          ">");

  public static SvLibVariableDeclaration INT_GREATER_EQUAL_THAN =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          ">=",
          ">=",
          ">=");

  public static SvLibVariableDeclaration intSubtraction(int amountArguments) {
    Verify.verify(amountArguments == 1 || amountArguments == 2);
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new SvLibFunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.INT)),
            SvLibSmtLibPredefinedType.INT),
        "-",
        "-",
        "-");
  }

  public static SvLibVariableDeclaration INT_MULTIPLICATION =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "*",
          "*",
          "*");

  public static SvLibVariableDeclaration intAddition(int amountArguments) {
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new SvLibFunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.INT)),
            SvLibSmtLibPredefinedType.INT),
        "+",
        "+",
        "+");
  }

  public static SvLibVariableDeclaration INT_DIV =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "div",
          "div",
          "div");

  /* Non-Linear Integer stuff */

  public static SvLibVariableDeclaration INT_MOD =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "mod",
          "mod",
          "mod");

  /* Boolean stuff */

  public static SvLibVariableDeclaration BOOL_NEGATION =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.BOOL),
              SvLibSmtLibPredefinedType.BOOL),
          "not",
          "not",
          "not");

  public static SvLibVariableDeclaration boolDisjunction(int amountArguments) {
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new SvLibFunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "or",
        "or",
        "or");
  }

  public static SvLibVariableDeclaration boolConjunction(int amountArguments) {
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new SvLibFunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "and",
        "and",
        "and");
  }

  public static SvLibVariableDeclaration boolImplication(int amountArguments) {
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new SvLibFunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "=>",
        "=>",
        "=>");
  }

  /** Real stuff */
  public static SvLibVariableDeclaration REAL_FLOOR =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL),
              SvLibSmtLibPredefinedType.INT),
          "floor",
          "floor",
          "floor");

  public static SvLibVariableDeclaration REAL_MINUS =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL, SvLibSmtLibPredefinedType.REAL),
              SvLibSmtLibPredefinedType.REAL),
          "-",
          "-",
          "-");

  public static SvLibVariableDeclaration REAL_MULTIPLICATION =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL, SvLibSmtLibPredefinedType.REAL),
              SvLibSmtLibPredefinedType.REAL),
          "*",
          "*",
          "*");

  public static SvLibVariableDeclaration realAddition(int amountArguments) {
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new SvLibFunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.REAL)),
            SvLibSmtLibPredefinedType.REAL),
        "+",
        "+",
        "+");
  }

  /** Array stuff */
  public static SvLibVariableDeclaration arraySelect(SvLibType indexType, SvLibType elementType) {
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new SvLibFunctionType(
            FileLocation.DUMMY,
            ImmutableList.of(new SvLibSmtLibArrayType(indexType, elementType), indexType),
            elementType),
        "select",
        "select",
        "select");
  }

  public static SvLibVariableDeclaration arrayStore(SvLibType indexType, SvLibType elementType) {
    return new SvLibVariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new SvLibFunctionType(
            FileLocation.DUMMY,
            ImmutableList.of(
                new SvLibSmtLibArrayType(indexType, elementType), indexType, elementType),
            new SvLibSmtLibArrayType(indexType, elementType)),
        "store",
        "store",
        "store");
  }
}
