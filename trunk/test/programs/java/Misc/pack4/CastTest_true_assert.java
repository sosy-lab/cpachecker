// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack4;

public class CastTest_true_assert {


  public static void main(String[] args) {

    SuperType1 sub = new SubType1_true_assert();

    SubType1_true_assert sub2 = (SubType1_true_assert) sub;

    sub.num = 2; // accesses SuperType1.num

    sub2.num = 1; // accesses SubType1.num

    assert sub.num == 2; // always true

    assert ((SubType1_true_assert) sub).num == 1; // always true

    ((SubType1_true_assert) sub).method2(); // sub2.num = 3

    assert sub2.num == 3; // always true
  }
}
