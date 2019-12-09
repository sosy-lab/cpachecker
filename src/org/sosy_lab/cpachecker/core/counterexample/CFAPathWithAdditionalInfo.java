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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.AdditionalInfoConverter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.ExtendedWitnessFactory;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * This class represents a path of cfaEdges, that contain the additional Information to be exported
 * to extended witness {@link ExtendedWitnessFactory}.
 */
public class CFAPathWithAdditionalInfo extends ForwardingList<CFAEdgeWithAdditionalInfo> {
  private final ImmutableList<CFAEdgeWithAdditionalInfo> pathInfo;
  private final Set<AdditionalInfoConverter> additionalInfoConverters;

  private CFAPathWithAdditionalInfo(List<CFAEdgeWithAdditionalInfo> pPathInfo) {
    pathInfo = ImmutableList.copyOf(pPathInfo);
    additionalInfoConverters = new HashSet<>();
  }

  public static CFAPathWithAdditionalInfo empty() {
    return new CFAPathWithAdditionalInfo(ImmutableList.of());
  }

  public static CFAPathWithAdditionalInfo of(List<CFAEdgeWithAdditionalInfo> pPathInfo) {
    return new CFAPathWithAdditionalInfo(pPathInfo);
  }

  public static CFAPathWithAdditionalInfo of(ARGPath pPath, ConfigurableProgramAnalysis pCPA) {
    FluentIterable<ConfigurableProgramAnalysisWithAdditionalInfo> cpas =
        CPAs.asIterable(pCPA).filter(ConfigurableProgramAnalysisWithAdditionalInfo.class);

    Optional<CFAPathWithAdditionalInfo> result = Optional.empty();

    for (ConfigurableProgramAnalysisWithAdditionalInfo wrappedCpa : cpas) {
      CFAPathWithAdditionalInfo path = wrappedCpa.createExtendedInfo(pPath);
      path.addConverter(wrappedCpa.exportAdditionalInfoConverter());

      if (result.isPresent()) {
        result = result.orElseThrow().mergePaths(path);
        // If there were conflicts during merging, stop
        if (!result.isPresent()) {
          break;
        }
      } else {
        result = Optional.of(path);
      }
    }

    if (!result.isPresent()) {
      return CFAPathWithAdditionalInfo.empty();
    } else {
      return result.orElseThrow();
    }
  }

  private void addConverter(AdditionalInfoConverter pAdditionalInfoConverter) {
    additionalInfoConverters.add(pAdditionalInfoConverter);
  }

  public Set<AdditionalInfoConverter> getAdditionalInfoConverters() {
    return additionalInfoConverters;
  }

  private Optional<CFAPathWithAdditionalInfo> mergePaths(CFAPathWithAdditionalInfo pOtherPath) {
    if (pOtherPath.size() != this.size()) {
      return Optional.empty();
    }

    List<CFAEdgeWithAdditionalInfo> result = new ArrayList<>(size());
    Iterator<CFAEdgeWithAdditionalInfo> path2Iterator = iterator();

    for (CFAEdgeWithAdditionalInfo edge : this) {
      CFAEdgeWithAdditionalInfo other = path2Iterator.next();
      if (!edge.getCFAEdge().equals(other.getCFAEdge())) {
        return Optional.empty();
      }
      CFAEdgeWithAdditionalInfo resultEdge = edge.mergeEdge(other);
      result.add(resultEdge);
    }

    return Optional.of(new CFAPathWithAdditionalInfo(result));
  }

  @Override
  protected List<CFAEdgeWithAdditionalInfo> delegate() {
    return pathInfo;
  }

  public Map<ARGState, CFAEdgeWithAdditionalInfo> getAdditionalInfoMapping(ARGPath pPath) {
    Map<ARGState, CFAEdgeWithAdditionalInfo> result = new HashMap<>();

    PathIterator pathIterator = pPath.fullPathIterator();
    int multiEdgeOffset = 0;

    while (pathIterator.hasNext()) {
      CFAEdgeWithAdditionalInfo edgeWithAdditionalInfo =
          pathInfo.get(pathIterator.getIndex() + multiEdgeOffset);
      CFAEdge argPathEdge = pathIterator.getOutgoingEdge();

      if (!edgeWithAdditionalInfo.getCFAEdge().equals(argPathEdge)) {
        // path is not equivalent
        return ImmutableMap.of();
      }

      final ARGState abstractState;
      if (pathIterator.isPositionWithState()) {
        abstractState = pathIterator.getAbstractState();
      } else {
        abstractState = pathIterator.getPreviousAbstractState();
      }
      result.put(abstractState, edgeWithAdditionalInfo);

      pathIterator.advance();
    }
    // last state is ignored

    return result;
  }
}
