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
package assumptions;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import symbpredabstraction.interfaces.SymbolicFormula;
import cfa.objectmodel.CFANode;

/**
 * Representation of an assumption of the form \land_i. pc = l_i ==> \phi_i
 * 
 * @author g.theoduloz
 */
public class AssumptionWithLocation {
  
  // map from location to (conjunctive) list of invariants
  private final Map<CFANode, Assumption> map;
  
  public AssumptionWithLocation() {
    map = new HashMap<CFANode, Assumption>();
  }
  
  /**
   * Return the assumption as a formula for a given node
   */
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
  public void addAssumption(CFANode node, Assumption invariant)
  {
    if (!invariant.isTrue()) {
      Assumption oldInvariant = map.get(node);
      if (oldInvariant == null)
        map.put(node, invariant);
      else
        map.put(node, oldInvariant.and(invariant));
    }
  }
  
  /**
   * Dump the assumption to the given Appendable object
   * (e.g., PrintStream, Writer, etc.)
   * IOException are ignored.
   */
  public void dump(Appendable out)
  {
    try {
      for (Entry<CFANode, Assumption> entry : map.entrySet()) {
        String nodeId = Integer.toString(entry.getKey().getNodeNumber());
        Assumption inv = entry.getValue();
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
