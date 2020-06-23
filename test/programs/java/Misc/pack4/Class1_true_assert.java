// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack4;

public class Class1_true_assert {

  public static void main(String[] args) {

    boolean b = true;

    if (b) {

    } else
      assert b; // not reached

    for(int i = 0; i < 10 || b && !b; ++i) { // condition same as (i < 10) || (b && !b)
      for(int j = 0 ; j < 10 && b; ++j) {
        assert b; // always true
      }
    }

    for(int i = 2; i < 3; ++i){
      int j = 4;
    }
  }
}

