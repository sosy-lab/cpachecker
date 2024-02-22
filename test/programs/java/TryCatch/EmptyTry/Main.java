// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Main {
  private static void f() {
    throw new RuntimeException();
  }

  public static void main(String[] args) {
    try {
    } catch (RuntimeException e) {
      assert false;
    } finally {
      f();
    }
    assert false;
  }
}
