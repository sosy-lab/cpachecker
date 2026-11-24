// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record ArrayCopy(
    CFANode loopHead,
    AcslTerm sourceArray,
    AcslTerm destinationArray,
    AcslIdTerm index,
    AcslTerm lowerbound)
    implements AcslInvariantProvider {

  @SuppressWarnings("unused")
  @Override
  public AcslPredicate toInvariant(CProgramScope pScope, FileLocation pLoc, LogManager pLogger) {
    return null;
  }
}
