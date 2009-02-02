/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabs;

import java.util.Collection;

import logging.CustomLogLevel;
import logging.LazyLogger;
import cfa.objectmodel.CFANode;

import common.Pair;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

/**
 * TODO. This is currently broken
 */
public class SymbPredAbstMerge implements MergeOperator {

  private final SymbPredAbstDomain domain;

  public SymbPredAbstMerge(SymbPredAbstDomain domain) {
    this.domain = domain;
  }

  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2,
                               Precision prec) {

    if (element1.equals(domain.getBottomElement())) {
      return element2;
    } else if (element2.equals(domain.getBottomElement())) {
      return element2;
    }

    SymbPredAbstElement e1 = (SymbPredAbstElement)element1;
    SymbPredAbstElement e2 = (SymbPredAbstElement)element2;


    // we can merge two states if their location is the same
    if (e1.getLocation().equals(e2.getLocation())) {
      LazyLogger.log(CustomLogLevel.SpecificCPALevel,
          "Merging elements: ", e1, " and: ", e2);
      // how the merge is performed depends on the kind of location.
      // if we are at a loop start, then we have to compute the
      // abstraction of the combined concrete formula. Otherwise, we
      // do only a "syntactic" merge, without taking the abstraction into
      // account
      CFANode n = e1.getLocation();
      SymbolicFormula f1 = e1.getFormula();
      SymbolicFormula f2 = e2.getFormula();

      SymbPredAbstCPA cpa = domain.getCPA();
      Collection<Predicate> predicates =
        cpa.getPredicateMap().getRelevantPredicates(n);
      SymbolicFormulaManager mgr = cpa.getFormulaManager();
      AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();

      Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> merger =
        mgr.mergeSSAMaps(e1.getSSAMap(), e2.getSSAMap(), true);
      Pair<SymbolicFormula, SymbolicFormula> mf = merger.getFirst();
      SSAMap newssa = merger.getSecond();

      SymbPredAbstElement ret = null;
      if (n.isLoopStart()) {
        ret = new SymbPredAbstElement(n, mgr.makeTrue(),
            amgr.toAbstract(
                mgr,
                mgr.makeOr(mgr.makeAnd(f1, mf.getFirst()),
                    mgr.makeAnd(f2, mf.getSecond())),
                    newssa, predicates), e2.getParent(), newssa);
        if (e2.getConcreteFormula().isTrue()) {
          LazyLogger.log(CustomLogLevel.SpecificCPALevel,
              "CHECKING coverage of 'ret' by 'e2'");
          if (amgr.entails(ret.getAbstractFormula(),
              e2.getAbstractFormula())) {
            LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "YES, COVERED");
            ret = e2;
          } else {
            LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                "NO, not covered");
          }
        }
      } else {
        ret = new SymbPredAbstElement(
            n,
            mgr.makeOr(mgr.makeAnd(f1, mf.getFirst()),
                mgr.makeAnd(f2, mf.getSecond())),
                null, e2.getParent(), newssa);
      }
      // TODO - Shortcut, we set the coveredBy of e1 to be the new created
      // element, so that the stop operator detects that e1 is covered
      // by ret (see SymbPredAbstStop)
      e1.setCoveredBy(ret);

      LazyLogger.log(CustomLogLevel.SpecificCPALevel, "result is: ", ret);

      return ret;
    }

    return e2;
  }

  public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                           AbstractElementWithLocation pElement2,
                                           Precision prec) throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
