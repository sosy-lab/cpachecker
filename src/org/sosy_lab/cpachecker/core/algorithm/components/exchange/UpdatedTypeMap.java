// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange;

import com.google.common.collect.ForwardingConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public class UpdatedTypeMap extends ForwardingConcurrentMap<String, CType> {

  private final ConcurrentMap<String, CType> types;

  public UpdatedTypeMap(SSAMap pMap) {
    types = new ConcurrentHashMap<>();
    merge(pMap);
  }

  public void merge(SSAMap pMap) {
    for (String variable : pMap.allVariables()) {
      types.put(variable, pMap.getType(variable));
    }
  }

  @Override
  protected ConcurrentMap<String, CType> delegate() {
    return types;
  }
}
