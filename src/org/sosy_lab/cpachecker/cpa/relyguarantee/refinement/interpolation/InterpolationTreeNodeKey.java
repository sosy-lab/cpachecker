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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation;

public class InterpolationTreeNodeKey  {

  protected final int tid;
  protected final int artElementId;
  protected final int uniqueId;

  InterpolationTreeNodeKey(int tid, int  artElementId,  int uniqueId){
    this.tid = tid;
    this.artElementId = artElementId;
    this.uniqueId = uniqueId;
  }

  InterpolationTreeNodeKey(InterpolationDagNodeKey key,  int uniqueId){
    this.tid = key.tid;
    this.artElementId = key.artElementId;
    this.uniqueId = uniqueId;
  }

  public Integer getTid() {
    return tid;
  }

  public Integer getArtElementId() {
    return artElementId;
  }

  public Integer getUniqueId() {
    return uniqueId;
  }

  @Override
  public String toString(){
    return "itpTreeNodeKey ("+tid+","+artElementId+","+uniqueId+")";
  }

  @Override
  public int hashCode(){
    return 11 * (47 * tid + 13 * (artElementId + uniqueId));
  }

  @Override
  public boolean equals(Object other){
    if (!(other instanceof InterpolationTreeNodeKey)){
      return false;
    }

    InterpolationTreeNodeKey otherKey = (InterpolationTreeNodeKey) other;

    if (otherKey.uniqueId == uniqueId && otherKey.artElementId == artElementId && otherKey.tid == tid){
      return true;
    } else {
      return false;
    }
  }


}
