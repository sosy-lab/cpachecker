// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;

public class CCompositeTypeTest {

  /**
   * {@code struct node { const struct node *next; }}: the qualified copy of the struct shares the
   * list of members with the struct, and the list refers to the copy.
   */
  @Test
  public void selfReferenceThroughSharedMembersIsSerializable() {
    CCompositeType node =
        new CCompositeType(CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, "node", "node");
    CCompositeType constNode =
        new CCompositeType(CTypeQualifiers.CONST, ComplexTypeKind.STRUCT, "node", "node");
    ImmutableList<CCompositeTypeMemberDeclaration> members =
        ImmutableList.of(
            new CCompositeTypeMemberDeclaration(
                new CPointerType(CTypeQualifiers.NONE, constNode), "next"));
    node.setMembers(members);
    constNode.setMembers(members);

    CCompositeType copy = SerializableTester.reserialize(node);

    assertThat(copy).isEqualTo(node);
    CCompositeType target =
        (CCompositeType) ((CPointerType) copy.getMembers().getFirst().getType()).getType();
    assertThat(target).isEqualTo(constNode);
    assertThat(target.getMembers()).hasSize(1);
    assertThat(target.getMembers().getFirst().getName()).isEqualTo("next");
  }
}
