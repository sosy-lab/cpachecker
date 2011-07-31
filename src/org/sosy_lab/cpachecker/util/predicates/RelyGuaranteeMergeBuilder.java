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

public  class RelyGuaranteeMergeBuilder extends  RelyGuaranteePathFormulaBuilder {

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