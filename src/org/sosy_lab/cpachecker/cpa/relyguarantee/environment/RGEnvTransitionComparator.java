/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment;

import java.util.Comparator;

import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;

public enum RGEnvTransitionComparator implements Comparator<RGEnvTransition>{


  ARTID_MAX {

    @Override
    public int compare(RGEnvTransition et1, RGEnvTransition et2) {

      if (et1.equals(et2)){
        return 0;
      }

      int br1 = et1.getSourceARTElement().getRefinementBranches();
      int br2 = et2.getSourceARTElement().getRefinementBranches();

      if (br1 < br2){
        return 1;
      }

      if (br1 > br2){
        return -1;
      }

      int id1 = et1.getSourceARTElement().getElementId();
      int id2 = et2.getSourceARTElement().getElementId();

      if (id1 > id2){
        return 1;
      }

      if (id1 < id2){
        return -1;
      }

      int tarid1 = et1.getTargetARTElement().getElementId();
      int tarid2 = et2.getTargetARTElement().getElementId();
      assert tarid1 != tarid2;

      return (tarid1 > tarid2) ? 1 : -1;

    }
  }
}


