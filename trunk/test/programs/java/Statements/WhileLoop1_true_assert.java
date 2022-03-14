// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class WhileLoop1_true_assert {

  public static void main(String[] args) {
    int n1;

    n1 = 0;
    while (n1 < 10) {
      n1 = n1 + 1;

      if (n1 > 10) // never entered, n1 <= 10 always
      {
        assert (false);
      }
    }
  }
}
