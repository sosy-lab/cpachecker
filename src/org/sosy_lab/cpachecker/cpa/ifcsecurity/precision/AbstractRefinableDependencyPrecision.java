/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.precision;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

abstract public  class AbstractRefinableDependencyPrecision implements RefinableDependencyPrecision{

  protected Map<CFANode, Map<Variable, SortedSet<Variable>>> refinementInfo=new TreeMap<>();

  AbstractRefinableDependencyPrecision(){

  }

  abstract public boolean isTracked(Variable var);

  @Override
  public boolean isTracked(CFANode node, Variable lhs, Variable rhs){
    if(refinementInfo.keySet().contains(node)){
      Map<Variable, SortedSet<Variable>> vardepmapping=refinementInfo.get(node);
      if(vardepmapping.keySet().contains(lhs)){
        SortedSet<Variable> refinedDeps=vardepmapping.get(lhs);
        return isTracked(rhs) && refinedDeps.contains(rhs);
      }
    }
    return isTracked(rhs);
  }

  @Override
  public void addRefinementInfo(CFANode node, Variable var, SortedSet<Variable> refineddeps){
    if(!refinementInfo.keySet().contains(node)){
      refinementInfo.put(node, new TreeMap<Variable, SortedSet<Variable>>());
    }
    Map<Variable, SortedSet<Variable>> vardepmapping=refinementInfo.get(node);
    vardepmapping.put(var, refineddeps);
  }
}
