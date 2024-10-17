// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;

public class ConstraintReader {

  private ConstraintReader() {}

  public static Optional<Constraint> parseConstraint(final String pConstraint) {
    SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    // TODO
    return Optional.empty();
  }
}
