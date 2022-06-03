// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.assumptions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/** Representation of an assumption of the form \land_i. pc = l_i ==> \phi_i */
public class AssumptionWithLocation implements Appender {

  private final FormulaManagerView manager;

  // map from location to (conjunctive) list of invariants
  private final Map<CFANode, BooleanFormula> map = new HashMap<>();

  public AssumptionWithLocation(FormulaManagerView pManager) {
    manager = pManager;
  }

  public static AssumptionWithLocation copyOf(AssumptionWithLocation a) {
    AssumptionWithLocation result = new AssumptionWithLocation(a.manager);
    result.map.putAll(a.map);
    return result;
  }

  /** Return the number of locations for which we have an assumption. */
  public int getNumberOfLocations() {
    return map.size();
  }

  @Override
  public void appendTo(Appendable out) throws IOException {
    Joiner.on('\n')
        .appendTo(
            out, Collections2.transform(map.entrySet(), AssumptionWithLocation::formatAssumption));
  }

  @Override
  public String toString() {
    return Appenders.toString(this);
  }

  private static String formatAssumption(Entry<CFANode, BooleanFormula> entry) {
    int nodeId = entry.getKey().getNodeNumber();
    BooleanFormula assumption = entry.getValue();
    return "pc = " + nodeId + "\t =====>  " + assumption;
  }

  public void add(CFANode node, BooleanFormula assumption) {
    checkNotNull(node);
    checkNotNull(assumption);
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    if (!bfmgr.isTrue(assumption)) {
      BooleanFormula oldInvariant = map.get(node);
      if (oldInvariant == null) {
        map.put(node, assumption);
      } else {
        map.put(node, bfmgr.and(oldInvariant, assumption));
      }
    }
  }
}
