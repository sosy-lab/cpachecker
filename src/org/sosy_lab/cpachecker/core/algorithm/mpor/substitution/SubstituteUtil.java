// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Table.Cell;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SubstituteUtil {

  public static ImmutableList<MPORThread> extractThreads(
      ImmutableList<MPORSubstitution> pSubstitutions) {

    return pSubstitutions.stream()
        .map(MPORSubstitution::getThread)
        .collect(ImmutableList.toImmutableList());
  }

  public static MPORSubstitution extractMainThreadSubstitution(
      ImmutableList<MPORSubstitution> pSubstitutions) {

    return pSubstitutions.stream().filter(s -> s.thread.isMain()).findAny().orElseThrow();
  }

  /** Function and Type declarations are placed outside {@code main()}. */
  public static boolean isExcludedDeclarationEdge(
      MPOROptions pOptions, CDeclarationEdge pDeclarationEdge) {

    CDeclaration declaration = pDeclarationEdge.getDeclaration();
    if (declaration instanceof CFunctionDeclaration) {
      return !pOptions.inputFunctionDeclarations;

    } else if (declaration instanceof CTypeDeclaration) {
      return !pOptions.inputTypeDeclarations;

    } else if (declaration instanceof CVariableDeclaration variableDeclaration) {
      if (!pOptions.inputTypeDeclarations) {
        // if type declarations are excluded, extern variable declarations are excluded too
        return variableDeclaration.getCStorageClass().equals(CStorageClass.EXTERN);
      }
    }
    return false;
  }

  // Memory Locations ==============================================================================

  public static ImmutableSet<MemoryLocation> getAllMemoryLocations(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    Set<MemoryLocation> rMemoryLocations = new HashSet<>();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rMemoryLocations.addAll(substituteEdge.accessedMemoryLocations);
    }
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      for (MemoryLocation memoryLocation : substituteEdge.pointerAssignments.values()) {
        // ensure that all mem locations in pointer assignments are present
        assert rMemoryLocations.contains(memoryLocation)
            : "could not find memoryLocation from pointer assignment in rMemoryLocations";
      }
      for (MemoryLocation memoryLocation : substituteEdge.accessedPointerDereferences) {
        // ensure that all mem locations in pointer dereferences are present
        assert rMemoryLocations.contains(memoryLocation)
            : "could not find memoryLocation from pointer dereference in rMemoryLocations";
      }
    }
    return ImmutableSet.copyOf(rMemoryLocations);
  }

  static ImmutableSet<MemoryLocation> getPointerDereferencesByAccessType(
      MPORSubstitutionTracker pTracker, MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rPointerDereferences = ImmutableSet.builder();
    for (CSimpleDeclaration pointerDereference :
        pTracker.getPointerDereferencesByAccessType(pAccessType)) {
      rPointerDereferences.add(MemoryLocation.of(pointerDereference));
    }
    ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
        fieldReferencePointerDereferences =
            pTracker.getFieldReferencePointerDereferencesByAccessType(pAccessType);
    for (CSimpleDeclaration fieldOwner : fieldReferencePointerDereferences.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          fieldReferencePointerDereferences.get(fieldOwner)) {
        rPointerDereferences.add(MemoryLocation.of(fieldOwner, fieldMember));
      }
    }
    return rPointerDereferences.build();
  }

  static ImmutableSet<MemoryLocation> getMemoryLocationsByAccessType(
      MPORSubstitutionTracker pTracker, MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemoryLocations = ImmutableSet.builder();
    for (CVariableDeclaration variableDeclaration :
        pTracker.getVariablesByAccessType(pAccessType)) {
      rMemoryLocations.add(MemoryLocation.of(variableDeclaration));
    }
    ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration> fieldMembers =
        pTracker.getFieldMembersByAccessType(pAccessType);
    for (CVariableDeclaration fieldOwner : fieldMembers.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember : fieldMembers.get(fieldOwner)) {
        rMemoryLocations.add(MemoryLocation.of(fieldOwner, fieldMember));
      }
    }
    return rMemoryLocations.build();
  }

  // Pointer Assignments ===========================================================================

  /**
   * Maps pointers {@code ptr} to the memory locations e.g. {@code &var} assigned to them based on
   * {@code pSubstituteEdges}, including both global and local memory locations.
   */
  public static ImmutableMap<CVariableDeclaration, MemoryLocation> mapPointerAssignments(
      MPORSubstitutionTracker pTracker) {

    ImmutableMap.Builder<CVariableDeclaration, MemoryLocation> rAssignments =
        ImmutableMap.builder();
    for (var entry : pTracker.getPointerAssignments().entrySet()) {
      rAssignments.put(entry.getKey(), MemoryLocation.of(entry.getValue()));
    }
    ImmutableSet<Cell<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
        cellSet = pTracker.getPointerFieldMemberAssignments().cellSet();
    for (Cell<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration> cell :
        cellSet) {
      rAssignments.put(
          Objects.requireNonNull(cell.getRowKey()),
          MemoryLocation.of(cell.getColumnKey(), cell.getValue()));
    }
    return rAssignments.buildOrThrow();
  }

  // Main Function Arg =============================================================================

  public static ImmutableSet<CParameterDeclaration> findAllMainFunctionArgs(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSet.Builder<CParameterDeclaration> rArgs = ImmutableSet.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rArgs.addAll(substituteEdge.accessedMainFunctionArgs);
    }
    return rArgs.build();
  }
}
