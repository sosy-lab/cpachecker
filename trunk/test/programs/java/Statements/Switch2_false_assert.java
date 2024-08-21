// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Switch2_false_assert {

  public static void main(String[] args) {
    int n1;

    n1 = 2;

    switch (n1) {
      case 1:
        assert (false); // not reached
        int n6;
        // falls through
      case 2:
        assert (true); // always true
        int n7;
        // falls through
      case 3:
        assert (false); // always false!
        int n8;
    }

    n1 = 2;
  }
}
