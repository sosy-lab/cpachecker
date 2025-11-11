// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.builder;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;

public class SmtLibTheoryDeclarations {

  /* Integer stuff */

  public static SvLibVariableDeclaration INT_EQUALITY =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
              SvLibSmtLibType.BOOL),
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
              ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
              SvLibSmtLibType.BOOL),
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
              ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
              SvLibSmtLibType.BOOL),
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
              ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
              SvLibSmtLibType.BOOL),
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
              ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
              SvLibSmtLibType.BOOL),
          ">=",
          ">=",
          ">=");

  public static SvLibVariableDeclaration INT_MINUS =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
              SvLibSmtLibType.INT),
          "-",
          "-",
          "-");

  public static SvLibVariableDeclaration INT_MULTIPLICATION =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
              SvLibSmtLibType.INT),
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
            ImmutableList.copyOf(Collections.nCopies(amountArguments, SvLibSmtLibType.INT)),
            SvLibSmtLibType.INT),
        "+",
        "+",
        "+");
  }

  /* Non-Linear Integer stuff */

  public static SvLibVariableDeclaration INT_MOD =
      new SvLibVariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new SvLibFunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(SvLibSmtLibType.INT, SvLibSmtLibType.INT),
              SvLibSmtLibType.INT),
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
              FileLocation.DUMMY, ImmutableList.of(SvLibSmtLibType.BOOL), SvLibSmtLibType.BOOL),
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
            ImmutableList.copyOf(Collections.nCopies(amountArguments, SvLibSmtLibType.BOOL)),
            SvLibSmtLibType.BOOL),
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
            ImmutableList.copyOf(Collections.nCopies(amountArguments, SvLibSmtLibType.BOOL)),
            SvLibSmtLibType.BOOL),
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
            ImmutableList.copyOf(Collections.nCopies(amountArguments, SvLibSmtLibType.BOOL)),
            SvLibSmtLibType.BOOL),
        "=>",
        "=>",
        "=>");
  }
}
