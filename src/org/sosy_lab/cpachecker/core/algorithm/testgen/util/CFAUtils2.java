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
package org.sosy_lab.cpachecker.core.algorithm.testgen.util;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Optional;


public class CFAUtils2 {

  /**
   * returns an alternative edge (never the given edge)
   * of the leaving edges of the predecessor of the given edge.
   * This is semantically equivalent to a constraint negation,
   * where the constraint represents an assumption.
   * If the given edge is the only leaving edge of its predecessor,
   * this method returns Optional.absent()
   * @param decidingNode
   * @param wrongEdge
   * @return
   */
  public static Optional<CFAEdge> getAlternativeLeavingEdge(CFAEdge wrongEdge) {

    return getAlternativeLeavingEdge(wrongEdge.getPredecessor(), wrongEdge);
  }

  /**
   * same as {@link #getAlternativeLeavingEdge(CFAEdge)}, but allows specification of the given edge.
   * Its expected that wrongEdge is a leaving edge of node, but its not checked.
   * @param decidingNode
   * @param wrongEdge
   * @return
   */
  public static Optional<CFAEdge> getAlternativeLeavingEdge(CFANode node, CFAEdge wrongEdge) {
    for (CFAEdge cfaEdge : CFAUtils.leavingEdges(node)) {
      if (cfaEdge.equals(wrongEdge)) {
        continue;
      } else {
        return Optional.of(cfaEdge);
      }
    }
    return Optional.absent();
  }

}
