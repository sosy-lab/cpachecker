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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithExtendedInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.util.CPAs;

public class CFAPathWithAdditionalInfo extends ForwardingList<CFAEdgeWithAdditionalInfo> {
  private final ImmutableList<CFAEdgeWithAdditionalInfo> pathInfo;

  public CFAPathWithAdditionalInfo(List<CFAEdgeWithAdditionalInfo> pPathInfo) {
    pathInfo = ImmutableList.copyOf(pPathInfo);
  }

  public static CFAPathWithAdditionalInfo of(ARGPath pPath, ConfigurableProgramAnalysis pCPA) {
    FluentIterable<ConfigurableProgramAnalysisWithExtendedInfo> cpas =
        CPAs.asIterable(pCPA).filter(ConfigurableProgramAnalysisWithExtendedInfo.class);

    Optional<CFAPathWithAdditionalInfo> result = Optional.empty();
    for (ConfigurableProgramAnalysisWithExtendedInfo wrappedCpa : cpas) {
      CFAPathWithAdditionalInfo path = wrappedCpa.createExtendedInfo(pPath);


      if (result.isPresent()) {
        result = result.get().mergePaths(path);
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
      return result.get();
    }
  }

  private static CFAPathWithAdditionalInfo empty() {
    return new CFAPathWithAdditionalInfo(ImmutableList.of());
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
}
