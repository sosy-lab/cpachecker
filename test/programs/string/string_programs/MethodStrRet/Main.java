// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Main {

  static String Foo(String s) {
    return s;
  }

  public static void main(String[] args) {
    String iron = "man";
    String he = Foo(iron);
    iron = Foo("captain");
    assert iron.equals("captain");
  }
}
