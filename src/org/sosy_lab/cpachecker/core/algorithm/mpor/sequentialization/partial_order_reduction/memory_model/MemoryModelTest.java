// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.truth.Truth.assertThat;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

public class MemoryModelTest {

  // Simple Types

  private final CSimpleType INT_TYPE =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(CTypeQualifiers.NONE, INT_TYPE);

  // Expressions

  private final CIntegerLiteralExpression INT_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer INT_0_INITIALIZER =
      new CInitializerExpression(FileLocation.DUMMY, INT_0);

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

  // Memory Locations (primitives)

  private final SeqMemoryLocation GLOBAL_POINTER_A_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), GLOBAL_POINTER_A_DECLARATION);

  private final SeqMemoryLocation GLOBAL_POINTER_B_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), GLOBAL_POINTER_B_DECLARATION);

  private final SeqMemoryLocation LOCAL_POINTER_C_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), LOCAL_POINTER_C_DECLARATION);

  private final SeqMemoryLocation LOCAL_POINTER_D_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), LOCAL_POINTER_D_DECLARATION);

  private final SeqMemoryLocation GLOBAL_X_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), GLOBAL_X_DECLARATION);

  private final SeqMemoryLocation GLOBAL_Y_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), GLOBAL_Y_DECLARATION);

  private final SeqMemoryLocation LOCAL_Z_MEMORY_LOCATION =
      SeqMemoryLocation.of(
          MPOROptions.getDefaultTestInstance(), Optional.empty(), LOCAL_Z_DECLARATION);

  public MemoryModelTest() throws InvalidConfigurationException {}

  @Test
  public void test_memory_location_equals() throws InvalidConfigurationException {
    // create new MemoryLocation with the same parameters
    SeqMemoryLocation int_pointer_a_memory_location_alt =
        SeqMemoryLocation.of(
            MPOROptions.getDefaultTestInstance(), Optional.empty(), GLOBAL_POINTER_A_DECLARATION);
    // test that .equals returns true
    assertThat(GLOBAL_POINTER_A_MEMORY_LOCATION).isEqualTo(int_pointer_a_memory_location_alt);
    // test that .equals returns false
    assertThat(GLOBAL_X_MEMORY_LOCATION).isNotEqualTo(int_pointer_a_memory_location_alt);
  }

  @Test
  public void test_single_pointer_assignment() {
    // global_ptr_A = &global_X; i.e. pointer assignment
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<SeqMemoryLocation, SeqMemoryLocation>builder()
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, GLOBAL_X_MEMORY_LOCATION)
            .build();

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_A_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            ImmutableMap.of());

    // only memory location of 'global_X' should be associated with dereference of 'global_ptr_A'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations).contains(GLOBAL_X_MEMORY_LOCATION);
  }

  @Test
  public void test_multi_pointer_assignment() {
    // global_ptr_A = &global_X; and global_ptr_A = &global_Y; i.e. pointer assignments
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<SeqMemoryLocation, SeqMemoryLocation>builder()
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, GLOBAL_X_MEMORY_LOCATION)
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, GLOBAL_Y_MEMORY_LOCATION)
            .build();

    // find the memory locations associated with dereference of 'global_ptr_A'
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_A_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            ImmutableMap.of());

    // mem location of 'global_X' and 'global_Y' should be associated with deref of 'global_ptr_A'
    assertThat(memoryLocations).hasSize(2);
    assertThat(memoryLocations).contains(GLOBAL_X_MEMORY_LOCATION);
    assertThat(memoryLocations).contains(GLOBAL_Y_MEMORY_LOCATION);
  }

  @Test
  public void test_transitive_pointer_assignment() {
    // global_ptr_A = &global_X; and global_ptr_B = global_ptr_A;
    // i.e. pointer assignment (transitive)
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<SeqMemoryLocation, SeqMemoryLocation>builder()
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, GLOBAL_X_MEMORY_LOCATION)
            .put(GLOBAL_POINTER_B_MEMORY_LOCATION, GLOBAL_POINTER_A_MEMORY_LOCATION)
            .build();

    // find the memory locations associated with dereference of 'global_ptr_B'
    ImmutableSet<SeqMemoryLocation> memoryLocations =
        SeqMemoryLocationFinder.findMemoryLocationsByPointerDereference(
            GLOBAL_POINTER_B_MEMORY_LOCATION,
            pointerAssignments,
            ImmutableMap.of(),
            ImmutableMap.of());

    // memory location of 'global_X' should be associated with dereference of 'global_ptr_B'
    // even without direct assignment, due to transitive assignment of 'global_ptr_B = global_ptr_A'
    assertThat(memoryLocations).hasSize(1);
    assertThat(memoryLocations).contains(GLOBAL_X_MEMORY_LOCATION);
  }

  @Test
  public void test_implicit_global() {
    // global_ptr_A = &local_Z; i.e. pointer assignment
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<SeqMemoryLocation, SeqMemoryLocation>builder()
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, LOCAL_Z_MEMORY_LOCATION)
            .build();

    // test that local_Z is now an implicit global memory location, because the global pointer
    // 'global_ptr_A' gets its address, and can be dereferenced by other threads
    assertThat(GLOBAL_POINTER_A_MEMORY_LOCATION.declaration().isGlobal()).isTrue();
    assertThat(LOCAL_Z_MEMORY_LOCATION.declaration().isGlobal()).isFalse();
    assertThat(
            MemoryModelBuilder.isImplicitGlobal(
                LOCAL_Z_MEMORY_LOCATION,
                pointerAssignments,
                ImmutableMap.of(),
                ImmutableMap.of(),
                ImmutableSet.of()))
        .isTrue();
  }

  @Test
  public void test_transitive_implicit_global() {
    // 'local_ptr_C = &local_Z;' and 'local_ptr_D = local_ptr_C;' and 'global_ptr_A = local_ptr_D'
    // i.e. transitive pointer assignments
    ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<SeqMemoryLocation, SeqMemoryLocation>builder()
            .put(LOCAL_POINTER_C_MEMORY_LOCATION, LOCAL_Z_MEMORY_LOCATION)
            .put(LOCAL_POINTER_D_MEMORY_LOCATION, LOCAL_POINTER_C_MEMORY_LOCATION)
            .put(GLOBAL_POINTER_A_MEMORY_LOCATION, LOCAL_POINTER_D_MEMORY_LOCATION)
            .build();

    // test that local_Z is now an implicit global memory location, because of transitivity:
    // 'global_ptr_A -> local_ptr_D -> local_ptr_C', and can then be dereferenced by other threads
    assertThat(GLOBAL_POINTER_A_MEMORY_LOCATION.declaration().isGlobal()).isTrue();
    assertThat(LOCAL_POINTER_C_MEMORY_LOCATION.declaration().isGlobal()).isFalse();
    assertThat(LOCAL_POINTER_D_MEMORY_LOCATION.declaration().isGlobal()).isFalse();
    assertThat(LOCAL_Z_MEMORY_LOCATION.declaration().isGlobal()).isFalse();
    assertThat(
            MemoryModelBuilder.isImplicitGlobal(
                LOCAL_Z_MEMORY_LOCATION,
                pointerAssignments,
                ImmutableMap.of(),
                ImmutableMap.of(),
                ImmutableSet.of()))
        .isTrue();
  }
}
