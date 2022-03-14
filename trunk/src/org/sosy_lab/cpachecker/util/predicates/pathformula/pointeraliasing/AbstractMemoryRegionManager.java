// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.Pair;

abstract class AbstractMemoryRegionManager implements MemoryRegionManager {
  private final Map<Pair<CFAEdge, String>, Integer> targetStats = new HashMap<>();

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
  public void addTargetToStats(CFAEdge pEdge, String pUfName, PointerTarget pTarget) {
    Pair<CFAEdge, String> key = Pair.of(pEdge, pUfName);
    Integer v = targetStats.get(key);
    if (v != null) {
      targetStats.put(key, v + 1);
    } else {
      targetStats.put(key, 1);
    }
  }

  @Override
  public void printStatistics(PrintStream out) {
    int totalTargets = 0;
    Map<String, Integer> perUf = new HashMap<>();
    for (Map.Entry<Pair<CFAEdge, String>, Integer> entry : targetStats.entrySet()) {
      String key = entry.getKey().getSecond();
      Integer v = entry.getValue();
      if (v != null) {
        perUf.put(key, v + 1);
        totalTargets += v;
      } else {
        perUf.put(key, 1);
      }
    }
    out.println("Total number of created targets for pointer analysis: " + totalTargets);
    for (Map.Entry<String, Integer> entry : perUf.entrySet()) {
      out.println(
          "   Number of created targets for uf: " + entry.getKey() + " is " + entry.getValue());
    }
    out.println();
  }
}
