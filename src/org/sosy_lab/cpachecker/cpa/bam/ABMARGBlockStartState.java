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
package org.sosy_lab.cpachecker.cpa.abm;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class ABMARGBlockStartState extends ARGState {

  private static ABMARGBlockStartState dummyElem = null;

  private static final long serialVersionUID = -5143941913753150639L;

  private ARGState analyzedBlock = null;

  public ABMARGBlockStartState(AbstractState pWrappedState, ARGState pParentElement) {
    super(pWrappedState, pParentElement);
  }

  public void setAnalyzedBlock(ARGState pRootOfBlock) {
    analyzedBlock = pRootOfBlock;
  }


  public ARGState getAnalyzedBlock() {
    return analyzedBlock;
  }

  @Override
  public String toString() {
    return "ABMARGBlockStartState " + super.toString();
  }

  public static ABMARGBlockStartState getDummy() {
    return dummyElem;
  }

  public static ABMARGBlockStartState createDummy(AbstractState wrappedState) {
    if (dummyElem == null) {
      dummyElem = new ABMARGBlockStartState(wrappedState, null);
    }
    return dummyElem;
  }
}
