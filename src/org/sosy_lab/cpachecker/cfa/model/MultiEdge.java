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
package org.sosy_lab.cpachecker.cfa.model;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * A single edge which represents a sequence of serveral other simple edges of
 * the types
 * BlankEdge
 * DeclarationEdge
 * StatementEdge
 * ReturnStatementEdge
 */
public class MultiEdge extends AbstractCFAEdge implements Iterable<CFAEdge> {

  private final ImmutableList<CFAEdge> edges;

  public MultiEdge(CFANode pPredecessor, CFANode pSuccessor, List<CFAEdge> pEdges) {
    super("", pEdges.get(0).getLineNumber(), pPredecessor, pSuccessor);
    edges = ImmutableList.copyOf(pEdges);
  }

  public ImmutableList<CFAEdge> getEdges() {
    return edges;
  }

  @Override
  public Iterator<CFAEdge> iterator() {
    return edges.iterator();
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.MultiEdge;
  }

  @Override
  public final String getRawStatement() {
    return Joiner.on('\n').join(Lists.transform(edges, new Function<CFAEdge, String>() {

        @Override
        public String apply(CFAEdge pInput) {
          return pInput.getRawStatement();
        }
      }));
  }

  @Override
  public String getCode() {
    return Joiner.on('\n').join(Lists.transform(edges, new Function<CFAEdge, String>() {

        @Override
        public String apply(CFAEdge pInput) {
          return pInput.getCode();
        }
      }));
  }

  @Override
  public String getDescription() {
    return Joiner.on('\n').join(Lists.transform(edges, new Function<CFAEdge, String>() {

        @Override
        public String apply(CFAEdge pInput) {
          return pInput.getDescription();
        }
      }));
  }

  @Override
  public String toString() {
    return Joiner.on('\n').join(edges);
  }
}
