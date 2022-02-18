// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public interface Slice {

  CFA getOriginalCfa();

  ImmutableCollection<CFAEdge> getSlicingCriteria();

  ImmutableSet<CFAEdge> getRelevantEdges();

  ImmutableSet<ASimpleDeclaration> getRelevantDeclarations();

  boolean isRelevantDef(CFAEdge pEdge, MemoryLocation pMemoryLocation);

  boolean isRelevantUse(CFAEdge pEdge, MemoryLocation pMemoryLocation);
}
