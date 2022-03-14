// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Switch_true_assert {

  public static void main(String[] args) {
    int n1 = 10;
    int n2 = 0;

    switch (n1) {
      case 1:
        assert (false);
      case 5:
        assert (false);
      case 10:
        // this branch happens
        assert (true);
        // $FALL-THROUGH$
      case 12:
        n2 = 1;
        break;
      default:
        assert (false);
    }

    assert n2 == 1;
  }
}
