// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;

public class MemoryLocationUtil {

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
