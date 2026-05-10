// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

/**
 * Represents an overapproximating memory location that can be used to create partial order
 * reduction statements in the sequentialization.
 *
 * @param callContext The call context for this memory location. Separate call contexts result in
 *     separate memory locations.
 * @param declaration The {@link CSimpleDeclaration} of the memory location which can be null for
 *     functions that were not declared.
 * @param fieldMember The optional {@link CCompositeTypeMemberDeclaration} that is accessed in a
 *     {@link CFieldReference}.
 * @param functionCallExpression The optional {@link CFunctionCallExpression} that returns a memory
 *     location such as {@code malloc}.
 */
public record SeqMemoryLocation(
    Optional<CFAEdgeForThread> callContext,
    @Nullable CSimpleDeclaration declaration,
    Optional<CCompositeTypeMemberDeclaration> fieldMember,
    Optional<CFunctionCallExpression> functionCallExpression) {

  public SeqMemoryLocation {
    if (declaration != null) {
      checkArgument(
          declaration instanceof CFunctionDeclaration
              || declaration instanceof CVariableDeclaration,
          "declaration must be CFunctionDeclaration or CVariableDeclaration");
      checkArgument(
          !(declaration instanceof CFunctionDeclaration) || functionCallExpression.isPresent(),
          "If declaration is a CFunctionDeclaration, then functionCallExpression must be present.");
      checkArgument(
          !(declaration instanceof CFunctionDeclaration) || fieldMember.isEmpty(),
          "If declaration is a CFunctionDeclaration, then fieldMember must be empty.");
      checkArgument(
          !(declaration instanceof CVariableDeclaration) || functionCallExpression.isEmpty(),
          "If declaration is a CVariableDeclaration, then functionCallExpression must be empty.");
    }
  }

  public static SeqMemoryLocation of(
      Optional<CFAEdgeForThread> pCallContext, CSimpleDeclaration pDeclaration) {

    return new SeqMemoryLocation(pCallContext, pDeclaration, Optional.empty(), Optional.empty());
  }

  public static SeqMemoryLocation of(
      Optional<CFAEdgeForThread> pCallContext,
      CSimpleDeclaration pDeclaration,
      Optional<CCompositeTypeMemberDeclaration> pFieldMember) {

    return new SeqMemoryLocation(pCallContext, pDeclaration, pFieldMember, Optional.empty());
  }

  public static SeqMemoryLocation of(
      Optional<CFAEdgeForThread> pCallContext,
      CSimpleDeclaration pDeclaration,
      Optional<CCompositeTypeMemberDeclaration> pFieldMember,
      Optional<CFunctionCallExpression> pFunctionCallExpression) {

    return new SeqMemoryLocation(pCallContext, pDeclaration, pFieldMember, pFunctionCallExpression);
  }

  public String getName() {
    StringBuilder name = new StringBuilder();

    // only local variables are prefixed with a thread id
    if (isGlobal()) {
      // use call context if possible, otherwise use 0 (only main() declarations have no context)
      name.append("T")
          .append(callContext.isPresent() ? callContext.orElseThrow().threadId : 0)
          .append("_");
    }

    if (declaration != null) {
      name.append(declaration.getName());
    }

    if (fieldMember.isPresent()) {
      name.append("_").append(fieldMember.orElseThrow().getName());
    }

    if (functionCallExpression.isPresent()) {
      name.append("_").append(functionCallExpression.orElseThrow().toASTString());
    }

    return name.toString();
  }

  public boolean isGlobal() {
    return declaration instanceof CVariableDeclaration variableDeclaration
        && variableDeclaration.isGlobal();
  }

  public boolean isDeclarationPointerType() {
    return declaration != null && declaration.getType() instanceof CPointerType;
  }

  public boolean isFieldMemberPointerType() {
    return fieldMember.isPresent() && fieldMember.orElseThrow().getType() instanceof CPointerType;
  }

  public SeqMemoryLocation getFieldOwnerMemoryLocation() {
    checkArgument(
        declaration != null, "Cannot get field owner MemoryLocation because declaration is null.");
    checkArgument(
        fieldMember.isPresent(),
        "Cannot get field owner MemoryLocation because field member is empty.");
    // just return the declaration of the field owner, without any field member
    return SeqMemoryLocation.of(callContext, declaration);
  }

  @Override
  public int hashCode() {
    // consider call context only for non-global memory locations
    return Objects.hash(
        isGlobal() ? null : callContext, declaration, fieldMember, functionCallExpression);
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
                Optional<CFunctionCallExpression> pFunctionCallExpression)
        // consider call context only for non-global memory locations
        && (isGlobal() || callContext.equals(pCallContext))
        && fieldMember.equals(pFieldMember)
        && (declaration != null && declaration.equals(pDeclaration))
        && functionCallExpression.equals(pFunctionCallExpression);
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder("SeqMemoryLocation[");

    stringBuilder.append("name='").append(getName()).append("'");

    callContext.ifPresent(cfaEdge -> stringBuilder.append(", thread=").append(cfaEdge.threadId));

    if (isGlobal()) {
      stringBuilder.append(", [GLOBAL]");
    } else if (functionCallExpression.isPresent()) {
      stringBuilder.append(", [FUNCTION]");
    } else {
      stringBuilder.append(", [LOCAL]");
    }

    return stringBuilder.append("]").toString();
  }
}
