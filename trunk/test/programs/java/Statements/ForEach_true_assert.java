// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class ForEach_true_assert {

  public static void main(String[] args) {
    int[] numberArray = { 1, 2, 3, 4, 5 };
    int counter = 0;

    for (int currentNumber : numberArray) {
      assert numberArray[counter] == currentNumber;
      counter = counter + 1;
    }
  }
}
