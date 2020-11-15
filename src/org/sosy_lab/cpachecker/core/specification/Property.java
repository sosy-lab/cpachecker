// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

public interface Property {

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
    ;

    private final String representation;

    CommonPropertyType(String pRepresentation) {
      representation = pRepresentation;
    }

    @Override
    public String toString() {
      return representation;
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
  }
}
