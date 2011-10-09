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
package org.sosy_lab.cpachecker.fshell.clustering;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.util.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.util.ecp.ECPRepetition;
import org.sosy_lab.cpachecker.util.ecp.ECPUnion;
import org.sosy_lab.cpachecker.util.ecp.ECPVisitor;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.util.ecp.SingletonECPEdgeSet;

public class ECPDominatorVisitor implements ECPVisitor<List<SingletonECPEdgeSet>> {

  public static ECPDominatorVisitor INSTANCE = new ECPDominatorVisitor();

  private ECPDominatorVisitor() {

  }

  @Override
  public List<SingletonECPEdgeSet> visit(ECPEdgeSet pEdgeSet) {
    if (pEdgeSet instanceof SingletonECPEdgeSet) {
      return Collections.singletonList((SingletonECPEdgeSet)pEdgeSet);
    }

    return Collections.emptyList();
  }

  @Override
  public List<SingletonECPEdgeSet> visit(ECPNodeSet pNodeSet) {
    return Collections.emptyList();
  }

  @Override
  public List<SingletonECPEdgeSet> visit(ECPPredicate pPredicate) {
    return Collections.emptyList();
  }

  @Override
  public List<SingletonECPEdgeSet> visit(ECPConcatenation pConcatenation) {
    List<SingletonECPEdgeSet> lDominators = new LinkedList<SingletonECPEdgeSet>();

    for (ElementaryCoveragePattern lSubpattern : pConcatenation) {
      List<SingletonECPEdgeSet> lSubdominators = lSubpattern.accept(this);
      lDominators.addAll(lSubdominators);
    }

    if (lDominators.isEmpty()) {
      return Collections.emptyList();
    }

    return lDominators;
  }

  @Override
  public List<SingletonECPEdgeSet> visit(ECPUnion pUnion) {
    // TODO query rewriting might improve this case in case it becomes a problem
    return Collections.emptyList();
  }

  @Override
  public List<SingletonECPEdgeSet> visit(ECPRepetition pRepetition) {
    return Collections.emptyList();
  }

}
