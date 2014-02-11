/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** This class guarantees a fixed order of variables in the BDD,
 * that should be good for the operations in the BitvectorManager. */
@Options(prefix = "cpa.bdd")
public class PredicateManager {

  @Option(description = "declare first bit of all vars, then second bit,...")
  private boolean initBitwise = true;

  @Option(description = "declare the bits of a var from 0 to N or from N to 0")
  private boolean initBitsIncreasing = true;

  @Option(description = "declare partitions ordered")
  private boolean initPartitionsOrdered = true;

  @Option(description = "declare vars partitionwise")
  private boolean initPartitions = true;

  protected static final String TMP_VARIABLE = "__CPAchecker_tmp_var";
  private final Map<Multimap<String, String>, String> varsToTmpVar = new HashMap<>();

  /** Contains the varNames of all really tracked vars.
   * This set may differ from the union of all partitions,
   * because not every variable, that appears in the sourcecode,
   * is analyzed or even reachable. */
  private final Multimap<String, String> trackedVars = LinkedHashMultimap.create();

  private final NamedRegionManager rmgr;

  public PredicateManager(final Configuration config, final NamedRegionManager pRmgr,
                          final BDDPrecision pPrecision, final CFA pCfa,
                          final BDDTransferRelation pTransferRelation
                          ) throws InvalidConfigurationException {
    config.inject(this);
    this.rmgr = pRmgr;

    if (initPartitions) {
      initVars(pPrecision, pCfa, pTransferRelation);
    }
  }

  public Multimap<String, String> getTrackedVars() {
    return trackedVars;
  }

  /** return a specific temp-variable, that should be at correct positions in the BDD. */
  public String getTmpVariableForVars(final Multimap<String, String> vars) {
    if (initPartitions) {
      return varsToTmpVar.get(vars);
    } else {
      return TMP_VARIABLE;
    }
  }


  /** The BDDRegionManager orders the variables as they are declared
   *  (later vars are deeper in the BDD).
   *  This function declares those vars in the beginning of the analysis,
   *  so that we can choose between some orders. */
  protected void initVars(BDDPrecision precision, CFA cfa, BDDTransferRelation transferRelation) {
    List<VariableClassification.Partition> partitions;
    if (initPartitionsOrdered) {
      BDDPartitionOrderer d = new BDDPartitionOrderer(cfa);
      partitions = d.getOrderedPartitions();
    } else {
      assert cfa.getVarClassification().isPresent();
      partitions = cfa.getVarClassification().get().getPartitions(); // may be unsorted
    }

    for (VariableClassification.Partition partition : partitions) {
      // maxBitSize is too much for most variables. we only create an order here, so this should not matter.
      createPredicates(partition.getVars(), precision, transferRelation.getMaxBitsize());
    }
  }

  /** This function declares variables for a given collection of vars.
   *
   * The value 'bitsize' chooses how much bits are used for each var.
   * The varname is build as "varname@pos". */
  public void createPredicates(final Multimap<String, String> vars, final BDDPrecision precision, final int bitsize) {

    assert bitsize >= 1 : "you need at least one bit for a variable.";

    // add a temporary variable for each set of variables, introducing it here is cheap, later it may be  expensive.
    String tmpVar = TMP_VARIABLE + "_" + varsToTmpVar.size();
    varsToTmpVar.put(vars, tmpVar);

    // bitvectors [a2, a1, a0]
    // 'initBitwise' chooses between initialing each var separately or bitwise overlapped.
    if (initBitwise) {

      // [a2, b2, c2, a1, b1, c1, a0, b0, c0]
      boolean isTrackingSomething = false;
      for (int i = 0; i < bitsize; i++) {
        int index = initBitsIncreasing ? i : (bitsize - i - 1);
        for (Map.Entry<String, String> entry : vars.entries()) {
          if (precision.isTracking(entry.getKey(), entry.getValue())) {
            createPredicateDirectly(entry.getKey(), entry.getValue(), index);
            isTrackingSomething = true;
          }
        }
        if (isTrackingSomething) {
          createPredicateDirectly(null, tmpVar, index);
        }
      }

    } else {
      // [a2, a1, a0, b2, b1, b0, c2, c1, c0]
      boolean isTrackingSomething = false;
      for (Map.Entry<String, String> entry : vars.entries()) { // different loop order!
        if (precision.isTracking(entry.getKey(), entry.getValue())) {
          for (int i = 0; i < bitsize; i++) {
            int index = initBitsIncreasing ? i : (bitsize - i - 1);
            createPredicateDirectly(entry.getKey(), entry.getValue(), index);
          }
          isTrackingSomething = true;
        }
      }
      if (isTrackingSomething) {
        for (int i = 0; i < bitsize; i++) {
          int index = initBitsIncreasing ? i : (bitsize - i - 1);
          createPredicateDirectly(null, tmpVar, index);
        }
      }
    }
  }

  /** This function returns a region for a variable.
   * This function does not track any statistics. */
  private Region createPredicateDirectly(@Nullable final String functionName, final String varName, final int index) {
    System.out.println(((functionName == null) ? varName : functionName + "::" + varName) + "@" + index);
    return rmgr.createPredicate(((functionName == null) ? varName : functionName + "::" + varName) + "@" + index);
  }

  /** This function returns regions containing bits of a variable.
   * returns regions for positions of a variable, s --> [s@2, s@1, s@0].
   * There is no check, if the variable is tracked by the the precision. */
  public Region[] createPredicates(@Nullable final String functionName, final String varName, final int size) {
    trackedVars.put(functionName, varName);
    final Region[] newRegions = new Region[size];
    for (int i = size - 1; i >= 0; i--) {
      // inverse order should be faster, because 'most changing bits' are at bottom position in BDDs.
      newRegions[i] = createPredicateDirectly(functionName, varName, i);
    }
    return newRegions;
  }
}
