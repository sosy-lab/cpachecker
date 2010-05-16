/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import org.sosy_lab.cpachecker.fllesh.cpa.edgevisit.Annotations;
import org.sosy_lab.cpachecker.fllesh.ecp.reduced.Atom;
import org.sosy_lab.cpachecker.fllesh.ecp.reduced.Pattern;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.Edge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Filter;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;

public class Translator {

  private Visitor mVisitor;
  private TargetGraph mTargetGraph;
  private org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.Translator mPathPatternTranslator;

  public Translator(CFAFunctionDefinitionNode pMainFunction) {
    mVisitor = new Visitor();

    mTargetGraph = TargetGraph.createTargetGraphFromCFA(pMainFunction);
    mPathPatternTranslator = new org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.Translator(mTargetGraph);
  }

  public Annotations getAnnotations() {
    return mPathPatternTranslator;
  }
  
  public org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.Translator getPathPatternTranslator() {
    return mPathPatternTranslator;
  }

  public Set<Pattern> translate(CoverageSpecification pSpecification) {
    return pSpecification.accept(mVisitor);
  }

  private class Visitor implements ASTVisitor<Set<Pattern>> {

    @Override
    public Set<Pattern> visit(Concatenation pConcatenation) {
      Set<Pattern> lResultSet = new HashSet<Pattern>();

      Set<Pattern> lPrefixSet = pConcatenation.getFirstSubspecification().accept(this);
      Set<Pattern> lSuffixSet = pConcatenation.getSecondSubspecification().accept(this);

      for (Pattern lPrefix : lPrefixSet) {
        for (Pattern lSuffix : lSuffixSet) {
          Pattern lConcatenation = new org.sosy_lab.cpachecker.fllesh.ecp.reduced.Concatenation(lPrefix, lSuffix);
          lResultSet.add(lConcatenation);
        }
      }

      return lResultSet;
    }

    @Override
    public Set<Pattern> visit(Quotation pQuotation) {
      Pattern pPattern = mPathPatternTranslator.translate(pQuotation.getPathPattern());

      return Collections.singleton(pPattern);
    }

    @Override
    public Set<Pattern> visit(Union pUnion) {
      Set<Pattern> lResultSet = new HashSet<Pattern>();

      Set<Pattern> lFirstSet = pUnion.getFirstSubspecification().accept(this);
      Set<Pattern> lSecondSet = pUnion.getSecondSubspecification().accept(this);

      lResultSet.addAll(lFirstSet);
      lResultSet.addAll(lSecondSet);

      return lResultSet;
    }

    @Override
    public Set<Pattern> visit(Edges pEdges) {
      Set<Pattern> lResultSet = new HashSet<Pattern>();

      Filter lFilter = pEdges.getFilter();

      TargetGraph lFilteredTargetGraph = mTargetGraph.apply(lFilter);

      for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
        CFAEdge lCFAEdge = lEdge.getCFAEdge();

        lResultSet.add(new Atom(mPathPatternTranslator.getId(lCFAEdge)));
      }

      return lResultSet;
    }

    @Override
    public Set<Pattern> visit(Nodes pNodes) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<Pattern> visit(Paths pPaths) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<Pattern> visit(Predicate pPredicate) {
      throw new UnsupportedOperationException();
    }

  }

}
