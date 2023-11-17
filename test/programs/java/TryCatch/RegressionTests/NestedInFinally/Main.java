// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Main {
  private static int i = 0;
  private static Boolean enteredF = false;
  private static Boolean enteredG = false;

  private static void f() {
    throw new RuntimeException();
  }

  private static void g() {
    throw new ArrayIndexOutOfBoundsException();
  }

  public static void main(String[] args) {
    try {
      while (i < 10) {
        f();
        i++;
      }
    } catch (RuntimeException e) {
      enteredF = true;
    } finally {
      try {
        g();
      } catch (ArrayIndexOutOfBoundsException a) {
        enteredG = true;
      } finally {
        assert enteredG;
      }
      assert enteredF;
    }
  }
}
