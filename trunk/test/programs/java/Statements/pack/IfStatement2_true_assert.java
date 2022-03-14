// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class IfStatement2_true_assert {

  public static void main(
      String[] args) {

    int n1 = 1 + 1 * 4; // n1 = 5
    int n2 = 2 + 2 * 6; // n2 = 14
    boolean b1 = n1 == n2; // b1 = false

    if (b1) { // never entered
      assert (false);
    } else {

    }
  }
}



