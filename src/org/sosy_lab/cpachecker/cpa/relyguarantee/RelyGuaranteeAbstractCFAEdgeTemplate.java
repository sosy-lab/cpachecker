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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class RelyGuaranteeAbstractCFAEdgeTemplate extends RelyGuaranteeCFAEdgeTemplate {

  /** Abstraction for the edge */
  private final AbstractionFormula absFilter;
  private final Region renRegion;

  public Region getRenRegion() {
    return renRegion;
  }

  public RelyGuaranteeAbstractCFAEdgeTemplate(AbstractionFormula filter, ARTElement lastARTAbstractionElement, RelyGuaranteeEnvironmentalTransition sourceEnvTransition, Region region){
    super(filter.asPathFormula(), lastARTAbstractionElement, sourceEnvTransition);
    this.absFilter = filter;
    this.renRegion = region;
  }

  @Override
  public int getType() {
    return RelyGuaranteeAbstractCFAEdgeTemplate;
  }

  public AbstractionFormula getAbstractFilter() {
    return absFilter;
  }

  @Override
  public String toString() {
    return "RG abstract edge -- op:"+this.getRawStatement()+", filter:"+this.getFilter()+",  T:"+this.getSourceTid()+", sART:"+this.getSourceARTElement().getElementId();
  }



}
