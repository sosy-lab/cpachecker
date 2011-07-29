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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdge;


public abstract class  RelyGuaranteePathFormulaBuilder {

  public  RelyGuaranteePathFormulaBuilder extendByEdge(CFAEdge edge){
    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge) {
      RelyGuaranteeCFAEdge rgEdge = (RelyGuaranteeCFAEdge) edge;
      return new RelyGuaranteeEnvTransitionBuilder(this, rgEdge);
    } else {
      return new RelyGuaranteeLocalTransitionBuilder(this, edge);
    }
  }

  public RelyGuaranteePathFormulaBuilder mergeWith(RelyGuaranteePathFormulaBuilder reached){
    return new RelyGuaranteeMergeBuilder(this, reached);
  }

}


class RelyGuaranteeLocalPathFormulaBuilder extends  RelyGuaranteePathFormulaBuilder {

  private  PathFormula pFormula;

  public RelyGuaranteeLocalPathFormulaBuilder(PathFormula pFormula) {
    this.pFormula = pFormula;
  }

  public PathFormula getPathFormula() {
    return pFormula;
  }

  @Override
  public String toString(){
    return pFormula.toString();
  }
}

class RelyGuaranteeLocalTransitionBuilder extends  RelyGuaranteePathFormulaBuilder {

  private  RelyGuaranteePathFormulaBuilder builder;
  private  CFAEdge  edge;

  public RelyGuaranteeLocalTransitionBuilder(RelyGuaranteePathFormulaBuilder oldBuilder, CFAEdge pEdge) {
    builder = oldBuilder;
    edge = pEdge;
  }

  public RelyGuaranteeLocalTransitionBuilder(RelyGuaranteePathFormulaBuilder oldBuilder) {
    builder = oldBuilder;
    edge = null;
  }

  public RelyGuaranteePathFormulaBuilder getBuilder() {
    return builder;
  }

  public CFAEdge getEdge() {
    return edge;
  }

  @Override
  public String toString(){
    return edge.getRawStatement()+","+builder;
  }

}

class RelyGuaranteeEnvTransitionBuilder extends  RelyGuaranteePathFormulaBuilder {

  private  RelyGuaranteePathFormulaBuilder builder;
  private  RelyGuaranteeCFAEdge envEdge;

  public RelyGuaranteeEnvTransitionBuilder(RelyGuaranteePathFormulaBuilder oldBuilder, RelyGuaranteeCFAEdge rgEdge) {
    builder = oldBuilder;
    envEdge = rgEdge;
  }

  public RelyGuaranteeCFAEdge getEnvEdge() {
    return envEdge;
  }

  public RelyGuaranteePathFormulaBuilder getBuilder() {
    return builder;
  }

  @Override
  public String toString(){
    return "env:+"+envEdge.getLocalEdge()+","+builder;
  }


}

class RelyGuaranteeMergeBuilder extends  RelyGuaranteePathFormulaBuilder {

  private  RelyGuaranteePathFormulaBuilder builder1;
  private  RelyGuaranteePathFormulaBuilder builder2;

  public RelyGuaranteeMergeBuilder(RelyGuaranteePathFormulaBuilder successor, RelyGuaranteePathFormulaBuilder reached) {
    builder1 = successor;
    builder2 = reached;
  }

  public RelyGuaranteePathFormulaBuilder getBuilder1() {
    return builder1;
  }

  public RelyGuaranteePathFormulaBuilder getBuilder2() {
    return builder2;
  }

  @Override
  public String toString(){
    return builder1+" | "+builder2;
  }

}