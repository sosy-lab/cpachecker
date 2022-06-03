// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

class A extends Throwable {}

public class Main {
  public static void main(String[] args) throws A {

    int[] intArray = new int[] {1, 2, 3};
    boolean throwException = true;
    try {
      if (throwException) {
        A a = new A();
        throw a;
      } else {
        System.out.println("No Exception thrown");
      }
      System.out.println("This statement is only reached after else clause");
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("Error message: " + e);
      System.out.println("caught exception");
    } catch (RuntimeException e) {
      System.out.println("Runtime Exception: " + e);
    }

    System.out.println("End");
  }
}
