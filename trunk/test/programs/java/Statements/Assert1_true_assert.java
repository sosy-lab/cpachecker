// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Assert1_true_assert {

  public static void main(String[] args) {
    boolean b1 = false;

    int n1 = 0;
    int n2 = 0;
    int n3 = 0;

    if ((n1 == n2 && n2 == n3) || n3 == n1) {
      b1 = true;
    }

    assert b1 : "Wrong"; // b1 always true
  }
}
