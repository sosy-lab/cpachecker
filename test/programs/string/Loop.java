// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Loop {
  public static void main(String[] args) {
    String a = "foo"+ "";
    for (int i = 0; i < 20; i++) {
      a += "bar";
      }
    assert a.length() == 63;
    }
}
