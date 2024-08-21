// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class MethodCall {

  public static void main(String[] args) {

    int n = 0;

    int startMethodInvocation;
    n = teileDurch2(n);
    int endMethodInvocation;

  }

  public static int teileDurch2(int op) {
    int startMethod;
    return op / 2;
  }

}
