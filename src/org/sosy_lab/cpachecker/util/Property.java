/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.util;

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
