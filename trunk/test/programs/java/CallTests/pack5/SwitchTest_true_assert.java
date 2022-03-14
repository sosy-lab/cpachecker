// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack5;

public class SwitchTest_true_assert {

  public static void main(String[] args) {
    WLE s = WLE.JA;

    switch(s){
    case JA:
      assert true; // always true
      break;
    case NEIN:
      assert false; // not reached
    }
  }

  public enum WLE {
    JA,
    NEIN;
  }
}
