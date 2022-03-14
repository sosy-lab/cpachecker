// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class SwitchAnweisung {

  public static void main(String[] args) {

    int condition = 1;
    int startSwitch;
    switch (condition) {
      case 0:
        int casePath0 = 0;
        break;
      case 1:
        int casePath1 = 1;
      case 2:
        int casePath2 = 2;
        break;
      default:
        int defaultPath = 3;
    }
    int endSwitch = 4;
  }
}
