// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class UnaryNumberOperators_true_assert {

  public static void main(String[] args) {

    checkPrefixIncrement();
    checkPostfixIncrement();

    checkPrefixDecrement();
    checkPostfixDecrement();
  }

  private static void checkPrefixIncrement() {
    int n = 1;

    assert n == 1;
    assert ++n == 2;
    assert n == 2;
  }

  private static void checkPostfixIncrement() {
    int n = 1;
    
    assert n == 1;
    assert n++ == 1;
    assert n == 2;
  }

  private static void checkPrefixDecrement() {
    int n = 1;
    
    assert n == 1;
    assert --n == 0;
    assert n == 0;
  }

  private static void checkPostfixDecrement() {
    int n = 1;
    
    assert n == 1;
    assert n-- == 1;
    assert n == 0;
  }
}
