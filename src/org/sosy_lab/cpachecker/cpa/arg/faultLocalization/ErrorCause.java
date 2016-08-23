/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.faultLocalization;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;

import java.util.List;
import java.util.Set;

/**
 * Possible cause of a property violation in a program.
 */
public class ErrorCause {

  private ARGPath fullErrorPath;
  private Set<CFAEdge> relevantEdges;

  public ErrorCause(
      final ARGPath pErrorPath,
      final List<CFAEdge> pFaultCausingEdges
  ) {
    fullErrorPath = pErrorPath;
    relevantEdges = ImmutableSet.copyOf(pFaultCausingEdges);
  }

  public boolean contains(final CFAEdge pEdge) {
    return relevantEdges.contains(pEdge);
  }
}
