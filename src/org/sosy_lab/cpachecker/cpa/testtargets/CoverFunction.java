// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.specification.Property;

public class CoverFunction implements Property {

  private static final Pattern COVERAGE_FUNCTION_PATTERN =
      Pattern.compile(
          "COVER\\( init\\(("
              + CFACreator.VALID_C_FUNCTION_NAME_PATTERN
              + ")\\(\\)\\), FQL\\(COVER EDGES\\(@CALL\\((.+)\\)\\)\\) \\)");

  private final String funName;

  public CoverFunction(final String pFunctionName) {
    funName = pFunctionName;
  }

  public String getCoverFunction() {
    return funName;
  }

  @Override
  public String toString() {
    return "COVER EDGES(@CALL(" + funName + "))";
  }

  public static Property getProperty(final String pRawProperty) {
    Matcher matcher = COVERAGE_FUNCTION_PATTERN.matcher(pRawProperty);

    if (matcher.matches() && matcher.groupCount() == 2) {
      return new CoverFunction(matcher.group(2).trim());
    }

    return null;
  }

}
