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

import java.util.LinkedList;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecificationVisitor;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Union;
import org.sosy_lab.cpachecker.fshell.fql2.ast.filter.Identity;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPatternVisitor;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Repetition;

public class InfeasibilityPropagation {

  public static Pair<Boolean, LinkedList<Edges>> canApplyInfeasibilityPropagation(FQLSpecification pFQLSpecification) {
    if (pFQLSpecification.hasPassingClause()) {
      return Pair.of(Boolean.FALSE, null); // TODO think about that
    }

    // TODO check for predication filter ?

    CoverageSpecification lCovers = pFQLSpecification.getCoverageSpecification();

    LinkedList<CoverageSpecification> lSequence = extractSequence(lCovers);

    if (lSequence == null) {
      return Pair.of(Boolean.FALSE, null);
    }

    LinkedList<Edges> lSubgoals = new LinkedList<Edges>();

    for (CoverageSpecification lElement : lSequence) {
      if (lElement instanceof Edges) {
        lSubgoals.add((Edges)lElement);
      }
    }

    return Pair.of(Boolean.TRUE, lSubgoals);
  }

  private static LinkedList<CoverageSpecification> extractSequence(CoverageSpecification pCoverageSpecification) {
    MyVisitor lVisitor = new MyVisitor();

    pCoverageSpecification.accept(lVisitor);

    if (lVisitor.mUseable) {
      boolean lHasToBeIdStar = true;

      for (CoverageSpecification lSubspecification : lVisitor.mSequence) {
        if (lHasToBeIdStar) {
          if (lSubspecification instanceof Quotation) {
            Quotation lQuotation = (Quotation)lSubspecification;

            if (lQuotation.getPathPattern().accept(IsIdStarVisitor.INSTANCE)) {
              lHasToBeIdStar = false;
            }

            break;
          }
          else {
            break;
          }
        }
        else {
          if (lSubspecification instanceof Edges) {
            lHasToBeIdStar = true;
          }
          else {
            break;
          }
        }
      }

      if (!lHasToBeIdStar) {
        return lVisitor.mSequence;
      }
    }

    return null;
  }

  private static class IsIdStarVisitor implements PathPatternVisitor<Boolean> {

    private static IsIdStarVisitor INSTANCE = new IsIdStarVisitor();

    private boolean mInRepetition = false;

    @Override
    public Boolean visit(
        org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Concatenation pConcatenation) {
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(Repetition pRepetition) {
      mInRepetition = true;
      Boolean lResult = pRepetition.getSubpattern().accept(this);
      mInRepetition = false;

      return lResult;
    }

    @Override
    public Boolean visit(
        org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Union pUnion) {
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(Edges pEdges) {
      if (!mInRepetition) {
        return Boolean.FALSE;
      }

      return pEdges.getFilter().equals(Identity.getInstance());
    }

    @Override
    public Boolean visit(Nodes pNodes) {
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(Paths pPaths) {
      return Boolean.FALSE;
    }

    @Override
    public Boolean visit(Predicate pPredicate) {
      return Boolean.FALSE;
    }

  }

  private static class MyVisitor implements CoverageSpecificationVisitor<Void> {

    LinkedList<CoverageSpecification> mSequence = new LinkedList<CoverageSpecification>();
    boolean mUseable = true;

    @Override
    public Void visit(Concatenation pConcatenation) {
      pConcatenation.getFirstSubspecification().accept(this);
      pConcatenation.getSecondSubspecification().accept(this);

      return null;
    }

    @Override
    public Void visit(Quotation pQuotation) {
      mSequence.addLast(pQuotation);

      return null;
    }

    @Override
    public Void visit(Union pUnion) {
      // TODO think about that again
      mUseable = false;

      return null;
    }

    @Override
    public Void visit(Edges pEdges) {
      mSequence.addLast(pEdges);

      return null;
    }

    @Override
    public Void visit(Nodes pNodes) {
      // TODO think about that again
      mUseable = false;

      return null;
    }

    @Override
    public Void visit(Paths pPaths) {
      // TODO think about that again
      mUseable = false;

      return null;
    }

    @Override
    public Void visit(Predicate pPredicate) {
      // TODO think about that again
      mUseable = false;

      return null;
    }

  }

}
