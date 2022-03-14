// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class BigDouble_true_assert {

  public static void main(String[] args) {
    double d1 = 1.7976931348623157E308;
    double d2 = -1.7976931348623157E308;
    double e1 = 4.9E-324;
    double e2 = -4.9E-324;

    assert d1 > d2; // always true
    assert e1 > e2; // always true
  }
}
