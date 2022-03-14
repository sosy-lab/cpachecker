// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class SuperClass {

  public static void main(String[] args) {
    boolean b = true;
    SuperClass obj;

    if (b) {
      obj = new SuperClass();
    } else {
      obj = new SubClass();
    }

    int startMethodInvocation;
    obj.method();
    int endMethodInvocation;
  }

  public void method() {
    int startMethod;
    int endMethod;
  }
}
