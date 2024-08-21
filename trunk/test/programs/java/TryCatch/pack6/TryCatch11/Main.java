// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*
 * Origin of the benchmark:
 *     license: 4-clause BSD (see /java/jbmc-regression/LICENSE)
 *     repo: https://github.com/diffblue/cbmc.git
 *     branch: develop
 *     directory: regression/cbmc-java/NullPointerException2
 * The benchmark was taken from the repo: 24 January 2018
 */
class A {
  int i;
}

public class Main {
  public static void main(String[] args) {
    A a = null;
    try {
      a.i = 0;
    } catch (NullPointerException exc) {
      assert false;
    }
  }
}
