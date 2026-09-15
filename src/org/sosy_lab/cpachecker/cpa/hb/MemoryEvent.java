// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

record MemoryEvent(
    MemoryEventType type,
    CVariableDeclaration var,
    Integer thread,
    Integer eid,
    MemoryEvent parent) {}
