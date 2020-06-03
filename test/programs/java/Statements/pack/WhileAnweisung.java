// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class WhileAnweisung {

  public static void main(String args[]) {
    boolean breakCondition = true;
    boolean condition = true;

    int startWhile;
    while (condition) {
      int startWhileBlock;
      int startLoop;

      if (breakCondition) {
        break;
      }
      int endWhileBlock;
    }

    int endWhile;
  }
}
