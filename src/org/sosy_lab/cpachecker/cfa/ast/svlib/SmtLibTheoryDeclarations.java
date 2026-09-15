// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;

public class SmtLibTheoryDeclarations {

  /* Integer stuff */

  public static SvLibFunctionDeclaration INT_EQUALITY =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "=",
          "=",
          // Empty parameter list, because it has no body
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_LESS_THAN =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "<",
          "<",
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_LESS_EQUAL_THAN =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          "<=",
          "<=",
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_GREATER_THAN =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          ">",
          ">",
          ImmutableList.of());

  public static SvLibFunctionDeclaration INT_GREATER_EQUAL_THAN =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.BOOL),
          ">=",
          ">=",
          ImmutableList.of());

  public static SvLibFunctionDeclaration intSubtraction(int amountArguments) {
    Verify.verify(amountArguments == 1 || amountArguments == 2);
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.INT)),
            SvLibSmtLibPredefinedType.INT),
        "-",
        "-",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration INT_MULTIPLICATION =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "*",
          "*",
          ImmutableList.of());

  public static SvLibFunctionDeclaration intAddition(int amountArguments) {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.INT)),
            SvLibSmtLibPredefinedType.INT),
        "+",
        "+",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration INT_DIV =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "div",
          "div",
          ImmutableList.of());

  /* Non-Linear Integer stuff */

  public static SvLibFunctionDeclaration INT_MOD =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.INT, SvLibSmtLibPredefinedType.INT),
              SvLibSmtLibPredefinedType.INT),
          "mod",
          "mod",
          ImmutableList.of());

  /* Boolean stuff */

  public static SvLibFunctionDeclaration BOOL_NEGATION =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.BOOL), SvLibSmtLibPredefinedType.BOOL),
          "not",
          "not",
          ImmutableList.of());

  public static SvLibFunctionDeclaration boolDisjunction(int amountArguments) {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "or",
        "or",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration boolConjunction(int amountArguments) {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "and",
        "and",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration boolImplication(int amountArguments) {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.BOOL)),
            SvLibSmtLibPredefinedType.BOOL),
        "=>",
        "=>",
        ImmutableList.of());
  }

  /** Real stuff */
  public static SvLibFunctionDeclaration REAL_FLOOR =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL), SvLibSmtLibPredefinedType.INT),
          "floor",
          "floor",
          ImmutableList.of());

  public static SvLibFunctionDeclaration REAL_MINUS =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL, SvLibSmtLibPredefinedType.REAL),
              SvLibSmtLibPredefinedType.REAL),
          "-",
          "-",
          ImmutableList.of());

  public static SvLibFunctionDeclaration REAL_MULTIPLICATION =
      new SvLibFunctionDeclaration(
          FileLocation.DUMMY,
          new SvLibFunctionType(
              ImmutableList.of(SvLibSmtLibPredefinedType.REAL, SvLibSmtLibPredefinedType.REAL),
              SvLibSmtLibPredefinedType.REAL),
          "*",
          "*",
          ImmutableList.of());

  public static SvLibFunctionDeclaration realAddition(int amountArguments) {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.copyOf(
                Collections.nCopies(amountArguments, SvLibSmtLibPredefinedType.REAL)),
            SvLibSmtLibPredefinedType.REAL),
        "+",
        "+",
        ImmutableList.of());
  }

  /** Array stuff */
  public static SvLibFunctionDeclaration arraySelect(
      SvLibSmtLibType indexType, SvLibSmtLibType elementType) {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(new SvLibSmtLibArrayType(indexType, elementType), indexType),
            elementType),
        "select",
        "select",
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration arrayStore(
      SvLibSmtLibType indexType, SvLibSmtLibType elementType) {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(
            ImmutableList.of(
                new SvLibSmtLibArrayType(indexType, elementType), indexType, elementType),
            new SvLibSmtLibArrayType(indexType, elementType)),
        "store",
        "store",
        ImmutableList.of());
  }
}
