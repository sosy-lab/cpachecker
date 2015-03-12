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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
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
   * the path for which to compute the interpolants
   */
  private final ARGPath path;

  public UseDefBasedInterpolator(ARGPath pPath, UseDefRelation pUseDefRelation) {
    path = pPath;
    useDefRelation = pUseDefRelation;
  }

  /**
   * This method obtains the mapping from {@link ARGState}s to {@link ValueAnalysisInterpolant}s.
   *
   * This method iterates over the path, building a use-def-relation that is seeded by the identifiers
   * that occur in the last (in iteration order, the first) assume edge of the path. Hence, this
   * interpolation approach only works if the given path is an (infeasible) sliced prefix, obtained
   * via {@link ErrorPathClassifier#obtainSlicedPrefix}.
   *
   * @param path the path (i.e., infeasible sliced prefix) for which to obtain the interpolants
   * @return the mapping mapping from {@link ARGState}s to {@link ValueAnalysisInterpolant}s
   */
  public Map<ARGState, ValueAnalysisInterpolant> obtainInterpolants() {

    Map<ARGState, ValueAnalysisInterpolant> interpolants = new LinkedHashMap<>();

    int update = 0;
    HashMap<MemoryLocation, Value> rawItp = new HashMap<>();

    List<CFAEdge> edges = path.getInnerEdges();
    List<ARGState> states = path.asStatesList();
    for (int i = edges.size() - 1; i >= 0; i--) {
      CFAEdge edge = edges.get(i);

      Collection<ASimpleDeclaration> defs;
      Collection<ASimpleDeclaration> uses;

      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
        for(CFAEdge singleEdge : ((MultiEdge)edge).getEdges()) {
          defs = useDefRelation.getDef(states.get(i), singleEdge);
          uses = useDefRelation.getUses(states.get(i), singleEdge);

          for(ASimpleDeclaration use : uses) {
            rawItp.put(MemoryLocation.valueOf(use.getQualifiedName()), new NumericValue(update++));
          }

          for(ASimpleDeclaration def : defs) {
            rawItp.remove(MemoryLocation.valueOf(def.getQualifiedName()));
          }
        }
      }
      else {
        defs = useDefRelation.getDef(states.get(i), edge);
        uses = useDefRelation.getUses(states.get(i), edge);

        for(ASimpleDeclaration use : uses) {
          rawItp.put(MemoryLocation.valueOf(use.getQualifiedName()), new NumericValue(update++));
        }

        for(ASimpleDeclaration def : defs) {
          rawItp.remove(MemoryLocation.valueOf(def.getQualifiedName()));
        }
      }

      if(rawItp.isEmpty()) {
        interpolants.put(states.get(i), ValueAnalysisInterpolant.TRUE);
      }
      else {
        interpolants.put(states.get(i), new ValueAnalysisInterpolant(new HashMap<>(rawItp), Collections.<MemoryLocation, Type>emptyMap()));
      }
    }

    interpolants.put(states.get(0), ValueAnalysisInterpolant.TRUE);
    return interpolants;
  }
}