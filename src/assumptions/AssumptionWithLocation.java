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
import java.util.Collections;
import java.util.Map.Entry;

import symbpredabstraction.interfaces.SymbolicFormula;
import cfa.objectmodel.CFANode;

/**
 * Representation of an assumption of the form \land_i. pc = l_i ==> \phi_i
 * 
 * @author g.theoduloz
 */
public abstract class AssumptionWithLocation {
  
  /**
   * Conjunct this assumption with the given assumption
   */
  public abstract AssumptionWithLocation and(AssumptionWithLocation other);
  
  /**
   * Return the assumption as a formula for a given node
   */
  public abstract Assumption getAssumption(CFANode node);
  
  /**
   * Returns an iterator over assumptions per location 
   */
  public abstract Iterable<Entry<CFANode, Assumption>> getAssumptionsIterator();
  
  /**
   * Dump the assumption to the given Appendable object
   * (e.g., PrintStream, Writer, etc.)
   * IOException are ignored.
   */
  public void dump(Appendable out)
  {
    try {
      for (Entry<CFANode, Assumption> entry : getAssumptionsIterator()) {
        String nodeId = Integer.toString(entry.getKey().getNodeNumber());
        Assumption inv = entry.getValue();
        SymbolicFormula disInv = inv.getDischargeableFormula();
        SymbolicFormula otherInv = inv.getOtherFormula();
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
  
  public static final AssumptionWithLocation TRUE =
    new AssumptionWithLocation() {
      @Override
      public AssumptionWithLocation and(AssumptionWithLocation other) {
        return other;
      }
      @Override
      public Assumption getAssumption(CFANode node) {
        return Assumption.TRUE;
      }
      @Override
      public Iterable<Entry<CFANode, Assumption>> getAssumptionsIterator() {
        return Collections.emptySet();
      }
      @Override
      public boolean equals(Object other) {
        return other == this;
      }
      @Override
      public String toString() {
        return "TRUE";
      }
  };
  
  public static final AssumptionWithLocation FALSE =
    new AssumptionWithLocation() {
      @Override
      public AssumptionWithLocation and(AssumptionWithLocation other) {
        return this;
      }
      @Override
      public Assumption getAssumption(CFANode node) {
        return Assumption.FALSE;
      }
      @Override
      public Iterable<Entry<CFANode, Assumption>> getAssumptionsIterator() {
        return Collections.emptySet();
      }
      @Override
      public boolean equals(Object other) {
        return other == this;
      }
      @Override
      public String toString() {
        return "FALSE";
      }
  };
  
}
