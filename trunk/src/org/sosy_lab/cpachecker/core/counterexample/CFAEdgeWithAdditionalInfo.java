// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
