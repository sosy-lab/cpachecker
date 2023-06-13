// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack5;

public class CallTests_true_assert {

  public static void main(String[] args) {

  SubType2 su = new SubType2(new SubType1(3, 5, 7, 8)); // no assert violated

   int t = su.test(); // no assert violated

   assert t == 368; // always true
  }

}
