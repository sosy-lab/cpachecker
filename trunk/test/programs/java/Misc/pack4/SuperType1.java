// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack4;

public class SuperType1 {

  public int num = 0;
  public SuperSuper sup;

  public SuperType1() {
    sup = new SuperSuper();
  }

  public void method() {
    num = 5;
  }

}
