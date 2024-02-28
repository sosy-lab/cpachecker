// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

public class SMGAdditionalInfo {
  public enum Level {
    ERROR,
    WARNING,
    NOTE,
    INFO
  }

  private final String value;
  private final Level level;
  private final boolean hide;

  private SMGAdditionalInfo(String pValue, Level pLevel, boolean pHide) {
    value = pValue;
    level = pLevel;
    hide = pHide;
  }

  public static SMGAdditionalInfo of(String pValue, Level pLevel, boolean pHide) {
    return new SMGAdditionalInfo(pValue, pLevel, pHide);
  }

  public static SMGAdditionalInfo of(String pValue, Level pLevel) {
    return new SMGAdditionalInfo(pValue, pLevel, false);
  }

  @Override
  public String toString() {
    return "level=\"" + level.ordinal() + "\" hide=\"" + hide + "\" value=\"" + value + "\"";
  }
}
