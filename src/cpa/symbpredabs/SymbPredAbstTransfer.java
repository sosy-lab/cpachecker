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

import java.util.ArrayList;
import java.util.List;

import logging.CPACheckerLogger;
import logging.CustomLogLevel;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFANode;

import exceptions.CPATransferException;
import exceptions.UnrecognizedCFAEdgeException;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import common.Pair;

/**
 * TODO. This is currently broken
 */
public class SymbPredAbstTransfer implements TransferRelation {

	private final SymbPredAbstDomain domain;

	public SymbPredAbstTransfer(SymbPredAbstDomain domain) {
		this.domain = domain;
	}

	public AbstractDomain getAbstractDomain() {
		return domain;
	}

	private AbstractElement buildSuccessor(SymbolicFormulaManager mgr,
			SymbPredAbstElement e,
			CFAEdge curEdge) {
		// Ok, found. What to do here depends on whether we have
		// computed the abstraction at the previous step or not.
		// If yes, we have to set the right parent pointer
		SymbPredAbstElement parent = e.getParent();
		if (e.getConcreteFormula().isTrue()) {
			parent = e;
		}
		try {
			Pair<SymbolicFormula, SSAMap> p =
				mgr.makeAnd(e.getConcreteFormula(), curEdge, e.getSSAMap(), false, true);
			SymbPredAbstElement ret = new SymbPredAbstElement(
					curEdge.getSuccessor(),
					p.getFirst(), e.getAbstractFormula(),
					parent, p.getSecond());
			// if the destination is an error location, we want to check for
			// feasibility of the path
			if (curEdge.getSuccessor() instanceof CFAErrorNode) {
				CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
						"Edge leads to Error Location, checking " +
				"feasibility...");
				SymbolicFormula f = ret.getFormula();
				if (!mgr.entails(mgr.makeTrue(), f)) {
					// if the path is infeasible, we return the bottom element
					// as successor
					CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
					"OK, Error Location UNREACHABLE");
					return domain.getBottomElement();
				} else {
					CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
					"ERROR LOCATION IS REACHABLE");
				}
			}
			return ret;
		} catch (UnrecognizedCFAEdgeException exc) {
			CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
					"UNRECOGNIZED CFA EDGE: " + exc.toString());
			return null;
		}
	}

	public AbstractElement getAbstractSuccessor(AbstractElement element,
			CFAEdge cfaEdge, Precision prec) throws CPATransferException {
		SymbPredAbstElement e = (SymbPredAbstElement)element;

		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
				"Getting Abstract Successor of element: " + e.toString() +
				" on edge: " + cfaEdge.getRawStatement());

		CFANode node = e.getLocation();
		SymbolicFormulaManager mgr = domain.getCPA().getFormulaManager();

		for (int i = 0; i < node.getNumLeavingEdges(); ++i) {
			CFAEdge curEdge = node.getLeavingEdge(i);
			if (curEdge == cfaEdge) {
				AbstractElement ret = buildSuccessor(mgr, e, curEdge);

				CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
						"    Successor is: " + ret.toString());

				return ret;
			}
		}

		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
		"    Successor is: BOTTOM");

		return domain.getBottomElement();
	}

	public List<AbstractElementWithLocation> getAllAbstractSuccessors(
	    AbstractElementWithLocation element, Precision prec) throws CPAException, CPATransferException {
		SymbPredAbstElement e = (SymbPredAbstElement)element;

		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
				"Getting ALL Abstract Successors of element: " + e.toString());

		List<AbstractElementWithLocation> allSucc = new ArrayList<AbstractElementWithLocation>();
		CFANode n = e.getLocation();
		SymbolicFormulaManager mgr = domain.getCPA().getFormulaManager();

		for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
			allSucc.add((SymbPredAbstElement)buildSuccessor(mgr, e, n.getLeavingEdge(i)));
		}

		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
				"    " + Integer.toString(allSucc.size()) +
		" successors found");

		return allSucc;
	}

}
