/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics.HeuristicPrecision;

public class ProgressObserverPrecision implements Precision {

  private final List<Precision> precisions;

  public ProgressObserverPrecision(ProgressObserverCPA pProgressObserverCPA) {
    this.precisions = new ArrayList<Precision>();
    for(StopHeuristics<?> heur: pProgressObserverCPA.getEnabledHeuristics()) {
      precisions.add(heur.getInitialPrecision());
    }
  }

  public void addPrecision(Precision precision){
    precisions.add(precision);
  }

  public boolean adjustPrecisions(){
    boolean precisionAdjusted = false;
    boolean shouldForceToStop = false;
    for(Precision pre: precisions){
      if(pre instanceof HeuristicPrecision){
        precisionAdjusted |= ((HeuristicPrecision)pre).adjustPrecision();
        shouldForceToStop |= ((HeuristicPrecision)pre).shouldForceToStop();
      }
    }
    return (!shouldForceToStop) & precisionAdjusted;
  }

}
