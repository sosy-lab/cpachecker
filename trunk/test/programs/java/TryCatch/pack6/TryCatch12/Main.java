// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

class Main {
  public static void main(String[] args) {
    Object o = null;
    try {
      o.getName();
    } catch (Exception e) {
      assert true;
    }
  }
}
;
