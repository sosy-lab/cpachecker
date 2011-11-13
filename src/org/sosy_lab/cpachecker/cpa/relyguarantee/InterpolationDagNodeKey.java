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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import org.sosy_lab.common.Pair;

class  InterpolationDagNodeKey{

  protected final Integer tid;
  protected final Integer artElementId;

  public InterpolationDagNodeKey(Integer tid, Integer artElementId){
    assert tid != null;
    assert artElementId != null;
    this.tid  = tid;
    this.artElementId = artElementId;
  }

  public Pair<Integer, Integer> getPair() {
    return Pair.of(tid, artElementId);
  }

  public Integer getTid(){
    return tid;
  }

  public Integer getARTElementId(){
    return artElementId;
  }

  public String toString(){
    return "("+tid+","+artElementId+")";
  }

  @Override
  public int hashCode(){
    return 11 * (47 * tid + artElementId);
  }

  public boolean equals(Object other){
    if (!(other instanceof InterpolationDagNodeKey)){
      return false;
    }

    InterpolationDagNodeKey otherKey = (InterpolationDagNodeKey) other;

    if (otherKey.getTid().equals(this.getTid()) && otherKey.getARTElementId().equals(this.getARTElementId())){
      return true;
    } else {
      return false;
    }
  }


}
