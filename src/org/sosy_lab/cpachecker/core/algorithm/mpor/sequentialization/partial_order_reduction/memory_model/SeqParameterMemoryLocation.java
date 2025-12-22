// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public final class SeqParameterMemoryLocation extends SeqMemoryLocation {

  /** We convert all {@link CParameterDeclaration}s to {@link CVariableDeclaration}s. */
  private final CVariableDeclaration declaration;

  /**
   * The arg index of this parameter memory location, starting at {@code 0}. A single {@link
   * CParameterDeclaration} may link to multiple memory locations in the same call context (cf.
   * variadic functions).
   */
  final int argumentIndex;

  private SeqParameterMemoryLocation(
      MPOROptions pOptions,
      // parameters always have a call context -> don't use Optional<>
      CFAEdgeForThread pCallContext,
      CVariableDeclaration pDeclaration,
      Optional<CCompositeTypeMemberDeclaration> pFieldMember,
      int pArgumentIndex) {

    super(pOptions, Optional.of(pCallContext), pFieldMember);
    checkNotNull(pDeclaration);
    declaration = pDeclaration;
    argumentIndex = pArgumentIndex;
  }

  public static SeqParameterMemoryLocation of(
      MPOROptions pOptions,
      CFAEdgeForThread pCallContext,
      CVariableDeclaration pDeclaration,
      int pArgumentIndex) {
    return new SeqParameterMemoryLocation(
        pOptions, pCallContext, pDeclaration, Optional.empty(), pArgumentIndex);
  }

  public static SeqParameterMemoryLocation of(
      MPOROptions pOptions,
      CFAEdgeForThread pCallContext,
      CVariableDeclaration pDeclaration,
      CCompositeTypeMemberDeclaration pFieldMember,
      int pArgumentIndex) {
    return new SeqParameterMemoryLocation(
        pOptions, pCallContext, pDeclaration, Optional.of(pFieldMember), pArgumentIndex);
  }

  @Override
  public CVariableDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), declaration, argumentIndex);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof SeqParameterMemoryLocation other
        && super.equals(other)
        && declaration.equals(other.declaration)
        && argumentIndex == other.argumentIndex;
  }
}
