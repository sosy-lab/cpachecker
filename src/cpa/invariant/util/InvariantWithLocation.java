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
package cpa.invariant.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import common.Pair;

import cfa.objectmodel.CFANode;

import symbpredabstraction.interfaces.SymbolicFormula;

/**
 * Representation of an invariant of the form \land_i. pc = l_i ==> \phi_i
 * 
 * @author g.theoduloz
 */
public class InvariantWithLocation {
  
  // map from location to (conjunctive) list of invariants
  private final Map<CFANode, List<SymbolicFormula>> map;
  private final MathsatInvariantSymbolicFormulaManager manager;
  
  public InvariantWithLocation() {
    map = new HashMap<CFANode, List<SymbolicFormula>>();
    manager = MathsatInvariantSymbolicFormulaManager.getInstance();
  }
  
  /**
   * Return the invariants for a given node as a list
   */
  public List<SymbolicFormula> getInvariants(CFANode node)
  {
    List<SymbolicFormula> result = map.get(node);
    if (result == null)
      return Collections.emptyList();
    else
      return result;
  }
  
  private SymbolicFormula conjunctList(List<SymbolicFormula> list)
  {
    SymbolicFormula result = manager.makeTrue();
    for (SymbolicFormula f : list)
    {
      result = manager.makeAnd(result, f);
    }
    return result;
  }
  
  /**
   * Return the invariant as a formula for a given node
   */
  public SymbolicFormula getInvariant(CFANode node)
  {
    List<SymbolicFormula> invariants = getInvariants(node);
    return conjunctList(invariants);
  }

  /**
   * Add an invariant at the given location
   */
  public void addInvariant(CFANode node, SymbolicFormula invariant)
  {
    if (!manager.entails(manager.makeTrue(), invariant)) {
      List<SymbolicFormula> list = map.get(node);
      if (list == null) {
        list = new LinkedList<SymbolicFormula>();
        map.put(node, list);
      }
      for (SymbolicFormula other : list) {
        if (invariant.equals(other))
          return; // already in the list
      }
      list.add(invariant);
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
      for (Entry<CFANode, List<SymbolicFormula>> entry : map.entrySet()) {
        out.append("pc = ");
        out.append(Integer.toString(entry.getKey().getNodeNumber()));
        out.append(" ===>   ");
        out.append(conjunctList(entry.getValue()).toString());
        out.append("\n");
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
