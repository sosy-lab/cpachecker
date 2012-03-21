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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.util.LinkedHashMap;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.collect.ImmutableMap;

@Options(prefix="cpa.rg")
public class RGMergeOperator implements MergeOperator {
  @Option(name="blk.threshold",
      description="maximum blocksize before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
  private int absBlockSize = 0;

  @Option(name="blk.atomThreshold",
      description="maximum number of atoms in a path formula before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
  private int atomThreshold = 0;

  @Option(name="blk.functions",
      description="force abstractions on function call/return")
  private boolean absOnFunction = true;

  @Option(name="blk.loops",
      description="force abstractions for each loop iteration")
  private boolean absOnLoop = true;

  @Option(name="blk.requireThresholdAndLBE",
      description="require that both the threshold and (functions or loops) "
        + "have to be fulfilled to compute an abstraction")
  private boolean absOnlyIfBoth = false;

  private RGCPA cpa;
  protected final LogManager logger;
  protected final PathFormulaManager formulaManager;
  public final Timer totalMergeTime = new Timer();

  public RGMergeOperator(RGCPA pCpa) {
    cpa = pCpa;
    logger = pCpa.logger;
    formulaManager = pCpa.pfManager;
    try {
      pCpa.getConfiguration().inject(this, RGMergeOperator.class);
    } catch (InvalidConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {


    RGAbstractElement elem1 = (RGAbstractElement)element1;
    RGAbstractElement elem2 = (RGAbstractElement)element2;

    // this will be the merged element
    RGAbstractElement merged;

    if (elem1 instanceof RGAbstractElement.AbstractionElement || elem2 instanceof RGAbstractElement.AbstractionElement) {
      // we don't merge if this is an abstraction location
      merged = elem2;
    } else {
      // don't merge if the elements are in different blocks (they have different abstractions)
      if (!elem1.getAbstractionFormula().equals(elem2.getAbstractionFormula())) {
        merged = elem2;
      }  else {
        totalMergeTime.start();
        // create a new element

        PathFormula mergedAbsPf = formulaManager.makeOr(elem1.getAbsPathFormula(), elem2.getAbsPathFormula());
        PathFormula mergedRefPf = formulaManager.makeOr(elem1.getRefPathFormula(), elem2.getRefPathFormula());

        // merge keys
        ImmutableMap<Integer, RGEnvTransition> imMap;

        ImmutableMap<Integer, RGEnvTransition> map1 = elem1.getEnvApplicationMap();
        ImmutableMap<Integer, RGEnvTransition> map2 = elem2.getEnvApplicationMap();

        if (map1.isEmpty()){
          imMap = map2;
        }
        else if (map2.isEmpty()){
          imMap = map1;
        }
        else {
          Map<Integer, RGEnvTransition> mergedMap = new LinkedHashMap<Integer, RGEnvTransition>(map1);
          mergedMap.putAll(map2);
          imMap = ImmutableMap.copyOf(mergedMap);
        }

        int blkItpSize = elem1.getBlockItpSize();

        merged = new RGAbstractElement(mergedAbsPf, mergedRefPf, elem1.getAbstractionFormula(), imMap, blkItpSize);

        // now mark elem1 so that coverage check can find out it was merged
        elem1.setMergedInto(merged);

        totalMergeTime.stop();
      }
    }

    return merged;
  }

}
