// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

class CAstNodeSubstitution {

  private final Map<CFAEdge, Map<CAstNode, CAstNode>> substitution;

  CAstNodeSubstitution() {
    substitution = new HashMap<>();
  }

  void insertSubstitute(CFAEdge pEdge, CAstNode pOriginal, CAstNode pSubstitute) {
    substitution.computeIfAbsent(pEdge, key -> new HashMap<>()).put(pOriginal, pSubstitute);
  }

  CAstNode getSubstitute(CFAEdge pEdge, CAstNode pOriginal) {
    return substitution.getOrDefault(pEdge, ImmutableMap.of()).get(pOriginal);
  }
}
