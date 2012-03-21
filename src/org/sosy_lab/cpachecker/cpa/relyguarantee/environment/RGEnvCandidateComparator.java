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

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;

public enum RGEnvCandidateComparator implements Comparator<RGEnvCandidate>{


  ARTID_MAX {

    @Override
    public int compare(RGEnvCandidate pO1, RGEnvCandidate pO2) {

      if (pO1.equals(pO2)){
        return 0;
      }

      int br1 = pO1.getElement().getRefinementBranches();
      int br2 = pO2.getElement().getRefinementBranches();

      if (br1 < br2){
        return 1;
      }

      if (br1 > br2){
        return -1;
      }

      int id1 = pO1.getElement().getElementId();
      int id2 = pO2.getElement().getElementId();

      if (id1 > id2){
        return 1;
      }

      if (id1 < id2){
        return -1;
      }

      int sid1 = pO1.getSuccessor().getElementId();
      int sid2 = pO2.getSuccessor().getElementId();

      assert sid1 != sid2;
      return sid1 > sid2 ? 1 : -1;

    }
  },

  ENVAPP_MIN_DISTANCE_MIN_TOP_MAX {

    @Override
    public int compare(RGEnvCandidate et1, RGEnvCandidate et2) {

      if (et1.equals(et2)){
        return 0;
      }

      ARTElement src1 = et1.getElement();
      ARTElement src2 = et2.getElement();

      int br1 = src1.getRefinementBranches();
      int br2 = src2.getRefinementBranches();

      if (br1 < br2){
        return 1;
      }

      if (br1 > br2){
        return -1;
      }

      Integer d1 = src1.getDistanceFromRoot();
      Integer d2 = src2.getDistanceFromRoot();
      assert d1 != null && d2 != null;

      if (d1 < d2){
        return 1;
      }

      if (d1 > d2){
        return -1;
      }

      Integer top1 = src1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      Integer top2 = src2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 > top2){
        return 1;
      }

      if (top1 < top2){
        return -1;
      }

      int id1 = src1.getElementId();
      int id2 = src2.getElementId();

      if (id1 < id2){
        return 1;
      }

      if (id1 > id2){
        return -1;
      }

      ARTElement trg1 = et1.getSuccessor();
      ARTElement trg2 = et2.getSuccessor();

      int topT1 = trg1.retrieveLocationElement().getLocationNode()
          .getTopologicalSortId();
      int topT2 = trg2.retrieveLocationElement().getLocationNode()
          .getTopologicalSortId();

      if (topT1 > topT2){
        return 1;
      }

      if (topT1 < topT2){
        return -1;
      }


      int idT1 = trg1.getElementId();
      int idT2 = trg2.getElementId();

      if (idT1 < idT2){
        return 1;
      }

      if (idT1 > idT2){
        return -1;
      }

      assert false;
      return 0;
    }

  },

  ITP_MIN_DIST_MAX {

    @Override
    public int compare(RGEnvCandidate et1, RGEnvCandidate et2) {

      if (et1.equals(et2)){
        return 0;
      }

      ARTElement src1 = et1.getElement();
      ARTElement src2 = et2.getElement();

      int br1 = src1.getRefinementBranches();
      int br2 = src2.getRefinementBranches();

      if (br1 < br2){
        return 1;
      }

      if (br1 > br2){
        return -1;
      }

      Integer d1 = src1.getDistanceFromRoot();
      Integer d2 = src2.getDistanceFromRoot();
      assert d1 != null && d2 != null;

      if (d1 > d2){
        return 1;
      }

      if (d1 < d2){
        return -1;
      }

      Integer top1 = src1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      Integer top2 = src2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 < top2){
        return 1;
      }

      if (top1 > top2){
        return -1;
      }

      int id1 = src1.getElementId();
      int id2 = src2.getElementId();

      if (id1 > id2){
        return 1;
      }

      if (id1 < id2){
        return -1;
      }

      ARTElement trg1 = et1.getSuccessor();
      ARTElement trg2 = et2.getSuccessor();

      int topT1 = trg1.retrieveLocationElement().getLocationNode()
          .getTopologicalSortId();
      int topT2 = trg2.retrieveLocationElement().getLocationNode()
          .getTopologicalSortId();

      if (topT1 < topT2){
        return 1;
      }

      if (topT1 > topT2){
        return -1;
      }


      int idT1 = trg1.getElementId();
      int idT2 = trg2.getElementId();

      if (idT1 > idT2){
        return 1;
      }

      if (idT1 < idT2){
        return -1;
      }

      assert false;
      return 0;
    }

  }



}
