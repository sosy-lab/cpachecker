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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

/**
 * Representation of an assumption of the form \land_i. pc = l_i ==> \phi_i
 *
 * All instances of this class are immutable.
 * 
 * @author g.theoduloz
 */
public abstract class AssumptionWithLocation {

  private AssumptionWithLocation() {  }

  /**
   * Return the assumption as a formula for a given node
   */
  public abstract Assumption getAssumption(CFANode node);

  /**
   * Return the conjunction of all assumptions for all nodes.
   */
  public abstract Assumption getCombinedAssumption();
  
  /**
   * Return the number of locations for which we have an assumption.
   */
  public abstract int getNumberOfLocations();

  public static AssumptionWithLocation emptyAssumption() {
    return emptyAssumptionWithLocation;
  }
  
  public static AssumptionWithLocation forLocation(CFANode location, Assumption assumption) {
    if (assumption.isTrue()) {
      return emptyAssumptionWithLocation;
    } else {
      return new AssumptionWithSingleLocation(location, assumption);
    }
  }
  
  /**
   * Conjunct two assumptions
   */
  public static AssumptionWithLocation and(AssumptionWithLocation pOne, AssumptionWithLocation pTwo) {
    if (pOne == emptyAssumptionWithLocation) {
      return pTwo;
    }
    if (pTwo == emptyAssumptionWithLocation) {
      return pOne;
    }
    if (pOne instanceof AssumptionWithSingleLocation && pTwo instanceof AssumptionWithSingleLocation) {
      AssumptionWithSingleLocation one = (AssumptionWithSingleLocation)pOne;
      AssumptionWithSingleLocation two = (AssumptionWithSingleLocation)pTwo;
      if (one.location.equals(two.location)) {
        return new AssumptionWithSingleLocation(one.location, one.assumption.and(two.assumption));
      }
    }

    // in all other cases
    return new Builder().add(pOne).add(pTwo).build();
  }
  
  public static AssumptionWithLocation.Builder builder() {
    return new Builder();
  }
  
  /**
   * Representation of an assumption of the form pc = l ==> \phi
   *
   * @author g.theoduloz
   */
  private static final AssumptionWithLocation emptyAssumptionWithLocation = new AssumptionWithLocation() {

    @Override
    public Assumption getAssumption(CFANode node) {
      return Assumption.TRUE;
    }

    @Override
    public Assumption getCombinedAssumption() {
      return Assumption.TRUE;
    }
    
    @Override
    public int getNumberOfLocations() {
      return 0;
    }
    
    @Override
    public String toString() {
      return "TRUE";
    }
  };
  
  /**
   * Representation of an assumption of the form pc = l ==> \phi
   *
   * @author g.theoduloz
   */
  private static class AssumptionWithSingleLocation extends AssumptionWithLocation {

    private final CFANode location;
    private final Assumption assumption;

    private AssumptionWithSingleLocation(CFANode pLocation, Assumption pAssumption) {
      Preconditions.checkArgument(!pAssumption.isTrue());
      location = pLocation;
      assumption = pAssumption;
    }

    @Override
    public Assumption getAssumption(CFANode node) {
      if (node == location) {
        return assumption;
      } else {
        return Assumption.TRUE;
      }
    }
    
    @Override
    public Assumption getCombinedAssumption() {
      return assumption;
    }
    
    @Override
    public int getNumberOfLocations() {
      return 1;
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof AssumptionWithSingleLocation) {
        AssumptionWithSingleLocation otherAssumption = (AssumptionWithSingleLocation)other;
        return (otherAssumption.location.equals(location))
          && (otherAssumption.assumption.equals(assumption));
      } else {
        return false;
      }
    }
    
    @Override
    public int hashCode() {
      return assumption.hashCode();
    }
    
    @Override
    public String toString() {
      return assumptionFormatter.apply(new AbstractMap.SimpleEntry<CFANode, Assumption>(location, assumption));
    }
  }

  /**
   * Representation of an assumption of the form \land_i. pc = l_i ==> \phi_i,
   * using a hash map of locations for efficient lookup.
   */
  private static class AssumptionWithMultipleLocations extends AssumptionWithLocation {

    // map from location to (conjunctive) list of invariants
    private final Map<CFANode, Assumption> map;

    private AssumptionWithMultipleLocations(Map<CFANode, Assumption> map) {
      this.map = map;
    }

    /**
     * Return the assumption as a formula for a given node
     */
    @Override
    public Assumption getAssumption(CFANode node) {
      Assumption result = map.get(node);

      if (result == null) {
        return Assumption.TRUE;
      } else {
        return result;
      }
    }
    
    @Override
    public Assumption getCombinedAssumption() {
      Assumption assumption = new Assumption();
      for (Assumption a : map.values()) {
        assumption = assumption.and(a);
      }
      return assumption;
    }
    
    @Override
    public int getNumberOfLocations() {
      return map.size();
    }
    
    @Override
    public String toString() {
      return Joiner.on('\n').join(Collections2.transform(map.entrySet(), assumptionFormatter));
    }
  }
  
  private static final Function<Entry<CFANode, Assumption>, String> assumptionFormatter
      = new Function<Entry<CFANode, Assumption>, String>() {
    
    @Override
    public String apply(Map.Entry<CFANode, Assumption> entry) {
      int nodeId = entry.getKey().getNodeNumber();
      Assumption assumption = entry.getValue();
      SymbolicFormula disInv = assumption.getDischargeableFormula();
      SymbolicFormula otherInv = assumption.getOtherFormula();
      StringBuilder result = new StringBuilder();
      if (!disInv.isTrue()) {
        result.append("pc = ").append(nodeId).append("\t =(d)=>  ");
        result.append(disInv.toString());
        if (!otherInv.isTrue()) {
          result.append('\n');
        }
      }
      if (!otherInv.isTrue()) {
        result.append("pc = ").append(nodeId).append("\t =====>  ");
        result.append(otherInv.toString());
      }
      return result.toString();
    }
  };
  
  public static class Builder {

    private final Map<CFANode, Assumption> assumptions = new HashMap<CFANode, Assumption>();
    
    public Builder add(CFANode node, Assumption assumption) {
      if (!assumption.isTrue()) {
        Assumption oldInvariant = assumptions.get(node);
        if (oldInvariant == null) {
          assumptions.put(node, assumption);
        } else {
          assumptions.put(node, oldInvariant.and(assumption));
        }
      }
      return this;
    }
    
    public Builder add(AssumptionWithLocation assumption) {
      if (assumption instanceof AssumptionWithSingleLocation) {
        AssumptionWithSingleLocation singleLocAssumption = (AssumptionWithSingleLocation)assumption;
        add(singleLocAssumption.location, singleLocAssumption.assumption);
      
      } else if (assumption instanceof AssumptionWithMultipleLocations) {
        AssumptionWithMultipleLocations multipleLocAssumption = (AssumptionWithMultipleLocations)assumption;
        for (Entry<CFANode, Assumption> otherEntry : multipleLocAssumption.map.entrySet()) {
          add(otherEntry.getKey(), otherEntry.getValue());
        }
      } else {
        assert assumption == emptyAssumptionWithLocation;
      }
      return this;
    }
    
    public AssumptionWithLocation build() {
      switch (assumptions.size()) {
      case 0:
        return emptyAssumptionWithLocation;
      
      case 1:    
        Entry<CFANode, Assumption> entry = Iterables.getOnlyElement(assumptions.entrySet());
        return new AssumptionWithSingleLocation(entry.getKey(), entry.getValue());
      
      default:
        return new AssumptionWithMultipleLocations(assumptions);
      }
    }
  }
}
