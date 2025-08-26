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
import java.util.Optional;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

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

  /**
   * The initial memory locations do not factor in memory locations that are only used in pointer
   * parameter assignments.
   */
  public static ImmutableSet<MemoryLocation> getInitialMemoryLocations(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    Set<MemoryLocation> rMemoryLocations = new HashSet<>();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rMemoryLocations.addAll(substituteEdge.accessedMemoryLocations);
      rMemoryLocations.addAll(substituteEdge.pointerAssignments.values());
      rMemoryLocations.addAll(substituteEdge.accessedPointerDereferences);
    }
    return ImmutableSet.copyOf(rMemoryLocations);
  }

  static ImmutableSet<MemoryLocation> getPointerDereferencesByAccessType(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<ThreadEdge> pCallContext,
      MPORSubstitutionTracker pTracker,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rPointerDereferences = ImmutableSet.builder();
    for (CSimpleDeclaration pointerDereference :
        pTracker.getPointerDereferencesByAccessType(pAccessType)) {
      MemoryLocation memoryLocation =
          MemoryLocationUtil.buildMemoryLocationByDeclarationScope(
              pOptions, pThread, pCallContext, pointerDereference);
      rPointerDereferences.add(memoryLocation);
    }
    ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
        fieldReferencePointerDereferences =
            pTracker.getFieldReferencePointerDereferencesByAccessType(pAccessType);
    for (CSimpleDeclaration fieldOwner : fieldReferencePointerDereferences.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          fieldReferencePointerDereferences.get(fieldOwner)) {
        MemoryLocation memoryLocation =
            MemoryLocationUtil.buildMemoryLocationByDeclarationScope(
                pOptions, pThread, pCallContext, fieldOwner, fieldMember);
        rPointerDereferences.add(memoryLocation);
      }
    }
    return rPointerDereferences.build();
  }

  static ImmutableSet<MemoryLocation> getMemoryLocationsByAccessType(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<ThreadEdge> pCallContext,
      MPORSubstitutionTracker pTracker,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemoryLocations = ImmutableSet.builder();
    for (CVariableDeclaration variableDeclaration :
        pTracker.getVariablesByAccessType(pAccessType)) {
      MemoryLocation memoryLocation =
          MemoryLocationUtil.buildMemoryLocationByDeclarationScope(
              pOptions, pThread, pCallContext, variableDeclaration);
      rMemoryLocations.add(memoryLocation);
    }
    ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration> fieldMembers =
        pTracker.getFieldMembersByAccessType(pAccessType);
    for (CVariableDeclaration fieldOwner : fieldMembers.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember : fieldMembers.get(fieldOwner)) {
        MemoryLocation memoryLocation =
            MemoryLocationUtil.buildMemoryLocationByDeclarationScope(
                pOptions, pThread, pCallContext, fieldOwner, fieldMember);
        rMemoryLocations.add(memoryLocation);
      }
    }
    return rMemoryLocations.build();
  }

  // Pointer Assignments ===========================================================================

  /**
   * Maps pointers {@code ptr} to the memory locations e.g. {@code &var} assigned to them based on
   * {@code pSubstituteEdges}, including both global and local memory locations.
   */
  public static ImmutableMap<MemoryLocation, MemoryLocation> mapPointerAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<ThreadEdge> pCallContext,
      MPORSubstitutionTracker pTracker) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rAssignments = ImmutableMap.builder();
    for (var entry : pTracker.getPointerAssignments().entrySet()) {
      MemoryLocation leftHandSide = MemoryLocation.of(Optional.empty(), entry.getKey());
      MemoryLocation rightHandSide =
          MemoryLocationUtil.buildMemoryLocationByDeclarationScope(
              pOptions, pThread, pCallContext, entry.getValue());
      rAssignments.put(leftHandSide, rightHandSide);
    }
    ImmutableSet<Cell<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
        cellSet = pTracker.getPointerFieldMemberAssignments().cellSet();
    for (Cell<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration> cell :
        cellSet) {
      MemoryLocation leftHandSide = MemoryLocation.of(Optional.empty(), cell.getRowKey());
      MemoryLocation rightHandSide =
          MemoryLocationUtil.buildMemoryLocationByDeclarationScope(
              pOptions, pThread, pCallContext, cell.getColumnKey(), cell.getValue());
      rAssignments.put(leftHandSide, rightHandSide);
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
