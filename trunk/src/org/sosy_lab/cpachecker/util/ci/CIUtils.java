// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci;

public class CIUtils {

  private CIUtils() {}

  public static String getSMTName(final String varName) {
    if (varName.contains(":")) {
      return "|" + varName + "|";
    }
    return varName;
  }

  public static String getSMTNameWithIndex(final String varName) {
    return getSMTName(varName + "@1");
  }
}
