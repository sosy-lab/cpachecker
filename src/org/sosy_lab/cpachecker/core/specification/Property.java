// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import org.sosy_lab.cpachecker.cfa.CFA;

public interface Property {

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
      representation = pRepresentation;
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
    public String toString() {
      return representation;
    }

    @Override
    public String toFullString(String pEntryPoint) {
      return String.format("COVER( init(%s()), FQL(%s) )", pEntryPoint, representation);
    }
  }
}
