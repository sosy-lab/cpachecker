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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateMergeOperator;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeMergeOperator extends PredicateMergeOperator {
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

  private RelyGuaranteeCPA cpa;

  public RelyGuaranteeMergeOperator(PredicateCPA pCpa) {
    super(pCpa.getLogger(), pCpa.getPathFormulaManager());
    cpa = (RelyGuaranteeCPA) pCpa;
    try {
      pCpa.getConfiguration().inject(this, RelyGuaranteeMergeOperator.class);
    } catch (InvalidConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }



  @Override
  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {


    RelyGuaranteeAbstractElement elem1 = (RelyGuaranteeAbstractElement)element1;
    RelyGuaranteeAbstractElement elem2 = (RelyGuaranteeAbstractElement)element2;

    // this will be the merged element
    RelyGuaranteeAbstractElement merged;

    if (elem1 instanceof RelyGuaranteeAbstractElement.AbstractionElement || elem2 instanceof RelyGuaranteeAbstractElement.AbstractionElement) {
      // we don't merge if this is an abstraction location
      merged = elem2;
    } else {
      // don't merge if the elements are in different blocks (they have different abstractions)
      if (!elem1.getAbstractionFormula().equals(elem2.getAbstractionFormula())) {
        merged = elem2;

      } else {
        totalMergeTime.start();
        // create a new element

        logger.log(Level.FINEST, "Merging two non-abstraction nodes.");

        int offset = elem1.getPathFormula().getPrimedNo()+1;
        Map<Integer, RelyGuaranteeCFAEdge> mergedPrimedMap = new HashMap<Integer,RelyGuaranteeCFAEdge>(elem2.getPrimedMap().size()+elem1.getPrimedMap().size());
        Map<Integer, Integer> adjustedMap = new HashMap<Integer, Integer>(elem2.getPrimedMap().size());
        for (Integer primeNo : elem2.getPrimedMap().keySet()){
          mergedPrimedMap.put(primeNo+offset, elem2.getPrimedMap().get(primeNo));
          adjustedMap.put(primeNo, primeNo+offset);
        }
        mergedPrimedMap.putAll(elem1.getPrimedMap());

        PathFormula adjustedPF = formulaManager.adjustPrimedNo(elem2.getPathFormula(), adjustedMap);


        PathFormula pathFormula = formulaManager.makeRelyGuaranteeOr(elem1.getPathFormula(), adjustedPF);

        logger.log(Level.ALL, "New path formula is", pathFormula);


        merged = new RelyGuaranteeAbstractElement(adjustedPF, elem1.getAbstractionFormula(), cpa.getThreadId(), mergedPrimedMap);

        // now mark elem1 so that coverage check can find out it was merged
        elem1.setMergedInto(merged);

        totalMergeTime.stop();
      }
    }

    return merged;
  }

  // copy from RelyGuaranteeTransferRelation
  protected boolean doAbstraction(PathFormula pf) {
    boolean result = false;

    // atom number threshold
    boolean athreshold = false;
    if(atomThreshold > 0) {
      athreshold = (this.cpa.formulaManager.countAtoms(pf.getFormula()) >= atomThreshold) ;
      /*if (athreshold) {
        numAtomThreshold++;
      }*/
    }
    // path length treshold
    if (absBlockSize > 0) {
      boolean threshold = (pf.getLength() >= absBlockSize);
      /*if (threshold) {
        numBlkThreshold++;
      }*/

      if (absOnlyIfBoth) {
        result = result && threshold;
      } else {
        result = result || threshold;
      }
    }
    return result || athreshold;
  }

}
