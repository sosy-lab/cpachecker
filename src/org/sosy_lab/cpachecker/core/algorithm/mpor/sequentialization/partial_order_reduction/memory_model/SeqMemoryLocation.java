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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public class SeqMemoryLocation {

  public final MPOROptions options;

  public final Optional<CFAEdgeForThread> callContext;

  private final boolean isExplicitGlobal;

  private final boolean isParameter;

  public final CSimpleDeclaration declaration;

  public final Optional<CCompositeTypeMemberDeclaration> fieldMember;

  /**
   * The optional arg index, if this memory location is a parameter. A single {@link
   * CParameterDeclaration} may link to multiple memory locations in the same call context (cf.
   * variadic functinos).
   */
  private final Optional<Integer> argumentIndex;

  private SeqMemoryLocation(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CSimpleDeclaration pDeclaration,
      Optional<CCompositeTypeMemberDeclaration> pFieldMember,
      Optional<Integer> pArgumentIndex) {

    options = pOptions;
    callContext = pCallContext;
    isExplicitGlobal =
        pDeclaration instanceof CVariableDeclaration variableDeclaration
            && variableDeclaration.isGlobal();
    isParameter = pDeclaration instanceof CParameterDeclaration;
    declaration = pDeclaration;
    fieldMember = pFieldMember;
    argumentIndex = pArgumentIndex;
  }

  public static SeqMemoryLocation of(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CSimpleDeclaration pDeclaration) {

    return new SeqMemoryLocation(
        pOptions, pCallContext, pDeclaration, Optional.empty(), Optional.empty());
  }

  public static SeqMemoryLocation of(
      MPOROptions pOptions,
      CFAEdgeForThread pCallContext,
      CParameterDeclaration pDeclaration,
      int pArgumentIndex) {

    return new SeqMemoryLocation(
        pOptions,
        Optional.of(pCallContext),
        pDeclaration,
        Optional.empty(),
        Optional.of(pArgumentIndex));
  }

  public static SeqMemoryLocation of(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember) {

    return new SeqMemoryLocation(
        pOptions, pCallContext, pFieldOwner, Optional.of(pFieldMember), Optional.empty());
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
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      CSimpleDeclaration pDeclaration) {

    // global variable and function declarations have no thread prefix, they "belong" to no thread
    if (pDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (variableDeclaration.isGlobal()) {
        return SeqSyntax.EMPTY_STRING;
      }
    }
    if (pDeclaration instanceof CFunctionDeclaration) {
      return SeqSyntax.EMPTY_STRING;
    }
    // use call context ID if possible, otherwise use 0 (only main() declarations have no context)
    int threadId = pCallContext.isPresent() ? pCallContext.orElseThrow().threadId : 0;
    return SeqNameUtil.buildThreadPrefix(pOptions, threadId);
  }

  public boolean isExplicitGlobal() {
    return isExplicitGlobal;
  }

  public boolean isParameter() {
    return isParameter;
  }

  public boolean isFieldOwnerPointerType() {
    if (fieldMember.isPresent()) {
      return declaration.getType() instanceof CPointerType;
    }
    return false;
  }

  public boolean isConstCpaCheckerTmp() {
    if (declaration instanceof CVariableDeclaration variableDeclaration) {
      return MPORUtil.isConstCpaCheckerTmp(variableDeclaration);
    }
    return false;
  }

  public SeqMemoryLocation getFieldOwnerMemoryLocation() {
    assert fieldMember.isPresent() : "cannot get field owner MemoryLocation, field member is empty";
    return SeqMemoryLocation.of(options, callContext, declaration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        // consider the call context only if it is a parameter memory location
        (isParameter ? callContext : null),
        isExplicitGlobal,
        isParameter,
        declaration,
        fieldMember,
        argumentIndex);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof SeqMemoryLocation other
        // consider the call context only if it is a parameter memory location
        && (!isParameter || callContext.equals(other.callContext))
        && isExplicitGlobal == other.isExplicitGlobal
        && isParameter == other.isParameter
        && declaration.equals(other.declaration)
        && fieldMember.equals(other.fieldMember)
        && argumentIndex.equals(other.argumentIndex);
  }

  @Override
  public String toString() {
    if (fieldMember.isEmpty()) {
      return declaration.toASTString();
    }
    return declaration.toASTString() + " -> " + fieldMember.orElseThrow().toASTString();
  }
}
