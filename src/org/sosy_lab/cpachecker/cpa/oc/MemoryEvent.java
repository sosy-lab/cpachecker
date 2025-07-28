// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public record MemoryEvent(
    int id,
    CVariableDeclaration declaration,
    CVariableDeclaration cssaDeclaration,
    Optional<PathFormula> guard,
    EventType eventType
) {
  public MemoryEvent withGuard(PathFormula pf) {
    return new MemoryEvent(id, declaration, cssaDeclaration, Optional.of(pf), eventType);
  }
}
