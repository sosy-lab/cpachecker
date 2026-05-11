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
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;

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
      substituteEdge
          .getPointerAssignments()
          .forEach(a -> rMemoryLocations.add(a.rightHandSideMemoryLocation()));
      rMemoryLocations.addAll(substituteEdge.accessedPointerDereferences);
    }
    // remove duplicates
    return rMemoryLocations.stream().distinct().collect(ImmutableList.toImmutableList());
  }

  static ImmutableSet<SeqMemoryLocation> getPointerDereferencesByAccessType(
      MPORSubstitutionTracker pTracker, SeqMemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rPointerDereferences = ImmutableSet.builder();
    rPointerDereferences.addAll(pTracker.getPointerDereferencesByAccessType(pAccessType));
    rPointerDereferences.addAll(
        pTracker.getFieldReferencePointerDereferencesByAccessType(pAccessType));
    return rPointerDereferences.build();
  }

  static ImmutableSet<SeqMemoryLocation> getMemoryLocationsByAccessType(
      MPORSubstitutionTracker pTracker, SeqMemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemoryLocations = ImmutableSet.builder();
    rMemoryLocations.addAll(pTracker.getDeclarationsByAccessType(pAccessType));
    rMemoryLocations.addAll(pTracker.getFieldMembersByAccessType(pAccessType));
    return rMemoryLocations.build();
  }
}
