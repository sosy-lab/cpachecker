// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryLocation {

  /**
   * The thread that this memory location belongs to, if it is local. Empty string if the memory
   * location is global.
   */
  private final String threadPrefix;

  public final Optional<ThreadEdge> callContext;

  private final boolean isExplicitGlobal;

  private final boolean isParameter;

  public final Optional<CSimpleDeclaration> declaration;

  public final Optional<SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
      fieldMember;

  private MemoryLocation(
      String pThreadPrefix,
      Optional<ThreadEdge> pCallContext,
      Optional<CSimpleDeclaration> pDeclaration,
      Optional<SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
          pFieldMember) {

    checkArgument(
        pDeclaration.isEmpty() || pFieldMember.isEmpty(),
        "either pDeclaration or pFieldMember must be empty");
    threadPrefix = pThreadPrefix;
    callContext = pCallContext;
    isExplicitGlobal = MemoryLocationUtil.isExplicitGlobal(pDeclaration, pFieldMember);
    isParameter = MemoryLocationUtil.isParameter(pDeclaration, pFieldMember);
    declaration = pDeclaration;
    fieldMember = pFieldMember;
    // checks after assignments
    assert !isExplicitGlobal || !isParameter
        : "explicit global memory locations cannot be parameters";
  }

  public static MemoryLocation of(
      MPOROptions pOptions, Optional<ThreadEdge> pCallContext, CSimpleDeclaration pDeclaration) {

    if (pCallContext.isPresent()) {
      int threadId = pCallContext.orElseThrow().threadId;
      return MemoryLocation.of(pOptions, threadId, pCallContext, pDeclaration);
    }
    return new MemoryLocation(
        SeqSyntax.EMPTY_STRING, pCallContext, Optional.of(pDeclaration), Optional.empty());
  }

  static MemoryLocation of(
      MPOROptions pOptions,
      int pThreadId,
      Optional<ThreadEdge> pCallContext,
      CSimpleDeclaration pDeclaration) {

    String threadPrefix = SeqNameUtil.buildThreadPrefix(pOptions, pThreadId);
    return new MemoryLocation(
        threadPrefix, pCallContext, Optional.of(pDeclaration), Optional.empty());
  }

  public static MemoryLocation of(
      MPOROptions pOptions,
      int pThreadId,
      Optional<ThreadEdge> pCallContext,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember) {

    String threadPrefix = SeqNameUtil.buildThreadPrefix(pOptions, pThreadId);
    return new MemoryLocation(
        threadPrefix,
        pCallContext,
        Optional.empty(),
        Optional.of(new AbstractMap.SimpleImmutableEntry<>(pFieldOwner, pFieldMember)));
  }

  static MemoryLocation empty() {
    return new MemoryLocation(
        SeqSyntax.EMPTY_STRING, Optional.empty(), Optional.empty(), Optional.empty());
  }

  public CSimpleDeclaration getSimpleDeclaration() {
    if (declaration.isPresent()) {
      return declaration.orElseThrow();
    }
    if (fieldMember.isPresent()) {
      return fieldMember.orElseThrow().getKey();
    }
    throw new IllegalArgumentException(
        "cannot get CSimpleDeclaration, both variable and fieldMember are empty");
  }

  public String getName() {
    if (declaration.isPresent()) {
      return threadPrefix + declaration.orElseThrow().getName();
    }
    Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration> entry = fieldMember.orElseThrow();
    return threadPrefix
        + entry.getKey().getName()
        + SeqSyntax.UNDERSCORE
        + entry.getValue().getName();
  }

  public boolean isEmpty() {
    return declaration.isEmpty() && fieldMember.isEmpty();
  }

  public boolean isExplicitGlobal() {
    return isExplicitGlobal;
  }

  public boolean isParameter() {
    return isParameter;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        threadPrefix,
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
        && threadPrefix.equals(other.threadPrefix)
        // consider the call context only if it is a parameter memory location
        && (!isParameter || callContext.equals(other.callContext))
        && isExplicitGlobal == other.isExplicitGlobal
        && isParameter == other.isParameter
        && declaration.equals(other.declaration)
        && fieldMember.equals(other.fieldMember);
  }

  @Override
  public String toString() {
    if (declaration.isPresent()) {
      return declaration.orElseThrow().toASTString();
    }
    if (fieldMember.isPresent()) {
      Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration> entry = fieldMember.orElseThrow();
      return entry.getKey().toASTString() + " -> " + entry.getValue().toASTString();
    }
    throw new IllegalArgumentException(
        "cannot get String, both variable and fieldMember are empty");
  }
}
