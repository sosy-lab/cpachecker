/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.assumptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

/**
 * Representation of an assumption of the form \land_i. pc = l_i ==> \phi_i,
 * using a hash map of locations for efficient lookup.
 * 
 * @author g.theoduloz
 */
public class AssumptionWithMultipleLocations
  extends AssumptionWithLocation
{
  
  // map from location to (conjunctive) list of invariants
  private final Map<CFANode, Assumption> map;
  
  public AssumptionWithMultipleLocations() {
    map = new HashMap<CFANode, Assumption>();
  }
  
  /**
   * Return a copy of the given assumption (shallow copy of the map) 
   */
  public AssumptionWithMultipleLocations copy()
  {
    AssumptionWithMultipleLocations result = new AssumptionWithMultipleLocations();
    result.map.putAll(this.map);
    return result;
  }
  
  /**
   * Return the assumption as a formula for a given node
   */
  @Override
  public Assumption getAssumption(CFANode node)
  {
    Assumption result = map.get(node);
    
    if (result == null)
      return Assumption.TRUE;
    else
      return result;
  }

  /**
   * Add an assumption at the given location
   */
  public void addAssumption(CFANode node, Assumption assumption)
  {
    if (!assumption.isTrue()) {
      Assumption oldInvariant = map.get(node);
      if (oldInvariant == null)
        map.put(node, assumption);
      else
        map.put(node, oldInvariant.and(assumption));
    }
  }
  
  /**
   * Add a given assumption with location
   * (same as and, but with side-effect)
   */
  public void addAssumption(AssumptionWithLocation assumption)
  {
    for (Entry<CFANode, Assumption> otherEntry : assumption.getAssumptionsIterator()) {
      addAssumption(otherEntry.getKey(), otherEntry.getValue());
    }
  }

  @Override
  public AssumptionWithLocation and(AssumptionWithLocation other) {
    AssumptionWithMultipleLocations result = copy();
    result.addAssumption(other);
    return result;
  }

  @Override
  public Iterable<Entry<CFANode, Assumption>> getAssumptionsIterator() {
    return map.entrySet();
  }
  
    
}
