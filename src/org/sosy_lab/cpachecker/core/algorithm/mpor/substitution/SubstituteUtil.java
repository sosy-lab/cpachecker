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
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
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

  public static ImmutableList<CVariableDeclaration> getAllGlobalVariables(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    return pSubstituteEdges.stream()
        .flatMap(s -> s.accessedGlobalVariables.stream())
        .distinct() // ensure that each variable present only once
        .collect(ImmutableList.toImmutableList());
  }

  // Pointer Assignments ===========================================================================

  /**
   * Maps pointers {@code ptr} to the memory locations e.g. {@code &var} assigned to them based on
   * {@code pSubstituteEdges}, including both global and local memory locations.
   */
  public static ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration>
      mapPointerAssignments(ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    // step 1: map pointers to memory locations assigned to them, including other pointers
    ImmutableSetMultimap.Builder<CVariableDeclaration, CSimpleDeclaration> initialBuilder =
        ImmutableSetMultimap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      if (!substituteEdge.pointerAssignment.isEmpty()) {
        assert substituteEdge.pointerAssignment.size() <= 1
            : "a single edge can have at most 1 pointer assignments";
        Map.Entry<CVariableDeclaration, CSimpleDeclaration> singleEntry =
            substituteEdge.pointerAssignment.entrySet().iterator().next();
        initialBuilder.put(singleEntry.getKey(), singleEntry.getValue());
      }
    }
    ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> initialMap =
        initialBuilder.build();
    // step 2: update the map so that only non-pointer variables are in the values
    ImmutableSetMultimap.Builder<CVariableDeclaration, CSimpleDeclaration> rFinal =
        ImmutableSetMultimap.builder();
    for (var entry : initialMap.entries()) {
      rFinal.putAll(
          entry.getKey(),
          findAllVariablesAssignedToPointer(entry.getKey(), initialMap, new HashSet<>()));
    }
    return rFinal.build();
  }

  private static ImmutableSet<CSimpleDeclaration> findAllVariablesAssignedToPointer(
      CSimpleDeclaration pPointer,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      Set<CVariableDeclaration> pVisitedPointers) {

    ImmutableSet.Builder<CSimpleDeclaration> rLocations = ImmutableSet.builder();
    if (pPointer instanceof CVariableDeclaration pointerVariable) {
      if (pPointerAssignments.containsKey(pointerVariable)) {
        for (CSimpleDeclaration simpleDeclaration : pPointerAssignments.get(pointerVariable)) {
          if (simpleDeclaration.getType() instanceof CPointerType) {
            if (pVisitedPointers.add(pointerVariable)) {
              // for pointers, recursively find all variables assigned to the pointer
              rLocations.addAll(
                  findAllVariablesAssignedToPointer(
                      simpleDeclaration, pPointerAssignments, pVisitedPointers));
            }
          } else {
            // for non-pointer variables, just add the variable itself
            rLocations.add(simpleDeclaration);
          }
        }
      }
    }
    return rLocations.build();
  }

  public static ImmutableSet<CParameterDeclaration> findAllMainFunctionArgs(
      ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    ImmutableSet.Builder<CParameterDeclaration> rArgs = ImmutableSet.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      rArgs.addAll(substituteEdge.accessedMainFunctionArgs);
    }
    return rArgs.build();
  }
}
