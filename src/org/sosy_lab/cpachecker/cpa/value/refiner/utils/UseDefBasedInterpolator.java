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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This class allows to obtain interpolants statically from a given ARGPath.
 */
public class UseDefBasedInterpolator {

  /**
   * the use-def relation of the final, failing (assume) edge
   */
  private final UseDefRelation useDefRelation;

  /**
   * the sliced infeasible prefix for which to compute the interpolants
   */
  private final ARGPath slicedPrefix;

  /**
   * This class allows the creation of (fake) interpolants by using the use-def-relation.
   * This interpolation approach only works if the given path is a sliced prefix,
   * obtained via {@link ErrorPathClassifier#obtainSlicedPrefix}.
   *
   * @param pSlicedPrefix
   * @param pUseDefRelation
   */
  public UseDefBasedInterpolator(ARGPath pSlicedPrefix, UseDefRelation pUseDefRelation) {
    slicedPrefix   = pSlicedPrefix;
    useDefRelation = pUseDefRelation;
  }

  /**
   * This method obtains the mapping from {@link ARGState}s to {@link ValueAnalysisInterpolant}s.
   *
   * @return the (ordered) mapping mapping from {@link ARGState}s to {@link ValueAnalysisInterpolant}s
   */
  public Map<ARGState, ValueAnalysisInterpolant> obtainInterpolants() {
    Map<ARGState, Collection<ASimpleDeclaration>> useDefSequence = useDefRelation.getExpandedUses(slicedPrefix);
    ValueAnalysisInterpolant trivialItp = ValueAnalysisInterpolant.FALSE;

    ArrayDeque<Pair<ARGState, ValueAnalysisInterpolant>> interpolants = new ArrayDeque<>();
    PathIterator iterator = slicedPrefix.reversePathIterator();
    while (iterator.hasNext()) {
      ARGState state = iterator.getAbstractState();

      Collection<ASimpleDeclaration> uses = useDefSequence.get(state);

      ValueAnalysisInterpolant interpolant = uses.isEmpty()
          ? trivialItp
          : createInterpolant(uses);

      interpolants.addFirst(Pair.of(state, interpolant));

      // as the traversal goes backwards, once the interpolant was non-trivial once,
      // the next time it is trivial, it has to be TRUE, and no longer FALSE
      if (interpolant != trivialItp) {
        trivialItp = ValueAnalysisInterpolant.TRUE;
      }

      iterator.advance();
    }

    return convertToLinkedMap(interpolants);
  }

  private ValueAnalysisInterpolant createInterpolant(Collection<ASimpleDeclaration> uses) {
    HashMap<MemoryLocation, Value> useDefInterpolant = new HashMap<>();
    for(ASimpleDeclaration use : uses) {
      useDefInterpolant.put(MemoryLocation.valueOf(use.getQualifiedName()), UnknownValue.getInstance());
    }

    return new ValueAnalysisInterpolant(useDefInterpolant, Collections.<MemoryLocation, Type>emptyMap());
  }

  private Map<ARGState, ValueAnalysisInterpolant> convertToLinkedMap(
      ArrayDeque<Pair<ARGState, ValueAnalysisInterpolant>> itps) {
    Map<ARGState, ValueAnalysisInterpolant> interpolants = new LinkedHashMap<>();
    for(Pair<ARGState, ValueAnalysisInterpolant> itp : itps) {
      interpolants.put(itp.getFirst(), itp.getSecond());
    }
    return interpolants;
  }
}