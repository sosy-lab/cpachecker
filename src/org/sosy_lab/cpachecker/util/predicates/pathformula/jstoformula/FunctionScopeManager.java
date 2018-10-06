/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/** Creates function scope IDs and stores them by qualified function name. */
class FunctionScopeManager {
  private final Map<String, List<Long>> scopeMap;
  private long scopeCounter;

  FunctionScopeManager() {
    scopeMap = new HashMap<>();
    scopeCounter = 0;
  }

  long createScope(final String pCalledFunctionName) {
    final long newScopeId = ++scopeCounter;
    addScopeId(pCalledFunctionName, newScopeId);
    return newScopeId;
  }

  private void addScopeId(final String pCalledFunctionName, final long pScopeId) {
    getModifiableScopeIds(pCalledFunctionName).add(pScopeId);
  }

  @Nonnull
  private List<Long> getModifiableScopeIds(final String pCalledFunctionName) {
    return scopeMap.computeIfAbsent(pCalledFunctionName, (pKey) -> new ArrayList<>());
  }

  @Nonnull
  List<Long> getScopeIds(final String pCalledFunctionName) {
    return Collections.unmodifiableList(getModifiableScopeIds(pCalledFunctionName));
  }
}
