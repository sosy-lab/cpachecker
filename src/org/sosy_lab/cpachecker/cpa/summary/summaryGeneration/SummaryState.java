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
package org.sosy_lab.cpachecker.cpa.summary.summaryGeneration;

import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;

/**
 * State of the top-level CPA.
 */
public class SummaryState
    implements Partitionable, Summary {

  private final String functionName;
  private final ReachedSet reachedSet;

  public SummaryState(String pFunctionName, ReachedSet pReachedSet) {
    functionName = pFunctionName;
    reachedSet = pReachedSet;
  }

  private void isCoveredBy() {

  }

  public static SummaryState start() {
    // todo: ReachedSet which contains exactly one function
    // call to "main".
    return new SummaryState("_start", null);
  }

  public static SummaryState emptySummaryForFunction(
      String pFunctionName
  ) {
    // todo: is it a problem if it is null?..
    return new SummaryState(pFunctionName, null);
  }

  public ReachedSet getReached() {
    return reachedSet;
  }

  @Override
  public Object getPartitionKey() {
    return functionName;
  }

  @Override
  public String getFunctionName() {
    return null;
  }
}
