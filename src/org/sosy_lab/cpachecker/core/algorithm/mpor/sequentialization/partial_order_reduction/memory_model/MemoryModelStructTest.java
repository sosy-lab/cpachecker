// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

public class MemoryModelStructTest {

  // Simple Types

  private final CSimpleType INT_TYPE =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(CTypeQualifiers.NONE, INT_TYPE);

  // Composite Type Member Declarations (inner struct)

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "inner_member");

  // Complex Types (inner struct)

  private final CComplexType INNER_STRUCT_COMPLEX_TYPE =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(INNER_STRUCT_MEMBER_DECLARATION),
          "inner_struct_complex",
          "inner_struct_complex");

  private final CElaboratedType INNER_STRUCT_ELABORATED_TYPE =
      new CElaboratedType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          "inner_struct_elaborated",
          "inner_struct_elaborated",
          INNER_STRUCT_COMPLEX_TYPE);

  private final CTypedefType INNER_STRUCT_TYPE =
      new CTypedefType(CTypeQualifiers.NONE, "Inner", INNER_STRUCT_ELABORATED_TYPE);

  // Composite Type Member Declarations (outer struct)

  private final CCompositeTypeMemberDeclaration OUTER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "outer_member");

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_DECLARATION =
      new CCompositeTypeMemberDeclaration(INNER_STRUCT_TYPE, "inner_struct");

  // Complex Types (outer struct)

  private final CComplexType OUTER_STRUCT_COMPLEX_TYPE =
      new CCompositeType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(OUTER_STRUCT_MEMBER_DECLARATION, INNER_STRUCT_DECLARATION),
          "outer_struct_complex",
          "outer_struct_complex");

  private final CElaboratedType OUTER_STRUCT_ELABORATED_TYPE =
      new CElaboratedType(
          CTypeQualifiers.NONE,
          ComplexTypeKind.STRUCT,
          "outer_struct_elaborated",
          "outer_struct_elaborated",
          OUTER_STRUCT_COMPLEX_TYPE);

  private final CTypedefType OUTER_STRUCT_TYPE =
      new CTypedefType(CTypeQualifiers.NONE, "Outer", OUTER_STRUCT_ELABORATED_TYPE);

  // Expressions

  private final CIntegerLiteralExpression INT_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer INT_0_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, INT_0);

  private final CInitializerList EMPTY_INITIALIZER_LIST =
      new CInitializerList(FileLocation.DUMMY, ImmutableList.of());

  // Declarations

  private final CVariableDeclaration GLOBAL_POINTER_A_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_POINTER_TYPE,
          "global_ptr_A",
          "global_ptr_A",
          "global_ptr_A",
          INT_0_INITIALIZER);

  private final CVariableDeclaration GLOBAL_POINTER_B_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_POINTER_TYPE,
          "global_ptr_B",
          "global_ptr_B",
          "global_ptr_B",
          INT_0_INITIALIZER);

  private final CVariableDeclaration OUTER_STRUCT_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          OUTER_STRUCT_TYPE,
          "outer_struct",
          "outer_struct",
          "outer_struct",
          EMPTY_INITIALIZER_LIST);

  // Memory Locations (primitives)

  private final SeqMemoryLocation GLOBAL_POINTER_A_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), GLOBAL_POINTER_A_DECLARATION);

  private final SeqMemoryLocation GLOBAL_POINTER_B_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), GLOBAL_POINTER_B_DECLARATION);

  // Memory Locations (structs)

  private final SeqMemoryLocation OUTER_STRUCT_MEMBER_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(),
          Optional.empty(),
          OUTER_STRUCT_DECLARATION,
          OUTER_STRUCT_MEMBER_DECLARATION);

  private final SeqMemoryLocation INNER_STRUCT_MEMBER_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(),
          Optional.empty(),
          OUTER_STRUCT_DECLARATION,
          INNER_STRUCT_MEMBER_DECLARATION);

  public MemoryModelStructTest() throws InvalidConfigurationException {}

  @Test
  public void test_field_owner_field_member() {
    // global_ptr_A = &outer_struct.outer_member; i.e. pointer assignment
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<SeqMemoryLocation, SeqMemoryLocation>builder()
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, OUTER_STRUCT_MEMBER_MEMORY_LOCATION)
            .build();

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_A_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            ImmutableMap.of());

    // mem location 'outer_struct.outer_member' should be associated with dereference of
    // 'global_ptr_A'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations).contains(OUTER_STRUCT_MEMBER_MEMORY_LOCATION);
  }

  @Test
  public void test_outer_inner_struct() {
    // global_ptr_A = &outer_struct.outer_member; i.e. pointer assignment and
    // global_ptr_B = &outer_struct.inner_struct.inner_member
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<SeqMemoryLocation, SeqMemoryLocation>builder()
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, OUTER_STRUCT_MEMBER_MEMORY_LOCATION)
            .put(GLOBAL_POINTER_B_MEMORY_LOCATION, INNER_STRUCT_MEMBER_MEMORY_LOCATION)
            .build();

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<SeqMemoryLocation> memoryLocationsA =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_A_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            ImmutableMap.of());
    // mem location 'outer_struct.outer_member' should be associated with deref of 'global_ptr_A'
    assertThat(memoryLocationsA).hasSize(1);
    assertThat(memoryLocationsA).contains(OUTER_STRUCT_MEMBER_MEMORY_LOCATION);

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<SeqMemoryLocation> memoryLocationsB =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_B_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            ImmutableMap.of());
    // mem location 'outer_struct.inner_struct.member' should be associated with deref
    // 'global_ptr_B'
    assertThat(memoryLocationsB).hasSize(1);
    assertThat(memoryLocationsB).contains(INNER_STRUCT_MEMBER_MEMORY_LOCATION);
  }
}
