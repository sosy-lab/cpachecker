// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.io.PrintStream;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

abstract class AbstractMemoryRegionManager implements MemoryRegionManager {
  private final Multiset<String> targetStats = HashMultiset.create();

  private final TypeHandlerWithPointerAliasing typeHandler;

  AbstractMemoryRegionManager(TypeHandlerWithPointerAliasing pTypeHandler) {
    typeHandler = pTypeHandler;
  }

  @Override
  public MemoryRegion makeMemoryRegion(
      CType pFieldOwnerType, CCompositeTypeMemberDeclaration pMember) {
    return makeMemoryRegion(
        pFieldOwnerType, typeHandler.getSimplifiedType(pMember), pMember.getName());
  }

  @Override
  public final String getPointerAccessName(MemoryRegion pRegion) {
    checkNotNull(pRegion);
    return pRegion.getName(typeHandler);
  }

  @Override
  public void addTargetToStats(String pUfName) {
    targetStats.add(pUfName);
  }

  @Override
  public void printStatistics(PrintStream out) {
    if (!targetStats.isEmpty()) {
      out.println("Number of created pointer-UF targets:     " + targetStats.size());
      for (Multiset.Entry<String> entry : targetStats.entrySet()) {
        out.println(
            "  Number of created targets for UF " + entry.getElement() + ": " + entry.getCount());
      }
    }
    out.println();
  }
}
