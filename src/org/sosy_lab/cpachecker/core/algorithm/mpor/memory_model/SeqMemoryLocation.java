// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public record SeqMemoryLocation(
    Optional<CFAEdgeForThread> callContext,
    CVariableDeclaration declaration,
    Optional<CCompositeTypeMemberDeclaration> fieldMember,
    ImmutableList<CExpression> expressions) {

  public SeqMemoryLocation {
    checkArgument(!expressions.isEmpty(), "The list of CExpressions cannot be empty.");
    checkArgument(
        fieldMember.isPresent() || expressions.size() == 1,
        "If fieldMember is empty, then expressions must have a single element.");
    checkArgument(
        fieldMember.isPresent()
            || !(Iterables.getOnlyElement(expressions) instanceof CFieldReference),
        "If fieldMember is empty, then expressions cannot contain a CFieldReference.");
    checkArgument(
        fieldMember.isEmpty() || expressions.stream().allMatch(e -> e instanceof CFieldReference),
        "If fieldMember is present, then expression must be a CFieldReference.");
  }

  public static SeqMemoryLocation of(
      Optional<CFAEdgeForThread> pCallContext,
      CVariableDeclaration pDeclaration,
      CExpression pExpression) {

    return new SeqMemoryLocation(
        pCallContext, pDeclaration, Optional.empty(), ImmutableList.of(pExpression));
  }

  public static SeqMemoryLocation of(
      Optional<CFAEdgeForThread> pCallContext,
      CVariableDeclaration pDeclaration,
      CCompositeTypeMemberDeclaration pFieldMember,
      ImmutableList<CExpression> pExpressions) {

    return new SeqMemoryLocation(
        pCallContext, pDeclaration, Optional.of(pFieldMember), pExpressions);
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
        fieldMember.isPresent(),
        "Cannot get field owner MemoryLocation because field member is empty.");
    // just return the declaration of the field owner, without any field member
    return SeqMemoryLocation.of(
        callContext, declaration, ((CFieldReference) expressions.getFirst()).getFieldOwner());
  }

  @Override
  public int hashCode() {
    // consider call context only for non-global variables
    return Objects.hash(declaration.isGlobal() ? null : callContext, declaration, fieldMember);
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
                Optional<CCompositeTypeMemberDeclaration> pFieldMember,
                // the expression is not used for equality because a memory location is defined by
                // its declaration and its call context, but not the way it is expressed
                ImmutableList<CExpression> ignored)
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
