// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class MemoryLocation {

  public final Optional<CSimpleDeclaration> variable;

  public final Optional<SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
      fieldMember;

  private MemoryLocation(
      Optional<CSimpleDeclaration> pVariable,
      Optional<SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
          pFieldMember) {

    variable = pVariable;
    fieldMember = pFieldMember;
  }

  public static MemoryLocation of(CSimpleDeclaration pDeclaration) {
    return new MemoryLocation(Optional.of(pDeclaration), Optional.empty());
  }

  public static MemoryLocation of(
      CSimpleDeclaration pFieldOwner, CCompositeTypeMemberDeclaration pFieldMember) {

    return new MemoryLocation(
        Optional.empty(),
        Optional.of(new AbstractMap.SimpleImmutableEntry<>(pFieldOwner, pFieldMember)));
  }

  public static MemoryLocation empty() {
    return new MemoryLocation(Optional.empty(), Optional.empty());
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
      return variable.orElseThrow().getName();
    }
    Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration> entry = fieldMember.orElseThrow();
    return entry.getKey().getName() + SeqSyntax.UNDERSCORE + entry.getValue().getName();
  }

  public boolean isEmpty() {
    return variable.isEmpty() && fieldMember.isEmpty();
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
