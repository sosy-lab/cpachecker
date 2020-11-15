// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Loop6_true_assert {

  public static void main(String[] args) {
    int c;

    for (c = 0; c < 10; c++) {

    }

    assert (c == 10); // always true
  }
}
