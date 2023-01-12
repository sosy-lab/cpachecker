// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.translators;

public class TranslatorsUtils {

  private TranslatorsUtils() {}

  public static String getVarLessOrEqualValRequirement(final String pVar, final Number pVal) {
    StringBuilder sb = new StringBuilder();
    sb.append("(<= ");
    sb.append(pVar);
    sb.append(" ");
    sb.append(pVal.longValue());
    sb.append(")");
    return sb.toString();
  }

  public static String getVarGreaterOrEqualValRequirement(final String pVar, final Number pVal) {
    StringBuilder sb = new StringBuilder();
    sb.append("(>= ");
    sb.append(pVar);
    sb.append(" ");
    sb.append(pVal.longValue());
    sb.append(")");
    return sb.toString();
  }

  public static String getVarInBoundsRequirement(
      final String pVar, final Number pLow, final Number pHigh) {
    StringBuilder sb = new StringBuilder();
    sb.append("(and ");
    sb.append(getVarGreaterOrEqualValRequirement(pVar, pLow));
    sb.append(" ");
    sb.append(getVarLessOrEqualValRequirement(pVar, pHigh));
    sb.append(")");
    return sb.toString();
  }
}
