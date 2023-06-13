// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class CastMixedExpression_true_assert {

  public static void main(String[] args) {
    double a = 1.000001;
    long b = 1000;

    assert b > a; // always true
  }
}
