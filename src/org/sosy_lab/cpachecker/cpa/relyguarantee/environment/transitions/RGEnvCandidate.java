/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;

/**
 * Stores information about a transition in an ART that may become an
 * environmental transition.
 */
public class RGEnvCandidate {

  private final ARTElement element;
  private final ARTElement successor;
  private final CFAEdge operation;
  private final RGAbstractElement rgElement;
  private final RGAbstractElement rgSuccessor;
  private final int tid;

  public RGEnvCandidate(ARTElement element, ARTElement successor, CFAEdge operation, int tid){
    this.element     = element;
    this.successor   = successor;
    this.operation   = operation;
    this.rgElement   = AbstractElements.extractElementByType(element, RGAbstractElement.class);
    this.rgSuccessor = AbstractElements.extractElementByType(successor, RGAbstractElement.class);
    this.tid         = tid;
  }

  public ARTElement getElement() {
    return element;
  }

  public ARTElement getSuccessor() {
    return successor;
  }

  public CFAEdge getOperation() {
    return operation;
  }

  public RGAbstractElement getRgElement() {
    return rgElement;
  }

  public RGAbstractElement getRgSuccessor() {
    return rgSuccessor;
  }

  public int getTid() {
    return tid;
  }

  public String toString(){
    return "RGEnvCandidate: "+element.getElementId()+"--"+operation.getRawStatement()+"-->"+successor.getElementId();
  }
}
