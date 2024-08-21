// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class ConditionalExpression1_true_assert {

  public static void main(String[] args) {
    int n1;
    int n2;

    n1 = 9;
    n2 = 10;
    n1 = n1 == n2 ? n1 : n2; // n1 = n2

    assert n1 == n2 : "The Values are not equal"; // always true

  }
}
