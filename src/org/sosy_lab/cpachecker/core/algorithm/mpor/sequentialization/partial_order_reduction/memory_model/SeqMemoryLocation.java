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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public record SeqMemoryLocation(
    MPOROptions options,
    Optional<CFAEdgeForThread> callContext,
    CVariableDeclaration declaration,
    Optional<CCompositeTypeMemberDeclaration> fieldMember) {

  public static SeqMemoryLocation of(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CVariableDeclaration pDeclaration) {
    return new SeqMemoryLocation(pOptions, pCallContext, pDeclaration, Optional.empty());
  }

  public static SeqMemoryLocation of(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CVariableDeclaration pDeclaration,
      CCompositeTypeMemberDeclaration pFieldMember) {
    return new SeqMemoryLocation(pOptions, pCallContext, pDeclaration, Optional.of(pFieldMember));
  }

  public String getName() {
    StringBuilder name = new StringBuilder();
    name.append(buildThreadPrefix());
    name.append(declaration.getName());
    if (fieldMember.isPresent()) {
      name.append("_").append(fieldMember.orElseThrow().getName());
    }
    return name.toString();
  }

  private String buildThreadPrefix() {
    // global variable declarations have no thread prefix, they "belong" to no thread
    if (declaration.isGlobal()) {
      return "";
    }
    // use call context ID if possible, otherwise use 0 (only main() declarations have no context)
    int threadId = callContext.isPresent() ? callContext.orElseThrow().threadId : 0;
    return SeqNameUtil.buildThreadPrefix(options, threadId);
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
    return SeqMemoryLocation.of(options, callContext, declaration);
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
    return pOther instanceof SeqMemoryLocation other
        // consider call context only for non-global variables
        && (declaration.isGlobal() || callContext.equals(other.callContext))
        && fieldMember.equals(other.fieldMember)
        && declaration.equals(other.declaration);
  }

  @Override
  public String toString() {
    if (fieldMember.isEmpty()) {
      return declaration.toASTString();
    }
    return declaration.toASTString() + " -> " + fieldMember.orElseThrow().toASTString();
  }
}
