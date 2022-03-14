// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack4;

public class TestContin_true_assert {

  /**
   * @param args
   */
  public static void main(String[] args) {

  int y = 0;
  int x = 0;
  int z = 0;

  label:  while(y < 2) {
    y++;
    x = 0;
    while(x < 2) {
    x++;

    if(x > 1) {
      continue label;
    }

    z++;
    }
  }

  System.out.println(y);


  assert z == 2; // 2 loops above, so z == 2 always true
  }
}
