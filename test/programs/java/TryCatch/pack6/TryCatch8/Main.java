// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

class A extends Throwable {}
;

public class Main {
  public static void main(String[] args) throws A {
    try {
      final A a = new A();
      throw a;
    } catch (IndexOutOfBoundsException e) {
      assert false;
    } catch (A a) {
      System.out.println("Caught " + a);
      assert true;
    } catch (RuntimeException e) {
      System.out.println("Caught " + e);
      assert false;
    }
    assert true;
  }
}
