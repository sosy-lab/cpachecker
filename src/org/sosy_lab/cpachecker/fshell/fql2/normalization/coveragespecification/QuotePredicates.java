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
package org.sosy_lab.cpachecker.fshell.fql2.normalization.coveragespecification;

import org.sosy_lab.cpachecker.fshell.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecificationVisitor;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Union;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.CompositeFQLSpecificationRewriter;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.FQLSpecificationRewriter;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.pathpattern.IdentityRewriter;

public class QuotePredicates implements CoverageSpecificationRewriter {

  private final static FQLSpecificationRewriter mFQLRewriter = new CompositeFQLSpecificationRewriter(new QuotePredicates(), IdentityRewriter.getInstance());

  public static FQLSpecificationRewriter getFQLSpecificationRewriter() {
    return mFQLRewriter;
  }

  private final static QuotePredicates mInstance = new QuotePredicates();

  public static QuotePredicates getRewriter() {
    return mInstance;
  }

  private Visitor mVisitor = new Visitor();

  @Override
  public CoverageSpecification rewrite(CoverageSpecification pSpecification) {
    return pSpecification.accept(mVisitor);
  }

  private static class Visitor implements CoverageSpecificationVisitor<CoverageSpecification> {

    @Override
    public Concatenation visit(Concatenation pConcatenation) {
      CoverageSpecification lFirstSubspecification = pConcatenation.getFirstSubspecification();
      CoverageSpecification lSecondSubspecification = pConcatenation.getSecondSubspecification();

      CoverageSpecification lNewFirstSubspecification = lFirstSubspecification.accept(this);
      CoverageSpecification lNewSecondSubspecification = lSecondSubspecification.accept(this);

      if (lNewFirstSubspecification.equals(lFirstSubspecification) && lNewSecondSubspecification.equals(lSecondSubspecification)) {
        return pConcatenation;
      }
      else {
        return new Concatenation(lNewFirstSubspecification, lNewSecondSubspecification);
      }
    }

    @Override
    public Quotation visit(Quotation pQuotation) {
      return pQuotation;
    }

    @Override
    public Union visit(Union pUnion) {
      CoverageSpecification lFirstSubspecification = pUnion.getFirstSubspecification();
      CoverageSpecification lSecondSubspecification = pUnion.getSecondSubspecification();

      CoverageSpecification lNewFirstSubspecification = lFirstSubspecification.accept(this);
      CoverageSpecification lNewSecondSubspecification = lSecondSubspecification.accept(this);

      if (lNewFirstSubspecification.equals(lFirstSubspecification) && lNewSecondSubspecification.equals(lSecondSubspecification)) {
        return pUnion;
      }
      else {
        return new Union(lNewFirstSubspecification, lNewSecondSubspecification);
      }
    }

    @Override
    public Edges visit(Edges pEdges) {
      return pEdges;
    }

    @Override
    public Nodes visit(Nodes pNodes) {
      return pNodes;
    }

    @Override
    public Paths visit(Paths pPaths) {
      return pPaths;
    }

    @Override
    public Quotation visit(Predicate pPredicate) {
      return new Quotation(pPredicate);
    }

  }

}
