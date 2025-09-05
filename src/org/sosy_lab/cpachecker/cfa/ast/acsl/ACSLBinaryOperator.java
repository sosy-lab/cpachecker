// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public enum ACSLBinaryOperator {
  AND("&&"),
  OR("||"),
  IMP("==>"),
  EQV("<==>"),
  XOR("^^"),
  EQ("=="),
  NEQ("!="),
  LEQ("<="),
  GEQ(">="),
  LT("<"),
  GT(">"),
  BAND("&"),
  BOR("|"),
  BIMP("-->"),
  BEQV("<-->"),
  BXOR("^"),
  PLUS("+"),
  MINUS("-"),
  TIMES("*"),
  DIVIDE("/"),
  MOD("%"),
  LSHIFT("<<"),
  RSHIFT(">>");

  private final String operator;

  ACSLBinaryOperator(String s) {
    operator = s;
  }

  public static boolean isComparisonOperator(ACSLBinaryOperator op) {
    return switch (op) {
      case EQ, NEQ, LEQ, GEQ, LT, GT -> true;
      default -> false;
    };
  }

  public static boolean isLogicalOperator(ACSLBinaryOperator op) {
    return switch (op) {
      case AND, OR, IMP, EQV, XOR -> true;
      default -> false;
    };
  }

  public static boolean isBitwiseOperator(ACSLBinaryOperator op) {
    return switch (op) {
      case BAND, BOR, BIMP, BEQV, BXOR -> true;
      default -> false;
    };
  }

  public static boolean isArithmeticOperator(ACSLBinaryOperator op) {
    return switch (op) {
      case PLUS, MINUS, TIMES, DIVIDE, MOD, LSHIFT, RSHIFT -> true;
      default -> false;
    };
  }

  public static boolean isCommutative(ACSLBinaryOperator op) {
    return switch (op) {
      case EQ, NEQ, AND, OR, EQV, XOR, BAND, BOR, BEQV, BXOR, PLUS, TIMES -> true;
      default -> false;
    };
  }

  @Override
  public String toString() {
    return operator;
  }
}
