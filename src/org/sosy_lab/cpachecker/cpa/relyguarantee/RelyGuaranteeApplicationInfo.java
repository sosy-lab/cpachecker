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

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Contains information on applications of environmental edges.
 */
public class RelyGuaranteeApplicationInfo {


  /**
   * Env. edges that have been applied.
   */
  public final Map<Integer, RelyGuaranteeCFAEdgeTemplate> envMap;

  /** Path formula for refinement */
  private PathFormula refinementPf;



  public RelyGuaranteeApplicationInfo(){
    this.envMap = new HashMap<Integer, RelyGuaranteeCFAEdgeTemplate>();
  }

  /** Makes a copy of another application info. */
  public RelyGuaranteeApplicationInfo(RelyGuaranteeApplicationInfo other){
    this.envMap = new HashMap<Integer, RelyGuaranteeCFAEdgeTemplate>(other.envMap);
  }

  /**
   * Add all entries from the other element into this one.
   * @param other
   * @return
   */
  public void mergeWith(RelyGuaranteeApplicationInfo other){
    envMap.putAll(other.envMap);
  }

  public Map<Integer, RelyGuaranteeCFAEdgeTemplate> getEnvMap() {
    return envMap;
  }

  /**
   * Shorthand for getEnvMap().put(i, rgEdge).
   * @param rgEdge
   * @param pf
   * @return
   */
  public RelyGuaranteeCFAEdgeTemplate putEnvApplication(Integer i, RelyGuaranteeCFAEdgeTemplate rgEdge){
    return envMap.put(i, rgEdge);
  }

  @Override
  public String toString(){
    return "rgEdges: "+envMap.keySet();
  }

  public void setRefinementFormula(PathFormula pf) {
    this.refinementPf = pf;
  }

  public PathFormula getRefinementPf() {
    return refinementPf;
  }

  public void setRefinementPf(PathFormula pRefinementPf) {
    refinementPf = pRefinementPf;
  }



}