// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack3;

public class RTTTest1_true_assert {


  public static void main(
      String[] args) {

    SubType1 obj1 = new SubType1();
    SubType1 obj2 = new SubSubType1();
    Interface2 obj3 = new SubType2(obj1);
    Interface2 obj4 = new SubType2(obj2);

    assert !obj3.objectInstanceOf(); // always true
    assert obj4.objectInstanceOf(); // always true
  }
}
