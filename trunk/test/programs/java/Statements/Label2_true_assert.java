// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Label2_true_assert {


  public static void main(String[] args) {
    int n1;

    n1 = 10;

    L1: {
      L2: {
        L3: {

          break L2;
          assert false; // not reached
        }

        assert false; // not reached
      }

      L4: {
        break L4;
        assert false; // not reached
      }
    }
  }
}
