// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public enum BinaryOperator {
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

  BinaryOperator(String s) {
    operator = s;
  }

  public static boolean isComparisonOperator(BinaryOperator op) {
    switch (op) {
      case EQ:
      case NEQ:
      case LEQ:
      case GEQ:
      case LT:
      case GT:
        return true;
      default:
        return false;
    }
  }

  public static boolean isLogicalOperator(BinaryOperator op) {
    switch (op) {
      case AND:
      case OR:
      case IMP:
      case EQV:
      case XOR:
        return true;
      default:
        return false;
    }
  }

  public static boolean isBitwiseOperator(BinaryOperator op) {
    switch (op) {
      case BAND:
      case BOR:
      case BIMP:
      case BEQV:
      case BXOR:
        return true;
      default:
        return false;
    }
  }

  public static boolean isArithmeticOperator(BinaryOperator op) {
    switch (op) {
      case PLUS:
      case MINUS:
      case TIMES:
      case DIVIDE:
      case MOD:
      case LSHIFT:
      case RSHIFT:
        return true;
      default:
        return false;
    }
  }

  public static boolean isCommutative(BinaryOperator op) {
    switch (op) {
      case EQ:
      case NEQ:
      case AND:
      case OR:
      case EQV:
      case XOR:
      case BAND:
      case BOR:
      case BEQV:
      case BXOR:
      case PLUS:
      case TIMES:
        return true;
      default:
        return false;
    }
  }

  @Override
  public String toString() {
    return operator;
  }
}
