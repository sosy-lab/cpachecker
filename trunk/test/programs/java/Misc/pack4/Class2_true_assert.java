// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack4;

public class Class2_true_assert {

  static boolean b = true;
  boolean b2 = true;


  public static void main(String[] args) {

  boolean b = true;
  boolean b2 = true;
  boolean b1 = true;
  boolean b3 = true;

  int n1 = 0;

  int b5 = 0;

  assert b : n1 = 3 + 7; // always true
  }
}
