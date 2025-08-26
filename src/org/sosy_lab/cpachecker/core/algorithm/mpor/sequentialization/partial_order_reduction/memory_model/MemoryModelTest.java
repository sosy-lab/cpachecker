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
import com.google.common.collect.ImmutableTable;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.Test;
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
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

public class MemoryModelTest {

  // Simple Types

  private final CSimpleType int_type =
      new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType int_pointer_type = new CPointerType(false, false, int_type);

  // Composite Type Member Declarations (inner struct)

  private final CCompositeTypeMemberDeclaration inner_struct_member_declaration =
      new CCompositeTypeMemberDeclaration(int_type, "inner_member");

  // Complex Types (inner struct)

  private final CComplexType inner_struct_complex_type =
      new CCompositeType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(inner_struct_member_declaration),
          "inner_struct_complex",
          "inner_struct_complex");

  private final CElaboratedType inner_struct_elaborated_type =
      new CElaboratedType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          "inner_struct_elaborated",
          "inner_struct_elaborated",
          inner_struct_complex_type);

  private final CTypedefType inner_struct_type =
      new CTypedefType(false, false, "Inner", inner_struct_elaborated_type);

  // Composite Type Member Declarations (outer struct)

  private final CCompositeTypeMemberDeclaration outer_struct_member_declaration =
      new CCompositeTypeMemberDeclaration(int_type, "outer_member");

  private final CCompositeTypeMemberDeclaration outer_struct_inner_struct_member_declaration =
      new CCompositeTypeMemberDeclaration(inner_struct_type, "inner_struct");

  // Complex Types (outer struct)

  private final CComplexType outer_struct_complex_type =
      new CCompositeType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(
              outer_struct_member_declaration, outer_struct_inner_struct_member_declaration),
          "outer_struct_complex",
          "outer_struct_complex");

  private final CElaboratedType outer_struct_elaborated_type =
      new CElaboratedType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          "outer_struct_elaborated",
          "outer_struct_elaborated",
          outer_struct_complex_type);

  private final CTypedefType outer_struct_type =
      new CTypedefType(false, false, "Outer", outer_struct_elaborated_type);

  // Expressions

  private final CIntegerLiteralExpression int_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, int_type, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer int_0_initializer =
      new CInitializerExpression(FileLocation.DUMMY, int_0);

  private final CInitializerList empty_initializer_list =
      new CInitializerList(FileLocation.DUMMY, ImmutableList.of());

  // Declarations

  private final CVariableDeclaration int_pointer_a_declaration =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          int_pointer_type,
          "int_ptr_A",
          "int_ptr_A",
          "int_ptr_A",
          int_0_initializer);

  private final CVariableDeclaration int_pointer_b_declaration =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          int_pointer_type,
          "int_ptr_B",
          "int_ptr_B",
          "int_ptr_B",
          int_0_initializer);

  private final CVariableDeclaration int_a_declaration =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          int_type,
          "int_A",
          "int_A",
          "int_A",
          int_0_initializer);

  private final CVariableDeclaration int_b_declaration =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          int_type,
          "int_B",
          "int_B",
          "int_B",
          int_0_initializer);

  private final CVariableDeclaration outer_struct_declaration =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          outer_struct_type,
          "outer_struct",
          "outer_struct",
          "outer_struct",
          empty_initializer_list);

  // Memory Locations

  private final MemoryLocation int_pointer_a_memory_location =
      MemoryLocation.of(Optional.empty(), int_pointer_a_declaration);

  private final MemoryLocation int_pointer_b_memory_location =
      MemoryLocation.of(Optional.empty(), int_pointer_b_declaration);

  private final MemoryLocation int_a_memory_location =
      MemoryLocation.of(Optional.empty(), int_a_declaration);

  private final MemoryLocation int_b_memory_location =
      MemoryLocation.of(Optional.empty(), int_b_declaration);

  private final MemoryLocation outer_struct_member_memory_location =
      MemoryLocation.of(
          Optional.empty(), outer_struct_declaration, outer_struct_member_declaration);

  private final MemoryLocation outer_struct_inner_struct_member_memory_location =
      MemoryLocation.of(
          Optional.empty(), outer_struct_declaration, inner_struct_member_declaration);

  // Memory Location IDs

  private final ImmutableMap<MemoryLocation, Integer> memory_location_ids =
      ImmutableMap.<MemoryLocation, Integer>builder()
          .put(int_pointer_a_memory_location, 0)
          .put(int_pointer_b_memory_location, 1)
          .put(int_a_memory_location, 2)
          .put(int_b_memory_location, 3)
          .put(outer_struct_member_memory_location, 4)
          .put(outer_struct_inner_struct_member_memory_location, 5)
          .buildOrThrow();

  @Test
  public void test_memory_location_equals() {
    MemoryLocation int_pointer_a_memory_location_alt =
        MemoryLocation.of(Optional.empty(), int_pointer_a_declaration);
    assertThat(int_pointer_a_memory_location.equals(int_pointer_a_memory_location_alt)).isTrue();
  }

  @Test
  public void test_single_pointer_assignment() {
    // int_ptr_A = &int_A; i.e. pointer assignment
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(int_pointer_a_declaration, int_a_memory_location)
            .build();
    // *int_ptr_A i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(int_pointer_a_memory_location).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'int_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            int_pointer_a_memory_location, Optional.empty(), testMemoryModel);

    // only memory location of 'int_A' should be associated with dereference of 'int_ptr_A'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(int_a_memory_location)).isTrue();
  }

  @Test
  public void test_multi_pointer_assignment() {
    // int_ptr_A = &int_A; and int_ptr_A = &int_B; i.e. pointer assignments
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(int_pointer_a_declaration, int_a_memory_location)
            .put(int_pointer_a_declaration, int_b_memory_location)
            .build();
    // *int_ptr_A i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(int_pointer_a_memory_location).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'int_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            int_pointer_a_memory_location, Optional.empty(), testMemoryModel);

    // memory location of 'int_A' and 'int_B' should be associated with dereference of 'int_ptr_A'
    assertThat(memoryLocations.size() == 2).isTrue();
    assertThat(memoryLocations.contains(int_a_memory_location)).isTrue();
    assertThat(memoryLocations.contains(int_b_memory_location)).isTrue();
  }

  @Test
  public void test_transitive_pointer_assignment() {
    // int_ptr_A = &int_A; and int_ptr_B = int_ptr_A; i.e. pointer assignment (transitive)
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(int_pointer_a_declaration, int_a_memory_location)
            .put(int_pointer_b_declaration, int_pointer_a_memory_location)
            .build();
    // *int_ptr_B i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(int_pointer_b_memory_location).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'int_ptr_B'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            int_pointer_b_memory_location, Optional.empty(), testMemoryModel);

    // memory location of 'int_A' should be associated with dereference of 'int_ptr_B'
    // even without direct assignment, due to transitive assignment of 'int_ptr_B = int_ptr_A'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(int_a_memory_location)).isTrue();
  }

  @Test
  public void test_field_owner_field_member() {
    // int_ptr_A = &outer_struct.outer_member; i.e. pointer assignment
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(int_pointer_a_declaration, outer_struct_member_memory_location)
            .build();
    // *int_ptr_A i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(int_pointer_a_memory_location).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'int_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            int_pointer_a_memory_location, Optional.empty(), testMemoryModel);

    // memory location of 'outer_struct.outer_member' should be associated with dereference of
    // 'int_ptr_A'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(outer_struct_member_memory_location)).isTrue();
  }

  @Test
  public void test_outer_inner_struct() {
    // int_ptr_A = &outer_struct.outer_member; i.e. pointer assignment and
    // int_ptr_B = &outer_struct.inner_struct.inner_member
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(int_pointer_a_declaration, outer_struct_member_memory_location)
            .put(int_pointer_b_declaration, outer_struct_inner_struct_member_memory_location)
            .build();
    // *int_ptr_A and *int_ptr_B i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder()
            .add(int_pointer_a_memory_location)
            .add(int_pointer_b_memory_location)
            .build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'int_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocationsA =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            int_pointer_a_memory_location, Optional.empty(), testMemoryModel);
    // memory location of 'outer_struct.outer_member' should be associated with dereference of
    // 'int_ptr_A'
    assertThat(memoryLocationsA.size() == 1).isTrue();
    assertThat(memoryLocationsA.contains(outer_struct_member_memory_location)).isTrue();

    // find the memory locations associated with dereference of 'int_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocationsB =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            int_pointer_b_memory_location, Optional.empty(), testMemoryModel);
    // memory location of 'outer_struct.inner_struct.member' should be associated with dereference
    // of 'int_ptr_B'
    assertThat(memoryLocationsB.size() == 1).isTrue();
    assertThat(memoryLocationsB.contains(outer_struct_inner_struct_member_memory_location))
        .isTrue();
  }
}
