// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryLocationUtil {

  public static MemoryLocation buildMemoryLocationByDeclarationScope(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<ThreadEdge> pCallContext,
      CSimpleDeclaration pDeclaration) {

    if (pDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (!variableDeclaration.isGlobal()) {
        return MemoryLocation.of(pOptions, pThread, pCallContext, pDeclaration);
      }
    }
    // treat all parameter declarations as local
    if (pDeclaration instanceof CParameterDeclaration) {
      return MemoryLocation.of(pOptions, pThread, pCallContext, pDeclaration);
    }
    return MemoryLocation.of(pCallContext, pDeclaration);
  }

  public static MemoryLocation buildMemoryLocationByDeclarationScope(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<ThreadEdge> pCallContext,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember) {

    if (pFieldOwner instanceof CVariableDeclaration variableDeclaration) {
      if (!variableDeclaration.isGlobal()) {
        return MemoryLocation.of(pOptions, pThread, pCallContext, pFieldOwner, pFieldMember);
      }
    }
    // treat all parameter declarations as local
    if (pFieldOwner instanceof CParameterDeclaration) {
      return MemoryLocation.of(pOptions, pThread, pCallContext, pFieldOwner, pFieldMember);
    }
    return MemoryLocation.of(pCallContext, pFieldOwner, pFieldMember);
  }

  static boolean isGlobal(
      Optional<CSimpleDeclaration> pVariable,
      Optional<SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
          pFieldMember) {

    if (pVariable.isPresent()) {
      if (pVariable.orElseThrow() instanceof CVariableDeclaration variableDeclaration) {
        return variableDeclaration.isGlobal();
      }
    }
    if (pFieldMember.isPresent()) {
      if (pFieldMember.orElseThrow().getKey() instanceof CVariableDeclaration variableDeclaration) {
        return variableDeclaration.isGlobal();
      }
    }
    return false;
  }

  static boolean isParameter(
      Optional<CSimpleDeclaration> pVariable,
      Optional<SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>>
          pFieldMember) {

    if (pVariable.isPresent()) {
      if (pVariable.orElseThrow() instanceof CParameterDeclaration) {
        return true;
      }
    }
    if (pFieldMember.isPresent()) {
      if (pFieldMember.orElseThrow().getKey() instanceof CParameterDeclaration) {
        return true;
      }
    }
    return false;
  }

  static boolean isConstCpaCheckerTmp(MemoryLocation pMemoryLocation) {
    if (pMemoryLocation.getSimpleDeclaration()
        instanceof CVariableDeclaration variableDeclaration) {
      return MPORUtil.isConstCpaCheckerTmp(variableDeclaration);
    }
    return false;
  }
}
