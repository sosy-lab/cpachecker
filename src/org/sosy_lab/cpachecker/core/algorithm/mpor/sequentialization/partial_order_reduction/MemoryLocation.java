// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class MemoryLocation {

  public final Optional<CVariableDeclaration> variable;

  public final Optional<SimpleImmutableEntry<CVariableDeclaration, CCompositeTypeMemberDeclaration>>
      fieldMember;

  private MemoryLocation(
      Optional<CVariableDeclaration> pVariable,
      Optional<SimpleImmutableEntry<CVariableDeclaration, CCompositeTypeMemberDeclaration>>
          pFieldMember) {

    checkArgument(
        pVariable.isPresent() || pFieldMember.isPresent(),
        "either pVariable or pFieldMember must be present");
    variable = pVariable;
    fieldMember = pFieldMember;
  }

  public static MemoryLocation of(CVariableDeclaration pVariableDeclaration) {
    return new MemoryLocation(Optional.of(pVariableDeclaration), Optional.empty());
  }

  public static MemoryLocation of(
      CVariableDeclaration pFieldOwner, CCompositeTypeMemberDeclaration pFieldMember) {

    return new MemoryLocation(
        Optional.empty(),
        Optional.of(new AbstractMap.SimpleImmutableEntry<>(pFieldOwner, pFieldMember)));
  }

  public String getName() {
    if (variable.isPresent()) {
      return variable.orElseThrow().getName();
    }
    Entry<CVariableDeclaration, CCompositeTypeMemberDeclaration> entry = fieldMember.orElseThrow();
    return entry.getKey().getName() + SeqSyntax.UNDERSCORE + entry.getValue().getName();
  }

  @Override
  public int hashCode() {
    return Objects.hash(variable, fieldMember);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof MemoryLocation other
        && variable.equals(other.variable)
        && fieldMember.equals(other.fieldMember);
  }
}
