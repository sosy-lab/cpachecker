// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class forAnweisung {

  public static void main(String[] args) {

    int condition = 5;
    int update = 0;
    boolean continueCondition = true;
    int startFor;

    for (int initializer; condition < 5; update = update + 1) {
      int startForBlock;

      if (continueCondition) {
        continue;
      }

      int endForBlock;
    }

    int endFor;
  }
}
