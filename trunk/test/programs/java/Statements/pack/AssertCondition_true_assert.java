// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class AssertCondition_true_assert {

  public static void main(String[] args) {
    boolean condition = true;
    int startAssert;

    assert condition;

    int endAssert;
  }
}
