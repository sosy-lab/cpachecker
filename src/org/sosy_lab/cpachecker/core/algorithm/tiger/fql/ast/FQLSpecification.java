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
package org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast;

import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.CoverageSpecificationVisitor;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.Union;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.filter.Identity;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.pathpattern.PathPatternVisitor;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.pathpattern.Repetition;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.parser.FQLParser;

public class FQLSpecification {
  private CoverageSpecification mCoverageSpecification;
  private PathPattern mPathPattern;

  private static PathPattern mDefaultPassingClause = new Repetition(new Edges(Identity.getInstance()));

  public static PathPattern getDefaultPassingClause() {
    return mDefaultPassingClause;
  }

  public FQLSpecification(CoverageSpecification pCoverageSpecification, PathPattern pPathPattern) {
    mCoverageSpecification = pCoverageSpecification;
    mPathPattern = pPathPattern;
  }

  public FQLSpecification(CoverageSpecification pCoverageSpecification) {
    mCoverageSpecification = pCoverageSpecification;
    mPathPattern = null;
  }

  public CoverageSpecification getCoverageSpecification() {
    return mCoverageSpecification;
  }

  public boolean hasPassingClause() {
    return (mPathPattern != null);
  }

  public boolean hasPredicate() {
    // TODO implement more efficiently by determining whether predicates are created during parsing

    final PathPatternVisitor<Boolean> pathPatternVisitor = new PathPatternVisitor<Boolean>() {

      @Override
      public Boolean visit(org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.pathpattern.Concatenation pConcatenation) {
        return pConcatenation.getFirstSubpattern().accept(this) || pConcatenation.getSecondSubpattern().accept(this);
      }

      @Override
      public Boolean visit(Repetition pRepetition) {
        return pRepetition.getSubpattern().accept(this);
      }

      @Override
      public Boolean visit(org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.pathpattern.Union pUnion) {
        return pUnion.getFirstSubpattern().accept(this) || pUnion.getSecondSubpattern().accept(this);
      }

      @Override
      public Boolean visit(Edges pEdges) {
        return false;
      }

      @Override
      public Boolean visit(Nodes pNodes) {
        return false;
      }

      @Override
      public Boolean visit(Paths pPaths) {
        return false;
      }

      @Override
      public Boolean visit(Predicate pPredicate) {
        return true;
      }

    };

    CoverageSpecificationVisitor<Boolean> visitor = new CoverageSpecificationVisitor<Boolean>() {

      @Override
      public Boolean visit(Concatenation pConcatenation) {
        return pConcatenation.getFirstSubspecification().accept(this) || pConcatenation.getSecondSubspecification().accept(this);
      }

      @Override
      public Boolean visit(Quotation pQuotation) {
        return pQuotation.getPathPattern().accept(pathPatternVisitor);
      }

      @Override
      public Boolean visit(Union pUnion) {
        return pUnion.getFirstSubspecification().accept(this) || pUnion.getSecondSubspecification().accept(this);
      }

      @Override
      public Boolean visit(Edges pEdges) {
        return false;
      }

      @Override
      public Boolean visit(Nodes pNodes) {
        return false;
      }

      @Override
      public Boolean visit(Paths pPaths) {
        return false;
      }

      @Override
      public Boolean visit(Predicate pPredicate) {
        return true;
      }

    };

    return mCoverageSpecification.accept(visitor) || (mPathPattern != null && mPathPattern.accept(pathPatternVisitor));
  }

  public PathPattern getPathPattern() {
    if (!hasPassingClause()) {
      throw new UnsupportedOperationException();
    }

    return mPathPattern;
  }

  @Override
  public String toString() {
    if (hasPassingClause()) {
      return "COVER " + mCoverageSpecification.toString() + " PASSING " + mPathPattern.toString();
    }
    else {
      return "COVER " + mCoverageSpecification.toString();
    }
  }

  public static FQLSpecification parse(String pFQLSpecificationString) throws Exception {
    FQLParser lParser = new FQLParser(pFQLSpecificationString);

    Object pParseResult;

    try {
      pParseResult = lParser.parse().value;
    }
    catch (Exception e) {
      throw e;
    }

    assert(pParseResult instanceof FQLSpecification);

    return (FQLSpecification)pParseResult;
  }

}
