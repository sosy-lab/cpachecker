// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;

public class CheckTypesOfStringsUtil {

  private CheckTypesOfStringsUtil() {}

  public static boolean isLong(String s) {
    return Pattern.matches("-?\\d+", s);
  }

  public static boolean isSIGN(String s) {
    try {
      SIGN.valueOf(s);
    } catch (IllegalArgumentException ex) {
      return false;
    }
    return true;
  }
}
