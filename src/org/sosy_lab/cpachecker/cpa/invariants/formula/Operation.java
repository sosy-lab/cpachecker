// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

public enum Operation {
  LOGICAL_NOT("!.ln"),
  LOGICAL_AND("&&.la"),
  EQUAL("=.eq"),
  LESS_THAN("<.lt"),
  ADD("+.add"),
  MULTIPLY("*.mult"),
  DIVIDE("/.div"),
  MODULO("%.mod"),
  BINARY_AND("&.bA"),
  BINARY_OR("|.bo"),
  BINARY_XOR("^.bxo"),
  SHIFT_LEFT("<<.sl"),
  SHIFT_RIGHT(">>.sr"),
  EXCLUSION("\\.ex"),
  UNION("U.uni"),
  BINARY_NOT("~.bn"),
  CAST(".cast"),
  IF("?.if"),
  ELSE(":.el");

  private final String representation;

  Operation(String representation) {
    this.representation = representation;
  }

  public String getRepresentation() {
    return this.representation;
  }

  public static Operation fromString(String representation) {
    for (Operation op : Operation.values()) {
      if (op.getRepresentation().equals(representation)) {
        return op;
      }
    }
    throw new IllegalArgumentException("Unknown operation: " + representation);
  }
}
