// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;

public interface Property {

  boolean isCoverage();

  boolean isVerification();

  /** Return a representation of this property in an unspecified format. */
  @Override
  String toString();

  /**
   * Return a full representation as used by SV-COMP or Test-Comp, e.g., including the entry point.
   */
  String toFullString(String entryPoint);

  /**
   * Return a full representation as used by SV-COMP or Test-Comp, e.g., including the entry point,
   * which is taken from the given CFA.
   */
  default String toFullString(CFA cfa) {
    // Make sure to use orig name in case the function was renamed.
    return toFullString(cfa.getMainFunction().getFunctionDefinition().getOrigName());
  }

  public class OtherVerificationProperty implements Property {
    private final String representation;

    public OtherVerificationProperty(String pRepresentation) {
      representation = checkNotNull(pRepresentation);
    }

    @Override
    public boolean isCoverage() {
      return false;
    }

    @Override
    public boolean isVerification() {
      return true;
    }

    @Override
    public String toString() {
      return representation;
    }

    @Override
    public String toFullString(String pEntryPoint) {
      return String.format("CHECK( init(%s()), LTL(%s) )", pEntryPoint, representation);
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof OtherVerificationProperty)) {
        return false;
      }

      OtherVerificationProperty other = (OtherVerificationProperty) pObj;
      return representation.equals(other.representation);
    }

    @Override
    public int hashCode() {
      return representation.hashCode();
    }
  }

  public enum CommonPropertyType implements Property {
    REACHABILITY_LABEL("G ! label(ERROR)"),

    REACHABILITY("G ! call(__VERIFIER_error())"),

    REACHABILITY_ERROR("G ! call(reach_error())"),

    VALID_FREE("G valid-free"),

    VALID_DEREF("G valid-deref"),

    VALID_MEMTRACK("G valid-memtrack"),

    VALID_MEMCLEANUP("G valid-memcleanup"),

    OVERFLOW("G ! overflow"),

    DEADLOCK("G ! deadlock"),

    TERMINATION("F end"),

    ASSERT("G assert"),
    ;

    private final String representation;

    CommonPropertyType(String pRepresentation) {
      representation = pRepresentation;
    }

    @Override
    public boolean isCoverage() {
      return false;
    }

    @Override
    public boolean isVerification() {
      return true;
    }

    @Override
    public String toString() {
      return representation;
    }

    @Override
    public String toFullString(String pEntryPoint) {
      return String.format("CHECK( init(%s()), LTL(%s) )", pEntryPoint, representation);
    }
  }

  public enum CommonCoverageType implements Property {
    COVERAGE_BRANCH("COVER EDGES(@DECISIONEDGE)"),

    COVERAGE_CONDITION("COVER EDGES(@CONDITIONEDGE)"),

    COVERAGE_STATEMENT("COVER EDGES(@BASICBLOCKENTRY)"),

    COVERAGE_ERROR("COVER EDGES(@CALL(__VERIFIER_error))"),
    ;

    private final String representation;

    CommonCoverageType(String pRepresentation) {
      representation = pRepresentation;
    }

    @Override
    public boolean isCoverage() {
      return true;
    }

    @Override
    public boolean isVerification() {
      return false;
    }

    @Override
    public String toString() {
      return representation;
    }

    @Override
    public String toFullString(String pEntryPoint) {
      return String.format("COVER( init(%s()), FQL(%s) )", pEntryPoint, representation);
    }
  }

  public static class CoverFunction implements Property {

    private static final Pattern COVERAGE_FUNCTION_PATTERN =
        Pattern.compile(
            "COVER\\( init\\(("
                + CFACreator.VALID_C_FUNCTION_NAME_PATTERN
                + ")\\(\\)\\), FQL\\(COVER EDGES\\(@CALL\\((.+)\\)\\)\\) \\)");

    private final String funName;

    public CoverFunction(final String pFunctionName) {
      funName = checkNotNull(pFunctionName);
    }

    @Override
    public boolean isCoverage() {
      return true;
    }

    @Override
    public boolean isVerification() {
      return false;
    }

    public String getCoverFunction() {
      return funName;
    }

    @Override
    public String toString() {
      return "COVER EDGES(@CALL(" + funName + "))";
    }

    @Override
    public String toFullString(String pEntryPoint) {
      return String.format("COVER( init(%s()), FQL(%s) )", pEntryPoint, toString());
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof CoverFunction)) {
        return false;
      }

      CoverFunction other = (CoverFunction) pObj;
      return funName.equals(other.funName);
    }

    @Override
    public int hashCode() {
      return funName.hashCode();
    }

    public static Property getProperty(final String pRawProperty) {
      Matcher matcher = COVERAGE_FUNCTION_PATTERN.matcher(pRawProperty);

      if (matcher.matches() && matcher.groupCount() == 2) {
        return new CoverFunction(matcher.group(2).trim());
      }

      return null;
    }
  }
}
