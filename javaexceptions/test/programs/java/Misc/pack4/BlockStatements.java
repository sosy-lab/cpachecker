// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack4;

public class BlockStatements {

  public static void main(String[] args) {
    int[] a = new int[3];
    if (a.length == 0) {
      System.out.println("test");
      int b = 1;
      System.out.println(b);
    } else if (a.length == 0) {
      System.out.println("test2");
      int b = 2;
      System.out.println(b);
    } else {
      System.out.println("test3");
      int b = 3;
      System.out.println(b);
    }
  }
}

