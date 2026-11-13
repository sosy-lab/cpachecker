// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.bdd;

import java.io.Serial;

/** Thrown if the BDD library cannot create more variables. */
public final class BDDVariableLimitException extends RuntimeException {

  @Serial private static final long serialVersionUID = 4529178815374925386L;

  BDDVariableLimitException(int pLimit) {
    super("The BDD library supports at most " + pLimit + " variables");
  }
}
