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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public abstract sealed class SeqMemoryLocation
    permits SeqParameterMemoryLocation, SeqVariableMemoryLocation {

  public final MPOROptions options;

  public final Optional<CFAEdgeForThread> callContext;

  public final Optional<CCompositeTypeMemberDeclaration> fieldMember;

  protected SeqMemoryLocation(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      Optional<CCompositeTypeMemberDeclaration> pFieldMember) {

    options = pOptions;
    callContext = pCallContext;
    fieldMember = pFieldMember;
  }

  public abstract CSimpleDeclaration getDeclaration();

  public String getName() {
    StringBuilder name = new StringBuilder();
    name.append(buildThreadPrefix());
    name.append(getDeclaration().getName());
    if (fieldMember.isPresent()) {
      name.append(SeqSyntax.UNDERSCORE);
      name.append(fieldMember.orElseThrow().getName());
    }
    return name.toString();
  }

  private String buildThreadPrefix() {
    // global variable declarations have no thread prefix, they "belong" to no thread
    if (getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
      if (variableDeclaration.isGlobal()) {
        return SeqSyntax.EMPTY_STRING;
      }
    }
    // use call context ID if possible, otherwise use 0 (only main() declarations have no context)
    int threadId = callContext.isPresent() ? callContext.orElseThrow().threadId : 0;
    return SeqNameUtil.buildThreadPrefix(options, threadId);
  }

  public boolean isExplicitGlobal() {
    return getDeclaration() instanceof CVariableDeclaration variableDeclaration
        && variableDeclaration.isGlobal();
  }

  public boolean isFieldOwnerPointerType() {
    if (fieldMember.isPresent()) {
      return getDeclaration().getType() instanceof CPointerType;
    }
    return false;
  }

  public boolean isConstCpaCheckerTmp() {
    if (getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
      return MPORUtil.isConstCpaCheckerTmp(variableDeclaration);
    }
    return false;
  }

  public SeqMemoryLocation getFieldOwnerMemoryLocation() {
    checkArgument(
        fieldMember.isPresent(), "cannot get field owner MemoryLocation, field member is empty");
    return switch (this) {
      case SeqParameterMemoryLocation parameterMemoryLocation ->
          SeqParameterMemoryLocation.of(
              options,
              callContext.orElseThrow(),
              parameterMemoryLocation.getDeclaration(),
              parameterMemoryLocation.argumentIndex);
      case SeqVariableMemoryLocation variableMemoryLocation ->
          SeqVariableMemoryLocation.of(
              options, callContext, variableMemoryLocation.getDeclaration());
      default ->
          throw new AssertionError(
              String.format("Unhandled SeqMemoryLocation type: %s", this.getClass()));
    };
  }

  @Override
  public int hashCode() {
    return Objects.hash(callContext, fieldMember);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof SeqMemoryLocation other
        && callContext.equals(other.callContext)
        && fieldMember.equals(other.fieldMember);
  }

  @Override
  public String toString() {
    if (fieldMember.isEmpty()) {
      return getDeclaration().toASTString();
    }
    return getDeclaration().toASTString() + " -> " + fieldMember.orElseThrow().toASTString();
  }
}
