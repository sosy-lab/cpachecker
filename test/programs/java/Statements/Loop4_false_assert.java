// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Loop4_false_assert {

  /*
   * Will always throw an AssertionError, if assertions allowed
   */
  public static void main(String[] args) {
    int n1;

    n1 = 10;

    do {
      assert false; // happens always
    } while (false);

  }

}
