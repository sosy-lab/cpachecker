// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class Declaration {

  static int staticFieldVariable = 1;
  private Declaration fieldVariable = null;

  public Declaration() {
    boolean localVariable = false;
  }

  public static void main(String args[]) {
    Declaration localVariable = new Declaration();
  }
}
