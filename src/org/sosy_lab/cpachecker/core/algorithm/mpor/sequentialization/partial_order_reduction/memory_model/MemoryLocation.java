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
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryLocation {

  /**
   * The thread that this memory location belongs to, if it is local. Empty string if the memory
   * location is global.
   */
  private final String threadPrefix;

  public final Optional<ThreadEdge> callContext;

  private final boolean isGlobal;

  private final boolean isParameter;

  public final Optional<CSimpleDeclaration> variable;

  public final Optional<SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
      fieldMember;

  private MemoryLocation(
      Optional<MPOROptions> pOptions,
      Optional<MPORThread> pThread,
      Optional<ThreadEdge> pCallContext,
      Optional<CSimpleDeclaration> pVariable,
      Optional<SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
          pFieldMember) {

    checkArgument(
        pVariable.isEmpty() || pFieldMember.isEmpty(),
        "either pVariable or pFieldMember must be empty");
    threadPrefix =
        pThread.isPresent()
            ? SeqNameUtil.buildThreadPrefix(pOptions.orElseThrow(), pThread.orElseThrow().id)
            : SeqSyntax.EMPTY_STRING;
    callContext = pCallContext;
    isGlobal = MemoryLocationUtil.isGlobal(pVariable, pFieldMember);
    isParameter = MemoryLocationUtil.isParameter(pVariable, pFieldMember);
    // check after assignment that the thread is present, if the memory location is local
    checkArgument(
        threadPrefix.isEmpty() || !isGlobal,
        "if threadPrefix is not empty, the memory location must be local");
    variable = pVariable;
    fieldMember = pFieldMember;
  }

  public static MemoryLocation of(
      MPOROptions pOptions,
      Optional<MPORThread> pThread,
      Optional<ThreadEdge> pCallContext,
      CSimpleDeclaration pDeclaration) {

    return new MemoryLocation(
        Optional.of(pOptions), pThread, pCallContext, Optional.of(pDeclaration), Optional.empty());
  }

  public static MemoryLocation of(
      MPOROptions pOptions,
      Optional<MPORThread> pThread,
      Optional<ThreadEdge> pCallContext,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember) {

    return new MemoryLocation(
        Optional.of(pOptions),
        pThread,
        pCallContext,
        Optional.empty(),
        Optional.of(new AbstractMap.SimpleImmutableEntry<>(pFieldOwner, pFieldMember)));
  }

  public static MemoryLocation empty() {
    return new MemoryLocation(
        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public CSimpleDeclaration getSimpleDeclaration() {
    if (variable.isPresent()) {
      return variable.orElseThrow();
    }
    if (fieldMember.isPresent()) {
      return fieldMember.orElseThrow().getKey();
    }
    throw new IllegalArgumentException(
        "cannot get CSimpleDeclaration, both variable and fieldMember are empty");
  }

  public String getName() {
    if (variable.isPresent()) {
      return threadPrefix + variable.orElseThrow().getName();
    }
    Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration> entry = fieldMember.orElseThrow();
    return threadPrefix
        + entry.getKey().getName()
        + SeqSyntax.UNDERSCORE
        + entry.getValue().getName();
  }

  public boolean isEmpty() {
    return variable.isEmpty() && fieldMember.isEmpty();
  }

  public boolean isGlobal() {
    return isGlobal;
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
        isGlobal,
        isParameter,
        variable,
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
        && isGlobal == other.isGlobal
        && isParameter == other.isParameter
        && variable.equals(other.variable)
        && fieldMember.equals(other.fieldMember);
  }

  @Override
  public String toString() {
    if (variable.isPresent()) {
      return variable.orElseThrow().toASTString();
    }
    if (fieldMember.isPresent()) {
      Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration> entry = fieldMember.orElseThrow();
      return entry.getKey().toASTString() + " -> " + entry.getValue().toASTString();
    }
    throw new IllegalArgumentException(
        "cannot get String, both variable and fieldMember are empty");
  }
}
