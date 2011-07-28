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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;

@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteePathFormulaConstructor {

  @Option(description="List of variables global to multiple threads")
  protected String[] globalVariables = {};

  private FormulaManager fManager;
  private PathFormulaManager pfManager;
  private Set<String> globalVariablesSet;

  private static RelyGuaranteePathFormulaConstructor constructor;

  public static RelyGuaranteePathFormulaConstructor getInstance(Configuration config, LogManager logger){
    if (constructor == null){
      constructor = new RelyGuaranteePathFormulaConstructor(config, logger);
    }
    return constructor;
  }

  public RelyGuaranteePathFormulaBuilder createEmpty(){
    PathFormula empty = pfManager.makeEmptyPathFormula();
    return new RelyGuaranteeLocalPathFormulaBuilder(empty);
  }

  public RelyGuaranteePathFormulaBuilder createEmpty(PathFormula pf){
    PathFormula empty = pfManager.makeEmptyPathFormula(pf);
    return new RelyGuaranteeLocalPathFormulaBuilder(empty);
  }

  private RelyGuaranteePathFormulaConstructor(Configuration config, LogManager logger){
    try {
      config.inject(this, RelyGuaranteePathFormulaConstructor.class);
      // build the set of global variables
      globalVariablesSet = new HashSet<String>();
      for (String var : globalVariables){
        globalVariablesSet.add(var);
      }
      // set up managers
      fManager =  MathsatFormulaManager.getInstance(config, logger);
      PathFormulaManager pfMgr  = PathFormulaManagerImpl.getInstance(fManager, config, logger);
      pfManager = CachingPathFormulaManager.getInstance(pfMgr);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
  }

  // returns the number of env transition in the subtree
  // TODO make it non-recurise or possible delete it
  public int envTransitionsNo(RelyGuaranteePathFormulaBuilder builder) {
    if (builder instanceof RelyGuaranteeLocalPathFormulaBuilder){
      return 0;
    }
    if (builder instanceof RelyGuaranteeLocalTransitionBuilder){
      RelyGuaranteeLocalTransitionBuilder currentB = (RelyGuaranteeLocalTransitionBuilder) builder;
      RelyGuaranteePathFormulaBuilder nextB = currentB.getBuilder();
      return envTransitionsNo(nextB);
    }
    if (builder instanceof RelyGuaranteeEnvTransitionBuilder){
      RelyGuaranteeEnvTransitionBuilder currentB = (RelyGuaranteeEnvTransitionBuilder) builder;
      RelyGuaranteePathFormulaBuilder nextB = currentB.getBuilder();
      return envTransitionsNo(nextB)+1;
    }
    if (builder instanceof RelyGuaranteeMergeBuilder){
      RelyGuaranteeMergeBuilder currentB = (RelyGuaranteeMergeBuilder) builder;
      RelyGuaranteePathFormulaBuilder nextB1 = currentB.getBuilder1();
      RelyGuaranteePathFormulaBuilder nextB2 = currentB.getBuilder2();
      return envTransitionsNo(nextB1)+envTransitionsNo(nextB2);
    };
    return -1;

  }

  // TODO make it non-recursive
  public PathFormula construct(RelyGuaranteePathFormulaBuilder builder) throws CPATransferException{
    if (builder instanceof RelyGuaranteeLocalPathFormulaBuilder){
      return ((RelyGuaranteeLocalPathFormulaBuilder) builder).getPathFormula();
    }
    if (builder instanceof RelyGuaranteeLocalTransitionBuilder){
      RelyGuaranteeLocalTransitionBuilder currentB = (RelyGuaranteeLocalTransitionBuilder) builder;
      RelyGuaranteePathFormulaBuilder nextB = currentB.getBuilder();
      PathFormula nextPF = construct(nextB);
      CFAEdge edge = currentB.getEdge();
      PathFormula currentPF = pfManager.makeAnd(nextPF, edge);
      return currentPF;
    }
    if (builder instanceof RelyGuaranteeEnvTransitionBuilder){
      RelyGuaranteeEnvTransitionBuilder currentB = (RelyGuaranteeEnvTransitionBuilder) builder;
      RelyGuaranteePathFormulaBuilder nextB = currentB.getBuilder();
      PathFormula nextPF = construct(nextB);
      RelyGuaranteeCFAEdge rgEdge = currentB.getEnvEdge();
      PathFormula envPF = rgEdge.getPathFormula();
      // prime the env. path formula so it does not collide with the local path formula
      int offset = nextPF.getPrimedNo() + 1;
      PathFormula primedEnvPF = pfManager.primePathFormula(envPF, offset);
      // make equalities between the last global values in the local and env. path formula
      PathFormula matchedPF = pfManager.matchPaths(nextPF, primedEnvPF, globalVariablesSet);
      // apply the strongest postcondition
      CFAEdge injectedEdge = pfManager.inject(rgEdge.getLocalEdge(), globalVariablesSet, offset, primedEnvPF.getSsa());
      PathFormula finalPF = pfManager.makeAnd(matchedPF, rgEdge.getLocalEdge());
      return finalPF;
    }
    if (builder instanceof RelyGuaranteeMergeBuilder){
      RelyGuaranteeMergeBuilder currentB = (RelyGuaranteeMergeBuilder) builder;
      RelyGuaranteePathFormulaBuilder nextB1 = currentB.getBuilder1();
      RelyGuaranteePathFormulaBuilder nextB2 = currentB.getBuilder2();
      PathFormula nextPF1 = construct(nextB1);
      PathFormula nextPF2 = construct(nextB2);
      PathFormula finalPF = pfManager.makeOr(nextPF1, nextPF2);
      return finalPF;
    } else {
      throw new UnrecognizedCFAEdgeException("");
    }

  }



}
