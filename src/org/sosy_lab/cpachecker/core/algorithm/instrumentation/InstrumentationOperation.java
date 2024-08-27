// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableList;

/**
 * Class for operations defined on the transitions of instrumentation automaton. Should not be used
 * outside the Sequentialization operator !
 * The operation can be any valid C expression. It is possible to insert the variables matched from
 * the pattern by special variables x1, x2, ..., xn. All these variables are after match
 * replaced by first, second, third, ..., n-th mathced variable from InstrumentationPattern.
 */
public class InstrumentationOperation {
  private String operation;

  public InstrumentationOperation(String pOperation) {
    operation = pOperation;
  }

  public String InsertVariablesInsideOperation(ImmutableList<String> pVariables) {
    String resultingOperation = operation;
    for (int i = 0; i < pVariables.size(); i++) {
      resultingOperation = resultingOperation.replace("x" + (i + 1), pVariables.get(i));
    }
    if (resultingOperation.contains("x" + (pVariables.size() + 1))) {
      return "";
    }
    return resultingOperation;
  }
}
