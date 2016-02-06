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
package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.collect.Lists;

public class CounterexampleInfo {

  private final boolean spurious;
  private final boolean isPreciseCounterExample;

  private final ARGPath targetPath;
  private final RichModel model;
  private final CFAPathWithAssumptions assignments;

  // list with additional information about the counterexample
  private final Collection<Pair<Object, PathTemplate>> furtherInfo;

  private static final CounterexampleInfo SPURIOUS = new CounterexampleInfo(true, null, null, null, false);

  private CounterexampleInfo(boolean pSpurious, ARGPath pTargetPath, RichModel pModel,
      CFAPathWithAssumptions pAssignments, boolean pIsPreciseCEX) {
    spurious = pSpurious;
    targetPath = pTargetPath;
    model = pModel;
    assignments = pAssignments;
    isPreciseCounterExample = pIsPreciseCEX;

    if (!spurious) {
      furtherInfo = Lists.newArrayListWithExpectedSize(1);
    } else {
      furtherInfo = null;
    }
  }

  public static CounterexampleInfo spurious() {
    return SPURIOUS;
  }

  public boolean isPreciseCounterExample() {
    checkState(!spurious);
    return isPreciseCounterExample;
  }

  /**
   * Creates a feasible counterexample whose target path is marked as being imprecise.
   */
  public static CounterexampleInfo feasible(ARGPath pTargetPath, RichModel pModel, CFAPathWithAssumptions pAssignments) {
    return new CounterexampleInfo(false, checkNotNull(pTargetPath), checkNotNull(pModel), checkNotNull(pAssignments), false);
  }

  /**
   * Creates a feasible counterexample whose target path is marked as being precise.
   */
  public static CounterexampleInfo feasiblePrecise(ARGPath pTargetPath, RichModel pModel, CFAPathWithAssumptions pAssignments) {
    return new CounterexampleInfo(false, checkNotNull(pTargetPath), checkNotNull(pModel), checkNotNull(pAssignments), true);
  }

  public boolean isSpurious() {
    return spurious;
  }

  public ARGPath getTargetPath() {
    checkState(!spurious);
    assert targetPath != null;

    return targetPath;
  }

  public RichModel getTargetPathModel() {
    checkState(!spurious);

    return model;
  }

  /**
   * Return a path that indicates which variables where assigned which values at
   * what edge. Note that not every value for every variable is available.
   */
  @Nullable
  public CFAPathWithAssumptions getCFAPathWithAssignments() {
    checkState(!spurious);
    return assignments;
  }

  @Nullable
  public Map<ARGState, CFAEdgeWithAssumptions> getExactVariableValues(ARGPath pPath) {
    checkState(!spurious);
    if (assignments.isEmpty()) {
      return null;
    }

    return assignments.getExactVariableValues(pPath);
  }

  @Nullable
  public CFAPathWithAssumptions getExactVariableValuePath(List<CFAEdge> pPath) {
    checkState(!spurious);
    if (assignments.isEmpty()) {
      return null;
    }

    return assignments.getExactVariableValues(pPath);
  }

  /**
   * Add some additional information about the counterexample.
   *
   * @param info The information.
   * @param dumpFile The file where "info.toString()" should be dumped (may be null).
   */
  public void addFurtherInformation(Object info, PathTemplate dumpFile) {
    checkState(!spurious);

    furtherInfo.add(Pair.of(checkNotNull(info), dumpFile));
  }

  /**
   * Get all additional information stored in this object.
   * A file where to dump it may be associated with each object, but this part
   * of the pair may be null.
   */
  public Collection<Pair<Object, PathTemplate>> getAllFurtherInformation() {
    checkState(!spurious);

    return Collections.unmodifiableCollection(furtherInfo);
  }
}
