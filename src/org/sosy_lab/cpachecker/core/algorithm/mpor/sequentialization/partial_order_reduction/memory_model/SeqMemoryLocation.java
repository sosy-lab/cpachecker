// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public record SeqMemoryLocation(
    Optional<CFAEdgeForThread> callContext,
    CVariableDeclaration declaration,
    Optional<CCompositeTypeMemberDeclaration> fieldMember) {

  public static SeqMemoryLocation of(
      Optional<CFAEdgeForThread> pCallContext, CVariableDeclaration pDeclaration) {

    return new SeqMemoryLocation(pCallContext, pDeclaration, Optional.empty());
  }

  public static SeqMemoryLocation of(
      Optional<CFAEdgeForThread> pCallContext,
      CVariableDeclaration pDeclaration,
      CCompositeTypeMemberDeclaration pFieldMember) {

    return new SeqMemoryLocation(pCallContext, pDeclaration, Optional.of(pFieldMember));
  }

  public String getName() {
    StringBuilder name = new StringBuilder();

    // only local variables are prefixed with a thread id
    if (!declaration.isGlobal()) {
      // use call context if possible, otherwise use 0 (only main() declarations have no context)
      name.append("T")
          .append(callContext.isPresent() ? callContext.orElseThrow().threadId : 0)
          .append("_");
    }

    name.append(declaration.getName());

    if (fieldMember.isPresent()) {
      name.append("_").append(fieldMember.orElseThrow().getName());
    }

    return name.toString();
  }

  public boolean isFieldOwnerPointerType() {
    if (fieldMember.isPresent()) {
      return declaration.getType() instanceof CPointerType;
    }
    return false;
  }

  public SeqMemoryLocation getFieldOwnerMemoryLocation() {
    checkArgument(
        fieldMember.isPresent(), "cannot get field owner MemoryLocation, field member is empty");
    // just return the declaration of the field owner, without any field member
    return SeqMemoryLocation.of(callContext, declaration);
  }

  @Override
  public int hashCode() {
    // consider call context only for non-global variables
    return Objects.hash(declaration.isGlobal() ? null : callContext, fieldMember);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther
            instanceof
            SeqMemoryLocation(
                Optional<CFAEdgeForThread> pCallContext,
                CVariableDeclaration pDeclaration,
                Optional<CCompositeTypeMemberDeclaration> pFieldMember)
        // consider call context only for non-global variables
        && (declaration.isGlobal() || callContext.equals(pCallContext))
        && fieldMember.equals(pFieldMember)
        && declaration.equals(pDeclaration);
  }

  @Override
  public String toString() {
    if (fieldMember.isEmpty()) {
      return declaration.toASTString();
    }
    return declaration.toASTString() + " -> " + fieldMember.orElseThrow().toASTString();
  }
}
