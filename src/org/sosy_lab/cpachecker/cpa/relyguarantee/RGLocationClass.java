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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;


/**
 * Represents a non-empty, immutable set of nodes that are seen as equivalent.
 */
public class RGLocationClass {

  private final ImmutableSet<CFANode> classNodes;

  public RGLocationClass(ImmutableSet<CFANode> nodes){
    Preconditions.checkArgument(!nodes.isEmpty(), "The empty set cannot be a location class");
    this.classNodes = nodes;
  }

  public ImmutableSet<CFANode> getClassNodes() {
    return classNodes;
  }

  @Override
  public String toString(){
    return classNodes.toString();
  }

  @Override
  public int hashCode(){
    return classNodes.hashCode();
  }

  public boolean equals(RGLocationClass other){
    if (this == other){
      return true;
    }

    return other.classNodes.equals(classNodes);
  }
}
