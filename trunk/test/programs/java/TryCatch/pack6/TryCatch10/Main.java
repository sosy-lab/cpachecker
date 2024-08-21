// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Main {
  public static void main(String[] args) {

    int[][] intArray = new int[3][2];

    try {
      final int i = intArray[3][1];
      System.out.println(i);
      System.out.println("No Exception thrown");
    } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("Error message: " + e);
      System.out.println("caught exception");
      assert true;
    } catch (RuntimeException e) {
      System.out.println("Runtime Exception: " + e);
      assert false;
    }
    finally{
      assert true;
      System.out.println("Finally Statement");
    }
    assert true;
    System.out.println("End");
  }
}
