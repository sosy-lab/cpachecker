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

/**
 * Instances represent some property that CPAchecker should check and are part of our {@link
 * Specification}.
 */
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

  /**
   * Represents an LTL property as used by SV-COMP except for the well-known ones that are
   * represented by {@link CommonVerificationProperty}.
   */
  public class OtherLtlProperty implements Property {
    private final String representation;

    OtherLtlProperty(String pRepresentation) {
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
      if (!(pObj instanceof OtherLtlProperty)) {
        return false;
      }

      OtherLtlProperty other = (OtherLtlProperty) pObj;
      return representation.equals(other.representation);
    }

    @Override
    public int hashCode() {
      return representation.hashCode();
    }
  }

  /** Represents the few commonly used hard-coded verification property used by SV-COMP. */
  public enum CommonVerificationProperty implements Property {
    REACHABILITY_LABEL("G ! label(ERROR)"),

    REACHABILITY("G ! call(__VERIFIER_error())"),

    REACHABILITY_ERROR("G ! call(reach_error())"),

    VALID_FREE("G valid-free"),

    VALID_DEREF("G valid-deref"),

    VALID_MEMTRACK("G valid-memtrack"),

    VALID_MEMCLEANUP("G valid-memcleanup"),

    OVERFLOW("G ! overflow"),

    DATA_RACE("G ! data-race"),

    DEADLOCK("G ! deadlock"),

    TERMINATION("F end"),

    ASSERT("G assert"),
    ;

    private final String representation;

    CommonVerificationProperty(String pRepresentation) {
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

  /** Represents the few commonly used hard-coded test properties used by Test-Comp. */
  public enum CommonCoverageProperty implements Property {
    COVERAGE_BRANCH("COVER EDGES(@DECISIONEDGE)"),

    COVERAGE_CONDITION("COVER EDGES(@CONDITIONEDGE)"),

    COVERAGE_STATEMENT("COVER EDGES(@BASICBLOCKENTRY)"),

    COVERAGE_ERROR("COVER EDGES(@CALL(__VERIFIER_error))"),
    ;

    private final String representation;

    CommonCoverageProperty(String pRepresentation) {
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

  /** Represents a property for covering all calls to a certain function as used by Test-Comp. */
  public static class CoverFunctionCallProperty implements Property {

    private static final Pattern COVERAGE_FUNCTION_PATTERN =
        Pattern.compile(
            "COVER\\( init\\(("
                + CFACreator.VALID_C_FUNCTION_NAME_PATTERN
                + ")\\(\\)\\), FQL\\(COVER EDGES\\(@CALL\\((.+)\\)\\)\\) \\)");

    private final String funName;

    CoverFunctionCallProperty(final String pFunctionName) {
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
      if (!(pObj instanceof CoverFunctionCallProperty)) {
        return false;
      }

      CoverFunctionCallProperty other = (CoverFunctionCallProperty) pObj;
      return funName.equals(other.funName);
    }

    @Override
    public int hashCode() {
      return funName.hashCode();
    }

    static Property getProperty(final String pRawProperty) {
      Matcher matcher = COVERAGE_FUNCTION_PATTERN.matcher(pRawProperty);

      if (matcher.matches() && matcher.groupCount() == 2) {
        return new CoverFunctionCallProperty(matcher.group(2).trim());
      }

      return null;
    }
  }
}
