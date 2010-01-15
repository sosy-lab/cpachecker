/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.invariant.common;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import symbpredabstraction.interfaces.SymbolicFormula;
import cfa.objectmodel.CFANode;

/**
 * Representation of an invariant of the form \land_i. pc = l_i ==> \phi_i
 * 
 * @author g.theoduloz
 */
public class InvariantWithLocation {
  
  // map from location to (conjunctive) list of invariants
  private final Map<CFANode, Invariant> map;
  
  public InvariantWithLocation() {
    map = new HashMap<CFANode, Invariant>();
  }
  
  /**
   * Return the invariant as a formula for a given node
   */
  public Invariant getInvariant(CFANode node)
  {
    Invariant result = map.get(node);
    
    if (result == null)
      return Invariant.TRUE;
    else
      return result;
  }

  /**
   * Add an invariant at the given location
   */
  public void addInvariant(CFANode node, Invariant invariant)
  {
    if (!invariant.isTrue()) {
      Invariant oldInvariant = map.get(node);
      if (oldInvariant == null)
        map.put(node, invariant);
      else
        map.put(node, oldInvariant.and(invariant));
    }
  }
  
  /**
   * Dump the invariant to the given Appendable object
   * (e.g., PrintStream, Writer, etc.)
   * IOException are ignored.
   */
  public void dump(Appendable out)
  {
    try {
      for (Entry<CFANode, Invariant> entry : map.entrySet()) {
        String nodeId = Integer.toString(entry.getKey().getNodeNumber());
        Invariant inv = entry.getValue();
        SymbolicFormula disInv = inv.getDischargeableAssumption();
        SymbolicFormula otherInv = inv.getOtherAssumption();
        if (!disInv.isTrue()) {
          out.append("pc = ").append(nodeId).append("\t =(d)=>  ");
          out.append(disInv.toString()).append("\n");
        }
        if (!otherInv.isTrue()) {
          out.append("pc = ").append(nodeId).append("\t =====>  ");
          out.append(otherInv.toString()).append("\n");
        }
      }
    } catch (IOException e) { }
  }

  @Override
  public String toString() {
    StringWriter writer = new StringWriter();
    dump(writer);
    return writer.toString();
  }
  
}
