// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class LabeledAnweisung {

  public static void main(String[] args) {
    boolean breakCondition = true;
    int startLabel;

    Label: {
      int startLabelBlock;

      if (breakCondition) {
        break Label;
      }

      int stopLabelBlock;
    }

    int endLabel;
  }
}
