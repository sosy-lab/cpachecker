/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.base.Preconditions;


public class RefinementResult {
  public enum RefinementStatus {
    TRUE,
    FALSE,
    UNKNOWN
  }
  private final Map<Class<? extends RefinementInterface>, Object> auxiliaryInfo = new HashMap<>();
  private final Pair<UsageInfo, UsageInfo> trueRace;
  //Currently only predicate one
  private PredicatePrecision precision;
  RefinementStatus status;

  private RefinementResult(RefinementStatus rStatus, UsageInfo firstUsage, UsageInfo secondUsage) {
    status = rStatus;
    if (firstUsage != null && secondUsage != null) {
      //True result
      Preconditions.checkArgument(status == RefinementStatus.TRUE);
      trueRace = Pair.of(firstUsage, secondUsage);
    } else {
      //Other results
      trueRace = null;
    }
    precision = PredicatePrecision.empty();
  }

  public void addInfo(Class<? extends RefinementInterface> caller, Object info) {
    //Now used only for transferring precision
    auxiliaryInfo.put(caller, info);
  }

  public Object getInfo(Class<? extends RefinementInterface> caller) {
    return auxiliaryInfo.get(caller);
  }

  public boolean isTrue() {
    return status == RefinementStatus.TRUE;
  }

  public boolean isFalse() {
    return status == RefinementStatus.FALSE;
  }

  public boolean isUnknown() {
    return status == RefinementStatus.UNKNOWN;
  }

  public static RefinementResult createTrue(ExtendedARGPath firstPath, ExtendedARGPath secondPath) {

    UsageInfo firstUsage = firstPath.getUsageInfo();
    UsageInfo secondUsage = secondPath.getUsageInfo();

    if (firstUsage == secondUsage) {
      secondUsage = secondUsage.clone();
    }
    firstUsage.setRefinedPath(firstPath.getInnerEdges());
    secondUsage.setRefinedPath(secondPath.getInnerEdges());
    return new RefinementResult(RefinementStatus.TRUE, firstUsage, secondUsage);
  }

  public static RefinementResult createTrue() {
    //Used for temporary result
    return new RefinementResult(RefinementStatus.TRUE, null, null);
  }

  public static RefinementResult createFalse() {
    return new RefinementResult(RefinementStatus.FALSE, null, null);
  }

  public static RefinementResult createUnknown() {
    return new RefinementResult(RefinementStatus.UNKNOWN, null, null);
  }

  public Pair<UsageInfo, UsageInfo> getTrueRace() {
    Preconditions.checkArgument(status == RefinementStatus.TRUE);
    return trueRace;
  }

  public void addPrecision(PredicatePrecision p) {
    precision = precision.mergeWith(p);
  }

  public PredicatePrecision getPrecision() {
    return precision;
  }

  @Override
  public String toString() {
    return status.name();
  }
}
