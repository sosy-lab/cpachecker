// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

class A extends B {}

class B extends Throwable {}

public class Main {
  public static void main(String[] args) throws B {
    try {
      final A a = new A();
      throw a;
    } catch (IndexOutOfBoundsException e) {
      assert false;
    } catch (B b) {
      System.out.println("Caught B");
      assert true;
    } catch (RuntimeException e) {
      System.out.println("Caught " + e);
      assert false;
    }
    assert true;
  }
}
