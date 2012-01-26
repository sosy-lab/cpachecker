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

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.RelyGuaranteeCFA;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Stores information about variables of a concurrent program.
 */
public class RGVariables {

  public final int threadNo;
  public final ImmutableSet<String> globalVars;
  public final ImmutableList<ImmutableSet<String>> localVars;
  public final ImmutableSet<String> allVars;

  public RGVariables(RelyGuaranteeCFA[] cfas){
    assert cfas.length > 0;

    this.threadNo = cfas.length;
    this.globalVars = ImmutableSet.copyOf(cfas[0].getGlobalVariables());

    Builder<ImmutableSet<String>> lb = new ImmutableList.Builder<ImmutableSet<String>>();
    for (RelyGuaranteeCFA cfa : cfas){
      lb.add(ImmutableSet.copyOf(cfa.getScopedLocalVars()));
    }
    this.localVars = lb.build();

    com.google.common.collect.ImmutableSet.Builder<String> avb = new ImmutableSet.Builder<String>();
    avb.addAll(globalVars);
    for (Set<String> lv : localVars){
      avb.addAll(lv);
    }
    this.allVars = avb.build();

    assert this.localVars.size() == this.threadNo;
  }

  /*public RelyGuaranteeVariables(int threadNo, Set<String> globalVars, List<Set<String>> localVars) {
    assert threadNo > 0;
    assert localVars.size() == threadNo;

    this.threadNo = threadNo;
    this.globalVars = ImmutableSet.copyOf(globalVars);
    Builder<ImmutableSet<String>> lb = new ImmutableList.Builder<ImmutableSet<String>>();
    for (Set<String> set : localVars) {
      lb.add(ImmutableSet.copyOf(set));
    }
    this.localVars = lb.build();

    assert this.localVars.size() == this.threadNo;
  }*/
}
