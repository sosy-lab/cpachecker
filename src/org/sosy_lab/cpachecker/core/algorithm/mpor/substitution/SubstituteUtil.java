// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

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
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public class SubstituteUtil {

  /**
   * Whether {@code pSimpleDeclaration} is a {@link CVariableDeclaration} or {@link
   * CParameterDeclaration}. Other declarations such as {@link CFunctionDeclaration}s are not
   * substituted.
   */
  static boolean isSubstitutable(CSimpleDeclaration pSimpleDeclaration) {
    return pSimpleDeclaration instanceof CVariableDeclaration
        || pSimpleDeclaration instanceof CParameterDeclaration;
  }

  static CVariableDeclaration asVariableDeclaration(CSimpleDeclaration pSimpleDeclaration) {
    checkArgument(isSubstitutable(pSimpleDeclaration));
    if (pSimpleDeclaration instanceof CVariableDeclaration variableDeclaration) {
      return variableDeclaration;
    }
    return ((CParameterDeclaration) pSimpleDeclaration).asVariableDeclaration();
  }

  public static MPORSubstitution extractMainThreadSubstitution(
      ImmutableList<MPORSubstitution> pSubstitutions) {

    return pSubstitutions.stream().filter(s -> s.thread.isMain()).findAny().orElseThrow();
  }

  /** Function and Type declarations are placed outside {@code main()}. */
  static boolean isExcludedDeclarationEdge(
      MPOROptions pOptions, CDeclarationEdge pDeclarationEdge) {

    CDeclaration declaration = pDeclarationEdge.getDeclaration();
    if (declaration instanceof CFunctionDeclaration) {
      return !pOptions.inputFunctionDeclarations();

    } else if (declaration instanceof CTypeDeclaration) {
      return !pOptions.inputTypeDeclarations();

    } else if (declaration instanceof CVariableDeclaration variableDeclaration) {
      if (!pOptions.inputTypeDeclarations()) {
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
  public static ImmutableList<SeqMemoryLocation> getInitialMemoryLocations(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    List<SeqMemoryLocation> rMemoryLocations = new ArrayList<>();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rMemoryLocations.addAll(substituteEdge.accessedMemoryLocations);
      rMemoryLocations.addAll(substituteEdge.pointerAssignments.values());
      rMemoryLocations.addAll(substituteEdge.accessedPointerDereferences);
    }
    // remove duplicates
    return rMemoryLocations.stream().distinct().collect(ImmutableList.toImmutableList());
  }

  static ImmutableSet<SeqMemoryLocation> getPointerDereferencesByAccessType(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      MPORSubstitutionTracker pTracker,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rPointerDereferences = ImmutableSet.builder();
    for (CVariableDeclaration pointerDereference :
        pTracker.getPointerDereferencesByAccessType(pAccessType)) {
      rPointerDereferences.add(SeqMemoryLocation.of(pOptions, pCallContext, pointerDereference));
    }
    ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
        fieldReferencePointerDereferences =
            pTracker.getFieldReferencePointerDereferencesByAccessType(pAccessType);
    for (CVariableDeclaration fieldOwner : fieldReferencePointerDereferences.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          fieldReferencePointerDereferences.get(fieldOwner)) {
        rPointerDereferences.add(
            SeqMemoryLocation.of(pOptions, pCallContext, fieldOwner, fieldMember));
      }
    }
    return rPointerDereferences.build();
  }

  static ImmutableSet<SeqMemoryLocation> getMemoryLocationsByAccessType(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      MPORSubstitutionTracker pTracker,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemoryLocations = ImmutableSet.builder();
    for (CVariableDeclaration declaration : pTracker.getDeclarationsByAccessType(pAccessType)) {
      rMemoryLocations.add(SeqMemoryLocation.of(pOptions, pCallContext, declaration));
    }
    ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration> fieldMembers =
        pTracker.getFieldMembersByAccessType(pAccessType);
    for (CVariableDeclaration fieldOwner : fieldMembers.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember : fieldMembers.get(fieldOwner)) {
        rMemoryLocations.add(SeqMemoryLocation.of(pOptions, pCallContext, fieldOwner, fieldMember));
      }
    }
    return rMemoryLocations.build();
  }

  // Pointer Assignments ===========================================================================

  /**
   * Maps pointers {@code ptr} to the memory locations e.g. {@code &var} assigned to them based on
   * {@code substituteEdges}, including both global and local memory locations.
   */
  static ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> mapPointerAssignments(
      MPOROptions pOptions,
      Optional<CFAEdgeForThread> pCallContext,
      MPORSubstitutionTracker pTracker) {

    ImmutableMap.Builder<SeqMemoryLocation, SeqMemoryLocation> rAssignments =
        ImmutableMap.builder();
    for (var entry : pTracker.getPointerAssignments().entrySet()) {
      SeqMemoryLocation leftHandSide = SeqMemoryLocation.of(pOptions, pCallContext, entry.getKey());
      SeqMemoryLocation rightHandSide =
          SeqMemoryLocation.of(pOptions, pCallContext, entry.getValue());
      rAssignments.put(leftHandSide, rightHandSide);
    }
    for (var cell : pTracker.getPointerFieldMemberAssignments().cellSet()) {
      SeqMemoryLocation leftHandSide =
          SeqMemoryLocation.of(pOptions, pCallContext, cell.getRowKey());
      SeqMemoryLocation rightHandSide =
          SeqMemoryLocation.of(pOptions, pCallContext, cell.getColumnKey(), cell.getValue());
      rAssignments.put(leftHandSide, rightHandSide);
    }
    return rAssignments.buildOrThrow();
  }

  // Main Function Arg =============================================================================

  public static ImmutableSet<CVariableDeclaration> findAllMainFunctionArgs(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSet.Builder<CVariableDeclaration> rArgs = ImmutableSet.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rArgs.addAll(substituteEdge.accessedMainFunctionArgs);
    }
    return rArgs.build();
  }
}
