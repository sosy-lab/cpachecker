// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack3;

public class RTTBaExample1_true_assert {


  public static void main(
      String[] args) {

    SubType1 obj2 = new SubSubType1();

    assert obj2 instanceof SubSubType1;

  }

}
