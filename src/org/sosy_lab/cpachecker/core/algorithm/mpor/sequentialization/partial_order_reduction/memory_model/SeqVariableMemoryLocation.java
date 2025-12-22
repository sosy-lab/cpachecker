// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public class SeqVariableMemoryLocation extends SeqMemoryLocation {

  private final CVariableDeclaration declaration;

  private SeqVariableMemoryLocation(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CVariableDeclaration pDeclaration,
      Optional<CCompositeTypeMemberDeclaration> pFieldMember) {

    super(pOptions, pCallContext, pFieldMember);
    declaration = pDeclaration;
  }

  public static SeqVariableMemoryLocation of(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CVariableDeclaration pDeclaration,
      CCompositeTypeMemberDeclaration pFieldMember) {
    return new SeqVariableMemoryLocation(
        pOptions, pCallContext, pDeclaration, Optional.of(pFieldMember));
  }

  public static SeqVariableMemoryLocation of(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CVariableDeclaration pDeclaration) {
    return new SeqVariableMemoryLocation(pOptions, pCallContext, pDeclaration, Optional.empty());
  }

  @Override
  public CVariableDeclaration getDeclaration() {
    return declaration;
  }
}
