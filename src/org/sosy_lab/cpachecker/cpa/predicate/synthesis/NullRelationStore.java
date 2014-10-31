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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis;

import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.predicate.synthesis.DefaultRelationStore.Relation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;


public class NullRelationStore implements RelationStore, RelationView {

  @Override
  public void addFact(CFAEdge pEdge, SSAMap pSsa) throws UnrecognizedCCodeException {
    return;
  }

  @Override
  public Map<CExpression, Relation> getRelationTo(CExpression pLhs) {
    throw new RuntimeException("Tracking of relations is disabled!");
  }

  @Override
  public Pair<CExpression, Set<CIdExpression>> getInlined(CExpression pLhs, Set<CIdExpression> pIdExprs) {
    throw new RuntimeException("Tracking of relations is disabled!");
  }

  @Override
  public Set<CExpression> getStoredExpressionsWithOrGlobal(Set<CIdExpression> pIds) {
    throw new RuntimeException("Tracking of relations is disabled!");
  }

  @Override
  public void dumpStoreContent(Appendable pTarget) {
    return;
  }

}
