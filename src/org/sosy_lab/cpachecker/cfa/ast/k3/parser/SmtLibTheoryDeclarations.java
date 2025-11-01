// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FunctionType;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SmtLibType;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;

public class SmtLibTheoryDeclarations {

  /* Integer stuff */

  public static K3VariableDeclaration INT_EQUALITY =
      new K3VariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new K3FunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(K3SmtLibType.INT, K3SmtLibType.INT),
              K3SmtLibType.BOOL),
          "=",
          "=",
          "=");

  public static K3VariableDeclaration INT_LESS_THAN =
      new K3VariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new K3FunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(K3SmtLibType.INT, K3SmtLibType.INT),
              K3SmtLibType.BOOL),
          "<",
          "<",
          "<");

  public static K3VariableDeclaration INT_LESS_EQUAL_THAN =
      new K3VariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new K3FunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(K3SmtLibType.INT, K3SmtLibType.INT),
              K3SmtLibType.BOOL),
          "<=",
          "<=",
          "<=");

  public static K3VariableDeclaration INT_MINUS =
      new K3VariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new K3FunctionType(
              FileLocation.DUMMY,
              ImmutableList.of(K3SmtLibType.INT, K3SmtLibType.INT),
              K3SmtLibType.INT),
          "-",
          "-",
          "-");

  public static K3VariableDeclaration intAddition(int amountArguments) {
    return new K3VariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new K3FunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(Collections.nCopies(amountArguments, K3SmtLibType.INT)),
            K3SmtLibType.INT),
        "+",
        "+",
        "+");
  }

  /* Boolean stuff */

  public static K3VariableDeclaration BOOL_NEGATION =
      new K3VariableDeclaration(
          FileLocation.DUMMY,
          true,
          true,
          new K3FunctionType(
              FileLocation.DUMMY, ImmutableList.of(K3SmtLibType.BOOL), K3SmtLibType.BOOL),
          "not",
          "not",
          "not");

  public static K3VariableDeclaration boolDisjunction(int amountArguments) {
    return new K3VariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new K3FunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(Collections.nCopies(amountArguments, K3SmtLibType.BOOL)),
            K3SmtLibType.BOOL),
        "or",
        "or",
        "or");
  }

  public static K3VariableDeclaration boolConjunction(int amountArguments) {
    return new K3VariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new K3FunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(Collections.nCopies(amountArguments, K3SmtLibType.BOOL)),
            K3SmtLibType.BOOL),
        "and",
        "and",
        "and");
  }

  public static K3VariableDeclaration boolImplication(int amountArguments) {
    return new K3VariableDeclaration(
        FileLocation.DUMMY,
        true,
        true,
        new K3FunctionType(
            FileLocation.DUMMY,
            ImmutableList.copyOf(Collections.nCopies(amountArguments, K3SmtLibType.BOOL)),
            K3SmtLibType.BOOL),
        "=>",
        "=>",
        "=>");
  }
}
