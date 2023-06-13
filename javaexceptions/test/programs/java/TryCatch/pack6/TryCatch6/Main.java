// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Main {
  public static void main(String[] args) {

    int[] intArray = new int[] {1, 2, 3};

    try {
      System.out.println(intArray[3]);
      System.out.println("No Exception thrown");
      boolean bool = true;
      if (bool) {
        return;
      } else {
        System.out.println("Test");
      }
    } catch (RuntimeException cnse) {
      throw new RuntimeException("A CloneNotSupportedException was thrown: " + cnse.getMessage());
    }
  }
}
