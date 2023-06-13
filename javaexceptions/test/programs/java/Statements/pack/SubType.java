// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack;

public class SubType extends SuperType {
  int subField;

  public SubType(int superParam, int subParam) {
    super(superParam);
    int startSubConstructor;
    subField = subParam;
    int endSubConstructor;
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    SuperType subType = new SubType(1, 1);
  }
}
