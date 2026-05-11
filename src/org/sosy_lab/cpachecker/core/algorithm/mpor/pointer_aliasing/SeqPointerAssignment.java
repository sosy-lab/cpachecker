// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public record SeqPointerAssignment(
    SeqPointerAssignmentType type,
    SeqMemoryLocation leftHandSideMemoryLocation,
    SeqMemoryLocation rightHandSideMemoryLocation) {

  public SeqPointerAssignment {
    // check that the left-hand side is actually a pointer
    if (leftHandSideMemoryLocation.fieldMember().isEmpty()) {
      // if there is no field member, then the declaration must be a valid CPointerType
      checkArgument(
          isValidDeclarationPointerType(leftHandSideMemoryLocation.declaration().getType()),
          "The CType of the memory locations declaration is not a valid CPointerType: %s",
          leftHandSideMemoryLocation.declaration().getType());
    } else {
      CCompositeTypeMemberDeclaration memberDeclaration =
          leftHandSideMemoryLocation.fieldMember().orElseThrow();
      // if there is a field member and the field owner is not a valid CPointerType
      // then the field member must be a validCPointerType
      if (!isValidDeclarationPointerType(leftHandSideMemoryLocation.declaration().getType())) {
        checkArgument(
            isValidDeclarationPointerType(memberDeclaration.getType()),
            "The CType of the memory locations field member is not a valid CPointerType: %s",
            memberDeclaration.getType());
      }
    }
  }

  private static boolean isValidDeclarationPointerType(CType pType) {
    if (pType instanceof CPointerType) {
      return true;
    }
    // CArrayType.getType() corresponds to the CType of the arrays elements
    if (pType instanceof CArrayType arrayType && arrayType.getType() instanceof CPointerType) {
      return true;
    }
    return false;
  }
}
