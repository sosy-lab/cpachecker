/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.harness;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.Formula;

public class HarnessState extends AbstractSingleWrapperState implements AbstractState {

  private static final long serialVersionUID = 1L;
  private final ImmutableList<Formula> orderedExternallyKnownLocations;
  private Map<AFunctionDeclaration, List<Integer>> functionToIndicesMap;

  public HarnessState(AbstractState pWrappedState) {
    super(pWrappedState);
    orderedExternallyKnownLocations = ImmutableList.of();
    functionToIndicesMap = new HashMap<>();
  }

  public HarnessState(
      AbstractState pWrappedState,
      ImmutableList<Formula> pOrderedExternallyKnownLocations) {
    super(pWrappedState);
    orderedExternallyKnownLocations = pOrderedExternallyKnownLocations;
    functionToIndicesMap = new HashMap<>();
  }

  public HarnessState(
      AbstractState pWrappedSuccessor,
      ImmutableList<Formula> pOrderedExternallyKnownLocations,
      Map<AFunctionDeclaration, List<Integer>> pFunctionToIndicesMap) {
    super(pWrappedSuccessor);
    orderedExternallyKnownLocations = pOrderedExternallyKnownLocations;
    functionToIndicesMap = pFunctionToIndicesMap;
  }

  public List<Formula> getExternallyKnownPointers() {
    return orderedExternallyKnownLocations;
  }

  public int getExternPointersArrayLength() {
    return orderedExternallyKnownLocations.size();
  }


  public HarnessState setWrappedState(AbstractState pWrappedSuccessor) {
    return new HarnessState(
        pWrappedSuccessor,
        orderedExternallyKnownLocations,
        functionToIndicesMap);
  }

  public void setIndices(Map<AFunctionDeclaration, List<Integer>> pIndicesOfFunctions) {
    functionToIndicesMap = pIndicesOfFunctions;
  }

  public Map<AFunctionDeclaration, List<Integer>> getFunctionToIndicesMap() {
    return functionToIndicesMap;
  }

  public HarnessState addExternallyKnownLocations(List<Formula> pFormulas) {
    Builder<Formula> builder = ImmutableList.builder();
    builder.addAll(orderedExternallyKnownLocations);
    builder.addAll(pFormulas);
    ImmutableList<Formula> newLocations = builder.build();
    return new HarnessState(getWrappedState(), newLocations);
  }

}
