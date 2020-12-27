// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci;

import com.google.common.base.Function;

public class CIUtils {

  private CIUtils(){}

  public static String getSMTName(final String varName) {
    if (varName.contains(":")) {
      return "|" + varName + "|";
    }
    return varName;
  }

  public static final Function<String, String> GET_SMTNAME = new Function<>() {
    @Override
    public String apply(final String varName) {
      return getSMTName(varName);
    }
  };

  public static final Function<String, String> GET_SMTNAME_WITH_INDEX = new Function<>() {
    @Override
    public String apply(final String varName) {
      return getSMTName(varName+"@1");
    }
  };

}
