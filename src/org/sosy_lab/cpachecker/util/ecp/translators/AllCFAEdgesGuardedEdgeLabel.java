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
package org.sosy_lab.cpachecker.util.ecp.translators;

import java.util.Collections;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;

public class AllCFAEdgesGuardedEdgeLabel extends GuardedEdgeLabel {

  private static AllCFAEdgesGuardedEdgeLabel sInstance = new AllCFAEdgesGuardedEdgeLabel();

  public static AllCFAEdgesGuardedEdgeLabel getInstance() {
    return sInstance;
  }

  private AllCFAEdgesGuardedEdgeLabel() {
    super(new ECPEdgeSet(Collections.<CFAEdge>emptySet()));
  }

  @Override
  public ECPEdgeSet getEdgeSet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(CFAEdge pCFAEdge) {
    return true;
  }

  @Override
  public boolean equals(Object pOther) {
    return (this == sInstance);
  }

}
