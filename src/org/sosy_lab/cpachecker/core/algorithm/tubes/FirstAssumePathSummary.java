// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public class FirstAssumePathSummary implements PathSummary<String> {

  @Override
  public String summarize(List<ARGPath> paths) {
    ImmutableList.Builder<String> expression = ImmutableList.builder();
    for (ARGPath path : paths) {
      for (CFAEdge cfaEdge : path.getFullPath()) {
        if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          if (!cfaEdge.getRawStatement().isBlank()) {
            expression.add(cfaEdge.getRawStatement());
            break;
          }
        }
      }
    }
    return Joiner.on(" && ").join(expression.build());
  }
}
