// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

class A extends RuntimeException {
  int i = 1;
};

class B extends A {};

public class Main {
  public static void main(String[] args) {
    int k = 6;
    try {
      if (k == 0) {
        A a = new A();
        throw a;
      } else {
        A b = new A();
        throw b;
      }

    } catch (B exc) {
      assert false;
    }
  }
}
