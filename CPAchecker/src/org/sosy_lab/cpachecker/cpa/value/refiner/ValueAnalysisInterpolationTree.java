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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;

/**
 * This class represents an interpolation tree, i.e. a set of states connected through a successor-predecessor-relation.
 * The tree is built from traversing backwards from error states. It can be used to retrieve paths from the root of the
 * tree to error states, in a way, that only path not yet excluded by previous path interpolation need to be interpolated.
 */
class ValueAnalysisInterpolationTree extends InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> {

  /**
   * This method acts as constructor of the interpolation tree.
   *
   * @param pLogger the logger to use
   * @param pTargetPaths the set of target paths from which to build the interpolation tree
   * @param pUseTopDownInterpolationStrategy the flag to choose the strategy to apply
   */
  ValueAnalysisInterpolationTree(
      final LogManager pLogger,
      final List<ARGPath> pTargetPaths,
      final boolean pUseTopDownInterpolationStrategy
  ) {
    super(ValueAnalysisInterpolantManager.getInstance(),
          pLogger,
          pTargetPaths,
          pUseTopDownInterpolationStrategy);
  }

  // Implement method here to make it visible to package classes
  @Override
  public Set<Map.Entry<ARGState, ValueAnalysisInterpolant>> getInterpolantMapping() {
    return super.getInterpolantMapping();
  }
}
