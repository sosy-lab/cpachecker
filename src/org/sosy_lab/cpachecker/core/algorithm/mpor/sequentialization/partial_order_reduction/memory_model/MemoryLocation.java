// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryLocation {

  public final MPOROptions options;

  public final Optional<ThreadEdge> callContext;

  private final boolean isExplicitGlobal;

  private final boolean isParameter;

  public final CSimpleDeclaration declaration;

  public final Optional<CCompositeTypeMemberDeclaration> fieldMember;

  private MemoryLocation(
      MPOROptions pOptions,
      Optional<ThreadEdge> pCallContext,
      @Nullable CSimpleDeclaration pDeclaration,
      Optional<CCompositeTypeMemberDeclaration> pFieldMember) {

    options = pOptions;
    callContext = pCallContext;
    isExplicitGlobal = MemoryLocationUtil.isExplicitGlobal(pDeclaration);
    isParameter = pDeclaration instanceof CParameterDeclaration;
    declaration = pDeclaration;
    fieldMember = pFieldMember;
    // checks after assignments
    assert !isExplicitGlobal || !isParameter
        : "explicit global memory locations cannot be parameters";
  }

  public static MemoryLocation of(
      MPOROptions pOptions, Optional<ThreadEdge> pCallContext, CSimpleDeclaration pDeclaration) {

    return new MemoryLocation(pOptions, pCallContext, pDeclaration, Optional.empty());
  }

  public static MemoryLocation of(
      MPOROptions pOptions,
      Optional<ThreadEdge> pCallContext,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember) {

    return new MemoryLocation(pOptions, pCallContext, pFieldOwner, Optional.of(pFieldMember));
  }

  static MemoryLocation empty() {
    return new MemoryLocation(
        MPOROptions.defaultTestInstance(), Optional.empty(), null, Optional.empty());
  }

  public String getName() {
    StringBuilder name = new StringBuilder();
    name.append(buildThreadPrefix(options, callContext, declaration));
    name.append(declaration.getName());
    if (fieldMember.isPresent()) {
      name.append(SeqSyntax.UNDERSCORE);
      name.append(fieldMember.orElseThrow().getName());
    }
    return name.toString();
  }

  private static String buildThreadPrefix(
      MPOROptions pOptions, Optional<ThreadEdge> pCallContext, CSimpleDeclaration pDeclaration) {

    if (pDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (variableDeclaration.isGlobal()) {
        // global declarations have to thread prefix, they "belong" to no thread
        return SeqSyntax.EMPTY_STRING;
      }
    }
    if (pDeclaration instanceof CFunctionDeclaration) {
      // function declarations have to thread prefix, they "belong" to no thread
      return SeqSyntax.EMPTY_STRING;
    }
    // use call context ID if possible, otherwise use 0 (only main() declarations have no context)
    int threadId = pCallContext.isPresent() ? pCallContext.orElseThrow().threadId : 0;
    return SeqNameUtil.buildThreadPrefix(pOptions, threadId);
  }

  public boolean isEmpty() {
    return declaration == null && fieldMember.isEmpty();
  }

  public boolean isExplicitGlobal() {
    return isExplicitGlobal;
  }

  public boolean isParameter() {
    return isParameter;
  }

  public MemoryLocation getFieldOwnerMemoryLocation() {
    assert fieldMember.isPresent() : "cannot get field owner MemoryLocation, field member is empty";
    return MemoryLocation.of(options, callContext, declaration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        // consider the call context only if it is a parameter memory location
        (isParameter ? callContext : null),
        isExplicitGlobal,
        isParameter,
        declaration,
        fieldMember);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof MemoryLocation other
        // consider the call context only if it is a parameter memory location
        && (!isParameter || callContext.equals(other.callContext))
        && isExplicitGlobal == other.isExplicitGlobal
        && isParameter == other.isParameter
        && declaration.equals(other.declaration)
        && fieldMember.equals(other.fieldMember);
  }

  @Override
  public String toString() {
    assert !isEmpty() : "cannot build String, MemoryLocation is empty";
    if (fieldMember.isEmpty()) {
      return declaration.toASTString();
    }
    return declaration.toASTString() + " -> " + fieldMember.orElseThrow().toASTString();
  }
}
