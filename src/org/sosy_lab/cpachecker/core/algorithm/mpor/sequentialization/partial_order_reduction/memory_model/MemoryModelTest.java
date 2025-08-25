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
import com.google.common.collect.ImmutableTable;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

public class MemoryModelTest {

  // Types

  private final CSimpleType int_type =
      new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType int_pointer_type = new CPointerType(false, false, int_type);

  // Expressions

  private final CIntegerLiteralExpression int_0 =
      new CIntegerLiteralExpression(FileLocation.DUMMY, int_type, BigInteger.valueOf(0));

  // Initializers

  private final CInitializer int_0_initializer =
      new CInitializerExpression(FileLocation.DUMMY, int_0);

  // Declarations

  private final CVariableDeclaration int_pointer_declaration =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          true,
          CStorageClass.AUTO,
          int_pointer_type,
          "int_ptr",
          "int_ptr",
          "int_ptr",
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

  // Memory Locations

  private final MemoryLocation int_pointer_memory_location =
      MemoryLocation.of(Optional.empty(), int_pointer_declaration);

  private final MemoryLocation int_a_memory_location =
      MemoryLocation.of(Optional.empty(), int_a_declaration);

  private final MemoryLocation int_b_memory_location =
      MemoryLocation.of(Optional.empty(), int_b_declaration);

  @Test
  public void test_single_pointer_assignment() {
    // first create necessary collections
    ImmutableMap<MemoryLocation, Integer> memoryLocationIds =
        ImmutableMap.<MemoryLocation, Integer>builder()
            .put(int_pointer_memory_location, 0)
            .put(int_a_memory_location, 1)
            .buildOrThrow();
    // int_ptr = &int_A; i.e. pointer assignment
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(int_pointer_declaration, int_a_memory_location)
            .build();
    // *int_ptr i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(int_pointer_memory_location).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memoryLocationIds, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'int_ptr'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            int_pointer_memory_location, Optional.empty(), testMemoryModel);

    // only memory location of 'int_A' should be associated with dereference of 'int_ptr'
    assertThat(memoryLocations.size() == 1).isTrue();
    assertThat(memoryLocations.contains(int_a_memory_location)).isTrue();
  }

  @Test
  public void test_multi_pointer_assignment() {
    // first create necessary collections
    ImmutableMap<MemoryLocation, Integer> memoryLocationIds =
        ImmutableMap.<MemoryLocation, Integer>builder()
            .put(int_pointer_memory_location, 0)
            .put(int_a_memory_location, 1)
            .put(int_b_memory_location, 2)
            .buildOrThrow();
    // int_ptr = &int_A; i.e. pointer assignment
    ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments =
        ImmutableSetMultimap.<CVariableDeclaration, MemoryLocation>builder()
            .put(int_pointer_declaration, int_a_memory_location)
            .put(int_pointer_declaration, int_b_memory_location)
            .build();
    // *int_ptr i.e. pointer dereference
    ImmutableSet<MemoryLocation> pointerDereferences =
        ImmutableSet.<MemoryLocation>builder().add(int_pointer_memory_location).build();

    // create memory model
    MemoryModel testMemoryModel =
        new MemoryModel(
            memoryLocationIds, pointerAssignments, ImmutableTable.of(), pointerDereferences);

    // find the memory locations associated with dereference of 'int_ptr'
    ImmutableSet<MemoryLocation> memoryLocations =
        MemoryLocationFinder.findMemoryLocationsByPointerDereference(
            int_pointer_memory_location, Optional.empty(), testMemoryModel);

    // memory location of 'int_A' and 'int_B' should be associated with dereference of 'int_ptr'
    assertThat(memoryLocations.size() == 2).isTrue();
    assertThat(memoryLocations.contains(int_a_memory_location)).isTrue();
    assertThat(memoryLocations.contains(int_b_memory_location)).isTrue();
  }
}
