// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryLocationUtil {

  public static boolean isMemoryLocationReachableByThread(
      MemoryLocation pMemoryLocation,
      MemoryModel pMemoryModel,
      MPORThread pThread,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      MemoryAccessType pAccessType) {

    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
      ImmutableSet<MemoryLocation> memoryLocations =
          MemoryLocationFinder.findMemoryLocationsBySubstituteEdge(
              substituteEdge, pMemoryModel, pAccessType);
      if (memoryLocations.contains(pMemoryLocation)) {
        return true;
      }
    }
    return false;
  }

  static boolean isExplicitGlobal(CSimpleDeclaration pDeclaration) {
    if (pDeclaration instanceof CVariableDeclaration variableDeclaration) {
      return variableDeclaration.isGlobal();
    }
    return false;
  }

  static boolean isConstCpaCheckerTmp(MemoryLocation pMemoryLocation) {
    if (pMemoryLocation.declaration instanceof CVariableDeclaration variableDeclaration) {
      return MPORUtil.isConstCpaCheckerTmp(variableDeclaration);
    }
    return false;
  }
}
