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
package org.sosy_lab.cpachecker.util.assumptions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;

/**
 * Representation of an assumption of the form \land_i. pc = l_i ==> \phi_i
 */
public class AssumptionWithLocation {

  private final FormulaManager manager;

  // map from location to (conjunctive) list of invariants
  private final Map<CFANode, Formula> map = new HashMap<CFANode, Formula>();

  public AssumptionWithLocation(FormulaManager pManager) {
    manager = pManager;
  }

  public static AssumptionWithLocation copyOf(AssumptionWithLocation a) {
    AssumptionWithLocation result = new AssumptionWithLocation(a.manager);
    result.map.putAll(a.map);
    return result;
  }

  /**
   * Return the number of locations for which we have an assumption.
   */
  public int getNumberOfLocations() {
    return map.size();
  }

  @Override
  public String toString() {
    return Joiner.on('\n').join(Collections2.transform(map.entrySet(), assumptionFormatter));
  }

  private static final Function<Entry<CFANode, Formula>, String> assumptionFormatter
      = new Function<Entry<CFANode, Formula>, String>() {

    @Override
    public String apply(Map.Entry<CFANode, Formula> entry) {
      int nodeId = entry.getKey().getNodeNumber();
      Formula assumption = entry.getValue();
      return "pc = " + nodeId + "\t =====>  " + assumption.toString();
    }
  };

  public void add(CFANode node, Formula assumption) {
    checkNotNull(node);
    checkNotNull(assumption);

    if (!assumption.isTrue()) {
      Formula oldInvariant = map.get(node);
      if (oldInvariant == null) {
        map.put(node, assumption);
      } else {
        map.put(node, manager.makeAnd(oldInvariant, assumption));
      }
    }
  }
}
