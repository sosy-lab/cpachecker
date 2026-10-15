// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record ArrayInitialization(
    CFANode loopHead,
    AcslTerm array,
    AcslIdTerm index,
    CExpression lowerbound,
    CExpression upperbound,
    AcslTerm value) {

  public AcslPredicate toInvariant() {
    // TODO
    return null;
  }
}

// represents the invariant \forall i in [lowerbound, upperbound): array[i] == value
