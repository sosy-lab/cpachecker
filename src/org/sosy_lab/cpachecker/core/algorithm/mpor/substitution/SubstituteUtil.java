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
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SubstituteUtil {

  public static ImmutableSet<MPORThread> extractThreads(
      ImmutableList<MPORSubstitution> pSubstitutions) {

    return pSubstitutions.stream()
        .map(MPORSubstitution::getThread)
        .collect(ImmutableSet.toImmutableSet());
  }

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
        .flatMap(s -> s.getGlobalVariables().stream())
        .distinct() // ensure that each variable present only once
        .collect(ImmutableList.toImmutableList());
  }
}
