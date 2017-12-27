/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CFAEdgeWithAdditionalInfo {
  private final Map<String, ImmutableSet<Object>> addinitonalInfo;
  private final CFAEdge edge;

  CFAEdgeWithAdditionalInfo(Map<String, ImmutableSet<Object>> pAddinitonalInfo, CFAEdge pEdge) {
    addinitonalInfo = new HashMap<>(pAddinitonalInfo);
    edge = Objects.requireNonNull(pEdge);
  }

  public CFAEdgeWithAdditionalInfo(
      CFAEdgeWithAdditionalInfo pEdge1,
      CFAEdgeWithAdditionalInfo pEdge2) {
    assert pEdge1.edge.equals(pEdge2.edge);

    /*
     * Constructor used when merging to edges.
     */
    edge = pEdge1.edge;
    addinitonalInfo = Stream.concat(pEdge1.addinitonalInfo.entrySet().stream(),
        pEdge2.addinitonalInfo.entrySet().stream()).collect(Collectors.toMap(
            entry -> entry.getKey(),
            entry -> entry.getValue(),
            (set1, set2) -> ImmutableSet.builder().addAll(set1).addAll(set2).build()));
  }

  public static CFAEdgeWithAdditionalInfo of(CFAEdge pEdge) {
    return new CFAEdgeWithAdditionalInfo(new HashMap<>(), pEdge);
  }

  public void addInfo(String tag, Object value) {
    mergeInfos(tag, ImmutableSet.of(value));
  }

  public void mergeInfos(String tag, ImmutableSet<Object> addInfos) {
    addinitonalInfo.merge(tag, addInfos, (v1, v2) -> ImmutableSet.builder().addAll
        (v1).addAll(v2).build());

  }

  CFAEdgeWithAdditionalInfo mergeEdge(CFAEdgeWithAdditionalInfo pEdge) {
    return new CFAEdgeWithAdditionalInfo(this, pEdge);
  }

  public CFAEdge getCFAEdge() {
    return edge;
  }

  public Set<Entry<String,ImmutableSet<Object>>> getInfos() {
    return addinitonalInfo.entrySet();
  }
}
