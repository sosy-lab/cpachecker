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
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

/**
 * TODO. This is currently broken
 */
public class SymbPredAbstStop implements StopOperator {

    private final SymbPredAbstDomain domain;

    SymbPredAbstStop(SymbPredAbstDomain domain) {
        this.domain = domain;
    }

    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    public <AE extends AbstractElement> boolean stop(AE element,
            Collection<AE> reached, Precision prec) throws CPAException {
        for (AbstractElement e2 : reached) {
            if (stop(element, e2)) {
                return true;
            }
        }
        return false;
    }

    public boolean stop(AbstractElement element, AbstractElement reachedElement)
            throws CPAException {
        SymbPredAbstElement e = (SymbPredAbstElement)element;
        SymbPredAbstElement e2 = (SymbPredAbstElement)reachedElement;
        SymbPredAbstCPA cpa = domain.getCPA();
        AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();

        // coverage test: both elements should refer to the same location,
        // both should have only an abstract formula, and the data region
        // represented by the abstract formula of e should be included
        // in that of e2
        if (e.getLocation().equals(e2.getLocation()) &&
            e.getConcreteFormula().isTrue() &&
            e2.getConcreteFormula().isTrue() &&
            amgr.entails(e.getAbstractFormula(), e2.getAbstractFormula())) {

            LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                           "Element: ", e, " covered by: ", e2);

            return true;
        } else if (e.getLocation().equals(e2.getLocation()) &&
                   e.getCoveredBy() == e2) {
            // TODO Shortcut: basically, when we merge two paths after
            // an if-then-else or a loop, we set the coveredBy of the old one to
            // the new one, so that we can then detect the coverage here.
            // This has to change to something nicer in the future!!
            LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                           "Element: ", e, " covered by: ", e2);

            return true;
        }

        LazyLogger.log(LazyLogger.DEBUG_1, "Element: ", e, " not covered");

        return false;
    }

}
