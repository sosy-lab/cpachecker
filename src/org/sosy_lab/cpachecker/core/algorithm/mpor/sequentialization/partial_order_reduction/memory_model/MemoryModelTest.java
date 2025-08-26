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

  private final CSimpleType INT_TYPE =
      new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(false, false, INT_TYPE);

  // Composite Type Member Declarations (inner struct)

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "inner_member");

  // Complex Types (inner struct)

  private final CComplexType INNER_STRUCT_COMPLEX_TYPE =
      new CCompositeType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(INNER_STRUCT_MEMBER_DECLARATION),
          "inner_struct_complex",
          "inner_struct_complex");

  private final CElaboratedType INNER_STRUCT_ELABORATED_TYPE =
      new CElaboratedType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          "inner_struct_elaborated",
          "inner_struct_elaborated",
          INNER_STRUCT_COMPLEX_TYPE);

  private final CTypedefType INNER_STRUCT_TYPE =
      new CTypedefType(false, false, "Inner", INNER_STRUCT_ELABORATED_TYPE);

  // Composite Type Member Declarations (outer struct)

  private final CCompositeTypeMemberDeclaration OUTER_STRUCT_MEMBER_DECLARATION =
      new CCompositeTypeMemberDeclaration(INT_TYPE, "outer_member");

  private final CCompositeTypeMemberDeclaration INNER_STRUCT_DECLARATION =
      new CCompositeTypeMemberDeclaration(INNER_STRUCT_TYPE, "inner_struct");

  // Complex Types (outer struct)

  private final CComplexType OUTER_STRUCT_COMPLEX_TYPE =
      new CCompositeType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          ImmutableList.of(OUTER_STRUCT_MEMBER_DECLARATION, INNER_STRUCT_DECLARATION),
          "outer_struct_complex",
          "outer_struct_complex");

  private final CElaboratedType OUTER_STRUCT_ELABORATED_TYPE =
      new CElaboratedType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          "outer_struct_elaborated",
          "outer_struct_elaborated",
          OUTER_STRUCT_COMPLEX_TYPE);

  private final CTypedefType OUTER_STRUCT_TYPE =
      new CTypedefType(false, false, "Outer", OUTER_STRUCT_ELABORATED_TYPE);

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

  private final CVariableDeclaration LOCAL_POINTER_C_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          INT_POINTER_TYPE,
          "local_ptr_C",
          "local_ptr_C",
          "local_ptr_C",
          INT_0_INITIALIZER);

  private final CVariableDeclaration LOCAL_POINTER_D_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          INT_POINTER_TYPE,
          "local_ptr_D",
          "local_ptr_D",
          "local_ptr_D",
          INT_0_INITIALIZER);

  private final CVariableDeclaration GLOBAL_X_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_TYPE,
          "global_X",
          "global_X",
          "global_X",
          INT_0_INITIALIZER);

  private final CVariableDeclaration GLOBAL_Y_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          INT_TYPE,
          "global_Y",
          "global_Y",
          "global_Y",
          INT_0_INITIALIZER);

  private final CVariableDeclaration LOCAL_Z_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          INT_TYPE,
          "local_Z",
          "local_Z",
          "local_Z",
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

  private final MemoryLocation GLOBAL_POINTER_A_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), GLOBAL_POINTER_A_DECLARATION);

  private final MemoryLocation GLOBAL_POINTER_B_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), GLOBAL_POINTER_B_DECLARATION);

  private final MemoryLocation LOCAL_POINTER_C_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), LOCAL_POINTER_C_DECLARATION);

  private final MemoryLocation LOCAL_POINTER_D_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), LOCAL_POINTER_D_DECLARATION);

  private final MemoryLocation GLOBAL_X_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), GLOBAL_X_DECLARATION);

  private final MemoryLocation GLOBAL_Y_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), GLOBAL_Y_DECLARATION);

  private final MemoryLocation LOCAL_Z_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), LOCAL_Z_DECLARATION);

  // Memory Locations (structs)

  private final MemoryLocation OUTER_STRUCT_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), OUTER_STRUCT_DECLARATION);

  private final MemoryLocation OUTER_STRUCT_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(
          Optional.empty(), OUTER_STRUCT_DECLARATION, OUTER_STRUCT_MEMBER_DECLARATION);

  private final MemoryLocation INNER_STRUCT_MEMORY_LOCATION =
      MemoryLocation.of(Optional.empty(), OUTER_STRUCT_DECLARATION, INNER_STRUCT_DECLARATION);

  private final MemoryLocation INNER_STRUCT_MEMBER_MEMORY_LOCATION =
      MemoryLocation.of(
          Optional.empty(), OUTER_STRUCT_DECLARATION, INNER_STRUCT_MEMBER_DECLARATION);

  // Memory Location IDs

  private final ImmutableMap<MemoryLocation, Integer> memory_location_ids =
      ImmutableMap.<MemoryLocation, Integer>builder()
          .put(GLOBAL_POINTER_A_MEMORY_LOCATION, 0)
          .put(GLOBAL_POINTER_B_MEMORY_LOCATION, 1)
          .put(GLOBAL_X_MEMORY_LOCATION, 2)
          .put(GLOBAL_Y_MEMORY_LOCATION, 3)
          .put(LOCAL_Z_MEMORY_LOCATION, 4)
          .put(OUTER_STRUCT_MEMORY_LOCATION, 5)
          .put(INNER_STRUCT_MEMORY_LOCATION, 6)
          .put(INNER_STRUCT_MEMBER_MEMORY_LOCATION, 7)
          .buildOrThrow();

  @Test
  public void test_memory_location_equals() {
    MemoryLocation int_pointer_a_memory_location_alt =
        MemoryLocation.of(Optional.empty(), GLOBAL_POINTER_A_DECLARATION);
    assertThat(GLOBAL_POINTER_A_MEMORY_LOCATION.equals(int_pointer_a_memory_location_alt)).isTrue();
  }

  @Test
  public void test_single_pointer_assignment() {
    // global_ptr_A = &global_X; i.e. pointer assignment
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(GLOBAL_POINTER_A_DECLARATION, GLOBAL_X_MEMORY_LOCATION)
            .build();
    // *global_ptr_A i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(GLOBAL_POINTER_A_MEMORY_LOCATION).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_A_MEMORY_LOCATION, Optional.empty(), testMemoryModel);

    // only memory location of 'global_X' should be associated with dereference of 'global_ptr_A'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(GLOBAL_X_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_multi_pointer_assignment() {
    // global_ptr_A = &global_X; and global_ptr_A = &global_Y; i.e. pointer assignments
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(GLOBAL_POINTER_A_DECLARATION, GLOBAL_X_MEMORY_LOCATION)
            .put(GLOBAL_POINTER_A_DECLARATION, GLOBAL_Y_MEMORY_LOCATION)
            .build();
    // *global_ptr_A i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(GLOBAL_POINTER_A_MEMORY_LOCATION).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_A_MEMORY_LOCATION, Optional.empty(), testMemoryModel);

    // mem location of 'global_X' and 'global_Y' should be associated with deref of 'global_ptr_A'
    assertThat(memoryLocations.size() == 2).isTrue();
    assertThat(memoryLocations.contains(GLOBAL_X_MEMORY_LOCATION)).isTrue();
    assertThat(memoryLocations.contains(GLOBAL_Y_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_transitive_pointer_assignment() {
    // global_ptr_A = &global_X; and global_ptr_B = global_ptr_A;
    // i.e. pointer assignment (transitive)
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(GLOBAL_POINTER_A_DECLARATION, GLOBAL_X_MEMORY_LOCATION)
            .put(GLOBAL_POINTER_B_DECLARATION, GLOBAL_POINTER_A_MEMORY_LOCATION)
            .build();
    // *global_ptr_B i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(GLOBAL_POINTER_B_MEMORY_LOCATION).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'global_ptr_B'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_B_MEMORY_LOCATION, Optional.empty(), testMemoryModel);

    // memory location of 'global_X' should be associated with dereference of 'global_ptr_B'
    // even without direct assignment, due to transitive assignment of 'global_ptr_B = global_ptr_A'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(GLOBAL_X_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_field_owner_field_member() {
    // global_ptr_A = &outer_struct.outer_member; i.e. pointer assignment
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(GLOBAL_POINTER_A_DECLARATION, OUTER_STRUCT_MEMBER_MEMORY_LOCATION)
            .build();
    // *global_ptr_A i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(GLOBAL_POINTER_A_MEMORY_LOCATION).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_A_MEMORY_LOCATION, Optional.empty(), testMemoryModel);

    // mem location 'outer_struct.outer_member' should be associated with dereference of
    // 'global_ptr_A'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(OUTER_STRUCT_MEMBER_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_outer_inner_struct() {
    // global_ptr_A = &outer_struct.outer_member; i.e. pointer assignment and
    // global_ptr_B = &outer_struct.inner_struct.inner_member
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(GLOBAL_POINTER_A_DECLARATION, OUTER_STRUCT_MEMBER_MEMORY_LOCATION)
            .put(GLOBAL_POINTER_B_DECLARATION, INNER_STRUCT_MEMBER_MEMORY_LOCATION)
            .build();
    // *global_ptr_A and *global_ptr_B i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder()
            .add(GLOBAL_POINTER_A_MEMORY_LOCATION)
            .add(GLOBAL_POINTER_B_MEMORY_LOCATION)
            .build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocationsA =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_A_MEMORY_LOCATION, Optional.empty(), testMemoryModel);
    // mem location 'outer_struct.outer_member' should be associated with deref of 'global_ptr_A'
    assertThat(memoryLocationsA.size() == 1).isTrue();
    assertThat(memoryLocationsA.contains(OUTER_STRUCT_MEMBER_MEMORY_LOCATION)).isTrue();

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<MemoryLocation> memoryLocationsB =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_B_MEMORY_LOCATION, Optional.empty(), testMemoryModel);
    // mem location 'outer_struct.inner_struct.member' should be associated with deref
    // 'global_ptr_B'
    assertThat(memoryLocationsB.size() == 1).isTrue();
    assertThat(memoryLocationsB.contains(INNER_STRUCT_MEMBER_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_implicit_global() {
    // global_ptr_A = &local_Z; i.e. pointer assignment
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(GLOBAL_POINTER_A_DECLARATION, LOCAL_Z_MEMORY_LOCATION)
            .build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), ImmutableSet.of());

    // test that local_Z is now an implicit global memory location, because the global pointer
    // 'global_ptr_A' gets its address, and can be dereferenced by other threads
    assertThat(GLOBAL_POINTER_A_MEMORY_LOCATION.isGlobal()).isTrue();
    assertThat(LOCAL_Z_MEMORY_LOCATION.isGlobal()).isFalse();
    assertThat(testMemoryModel.isImplicitGlobal(LOCAL_Z_MEMORY_LOCATION)).isTrue();
  }

  @Test
  public void test_transitive_implicit_global() {
    // 'local_ptr_C = &local_Z;' and 'local_ptr_D = local_ptr_C;' and 'global_ptr_A = local_ptr_D'
    // i.e. transitive pointer assignments
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(LOCAL_POINTER_C_DECLARATION, LOCAL_Z_MEMORY_LOCATION)
            .put(LOCAL_POINTER_D_DECLARATION, LOCAL_POINTER_C_MEMORY_LOCATION)
            .put(GLOBAL_POINTER_A_DECLARATION, LOCAL_POINTER_D_MEMORY_LOCATION)
            .build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memory_location_ids, pointerAssignments, ImmutableTable.of(), ImmutableSet.of());

    // test that local_Z is now an implicit global memory location, because of transitivity:
    // 'global_ptr_A -> local_ptr_D -> local_ptr_C', and can then be dereferenced by other threads
    assertThat(GLOBAL_POINTER_A_MEMORY_LOCATION.isGlobal()).isTrue();
    assertThat(LOCAL_POINTER_C_MEMORY_LOCATION.isGlobal()).isFalse();
    assertThat(LOCAL_POINTER_D_MEMORY_LOCATION.isGlobal()).isFalse();
    assertThat(LOCAL_Z_MEMORY_LOCATION.isGlobal()).isFalse();
    assertThat(testMemoryModel.isImplicitGlobal(LOCAL_Z_MEMORY_LOCATION)).isTrue();
  }
}
