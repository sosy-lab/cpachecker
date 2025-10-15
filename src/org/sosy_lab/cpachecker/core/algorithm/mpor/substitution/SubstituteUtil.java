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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class SubstituteUtil {

  public static SubstituteEdge getSubstituteEdgeByCfaEdgeAndCallContext(
      CFAEdge pCfaEdge,
      Optional<ThreadEdge> pCallContext,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    for (ThreadEdge threadEdge : pSubstituteEdges.keySet()) {
      if (threadEdge.cfaEdge.equals(pCfaEdge)) {
        if (threadEdge.callContext.equals(pCallContext)) {
          return pSubstituteEdges.get(threadEdge);
        }
      }
    }
    throw new IllegalArgumentException(
        String.format(
            "could not find pCfaEdge of type %s and pCallContext in pSubstituteEdges",
            pCfaEdge.getEdgeType()));
  }

  /**
   * Whether {@code pSimpleDeclaration} is a {@link CVariableDeclaration} or {@link
   * CParameterDeclaration}. Other declarations such as {@link CFunctionDeclaration}s are not
   * substituted.
   */
  public static boolean isSubstitutable(CSimpleDeclaration pSimpleDeclaration) {
    return pSimpleDeclaration instanceof CVariableDeclaration
        || pSimpleDeclaration instanceof CParameterDeclaration;
  }

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
  public static ImmutableList<MemoryLocation> getInitialMemoryLocations(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    List<MemoryLocation> rMemoryLocations = new ArrayList<>();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rMemoryLocations.addAll(substituteEdge.accessedMemoryLocations);
      rMemoryLocations.addAll(substituteEdge.pointerAssignments.values());
      rMemoryLocations.addAll(substituteEdge.accessedPointerDereferences);
    }
    // remove duplicates
    return rMemoryLocations.stream().distinct().collect(ImmutableList.toImmutableList());
  }

  static ImmutableSet<MemoryLocation> getPointerDereferencesByAccessType(
      MPOROptions pOptions,
      Optional<ThreadEdge> pCallContext,
      MPORSubstitutionTracker pTracker,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rPointerDereferences = ImmutableSet.builder();
    for (CSimpleDeclaration pointerDereference :
        pTracker.getPointerDereferencesByAccessType(pAccessType)) {
      rPointerDereferences.add(MemoryLocation.of(pOptions, pCallContext, pointerDereference));
    }
    ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
        fieldReferencePointerDereferences =
            pTracker.getFieldReferencePointerDereferencesByAccessType(pAccessType);
    for (CSimpleDeclaration fieldOwner : fieldReferencePointerDereferences.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          fieldReferencePointerDereferences.get(fieldOwner)) {
        rPointerDereferences.add(
            MemoryLocation.of(pOptions, pCallContext, fieldOwner, fieldMember));
      }
    }
    return rPointerDereferences.build();
  }

  static ImmutableSet<MemoryLocation> getMemoryLocationsByAccessType(
      MPOROptions pOptions,
      Optional<ThreadEdge> pCallContext,
      MPORSubstitutionTracker pTracker,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemoryLocations = ImmutableSet.builder();
    for (CSimpleDeclaration declaration : pTracker.getDeclarationsByAccessType(pAccessType)) {
      rMemoryLocations.add(MemoryLocation.of(pOptions, pCallContext, declaration));
    }
    ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration> fieldMembers =
        pTracker.getFieldMembersByAccessType(pAccessType);
    for (CSimpleDeclaration fieldOwner : fieldMembers.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember : fieldMembers.get(fieldOwner)) {
        rMemoryLocations.add(MemoryLocation.of(pOptions, pCallContext, fieldOwner, fieldMember));
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
      MPOROptions pOptions, Optional<ThreadEdge> pCallContext, MPORSubstitutionTracker pTracker) {

    ImmutableMap.Builder<MemoryLocation, MemoryLocation> rAssignments = ImmutableMap.builder();
    for (var entry : pTracker.getPointerAssignments().entrySet()) {
      MemoryLocation leftHandSide = MemoryLocation.of(pOptions, pCallContext, entry.getKey());
      MemoryLocation rightHandSide = MemoryLocation.of(pOptions, pCallContext, entry.getValue());
      rAssignments.put(leftHandSide, rightHandSide);
    }
    for (var cell : pTracker.getPointerFieldMemberAssignments().cellSet()) {
      MemoryLocation leftHandSide = MemoryLocation.of(pOptions, pCallContext, cell.getRowKey());
      MemoryLocation rightHandSide =
          MemoryLocation.of(pOptions, pCallContext, cell.getColumnKey(), cell.getValue());
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
