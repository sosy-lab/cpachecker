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

import java.util.List;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Represents a formula block.
 */
public class InterpolationDagNode extends InterpolationBlock{

  private final List<InterpolationDagNode> children;
  private final int tid;

  public InterpolationDagNode(PathFormula pf, int primedNo, ARTElement artElement, List<InterpolationDagNode> children, int tid) {
    super(pf, primedNo, artElement, null);
    this.children = children;
    this.tid      = tid;
  }

  public List<InterpolationDagNode> getChildren() {
    return children;
  }

  public String toString() {
    RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RelyGuaranteeAbstractElement.class);
    return "InterpolationDagNode element thread:"+rgElement.getTid()+" id:"+artElement.getElementId()+", trace:"+traceNo;
  }

  public int getTid() {
    return tid;
  }



}
