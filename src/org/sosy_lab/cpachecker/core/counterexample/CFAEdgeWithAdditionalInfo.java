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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.ConvertingTags;

/**
 * Contains additional info for a given statement, which is represented as cfa edge {@link CFAEdge},
 * in the error path. Converter should be provided by state {@link
 * ConfigurableProgramAnalysisWithAdditionalInfo}
 */
public class CFAEdgeWithAdditionalInfo {
  private final Multimap<ConvertingTags, Object> addinitonalInfo;
  private final CFAEdge edge;

  CFAEdgeWithAdditionalInfo(Multimap<ConvertingTags, Object> pAddinitonalInfo, CFAEdge pEdge) {
    addinitonalInfo = HashMultimap.create(pAddinitonalInfo);
    edge = Objects.requireNonNull(pEdge);
  }

  /** Constructor used when merging two edges. */
  public CFAEdgeWithAdditionalInfo(
      CFAEdgeWithAdditionalInfo pEdge1, CFAEdgeWithAdditionalInfo pEdge2) {
    assert pEdge1.edge.equals(pEdge2.edge);

    edge = pEdge1.edge;
    addinitonalInfo = HashMultimap.create(pEdge1.addinitonalInfo);
    addinitonalInfo.putAll(pEdge2.addinitonalInfo);
  }

  CFAEdgeWithAdditionalInfo mergeEdge(CFAEdgeWithAdditionalInfo pEdge) {
    return new CFAEdgeWithAdditionalInfo(this, pEdge);
  }

  public static CFAEdgeWithAdditionalInfo of(CFAEdge pEdge) {
    return new CFAEdgeWithAdditionalInfo(HashMultimap.create(), pEdge);
  }

  public void addInfo(ConvertingTags tag, Object value) {
    addinitonalInfo.put(tag, value);
  }

  public CFAEdge getCFAEdge() {
    return edge;
  }

  public Collection<Entry<ConvertingTags, Object>> getInfos() {
    return addinitonalInfo.entries();
  }
}
