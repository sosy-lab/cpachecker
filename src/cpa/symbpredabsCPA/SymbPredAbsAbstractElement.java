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
package cpa.symbpredabsCPA;

import symbpredabstraction.ParentsList;
import symbpredabstraction.PathFormula;
import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.mathsat.BDDAbstractFormula;

/**
 * AbstractElement for summary cpa
 *
 * @author erkan
 */
public class SymbPredAbsAbstractElement
implements AbstractElement {

	private SymbPredAbsAbstractDomain domain;

	/** Unique state id*/
	private int elementId;
	/** If the element is on an abstraction location */
	private boolean isAbstractionNode = false;
	/** The abstraction location for this node */
	private CFANode abstractionLocation;
	/** the path formula from the abstraction location to this node */
	private PathFormula pathFormula;
	/** initial abstraction values*/
	private PathFormula initAbstractionFormula;
	/** the abstraction which is updated only on abstraction locations */
	private AbstractFormula abstraction;
	/** parents of this element */
	private ParentsList parents;
	/** parent of this element on ART*/
	private SymbPredAbsAbstractElement artParent;
	/** predicate list for this element*/
	private PredicateMap predicates;

	private SSAMap maxIndex;

	public boolean isBottomElement = false;
	private static int nextAvailableId = 1;

	public PathFormula getPathFormula() {
		return pathFormula;
	}

	public void setAbstractionNode(){
		isAbstractionNode = true;
	}

	public boolean isAbstractionNode(){
		return isAbstractionNode;
	}

	public AbstractFormula getAbstraction() {
		return abstraction;
	}

	public void setAbstraction(AbstractFormula a) {
		abstraction = a;
	}
	public void setPathFormula(PathFormula pf){
		pathFormula = pf;
	}

	public ParentsList getParents() {
		return parents;
	}

	public void addParent(Integer i) {
		parents.addToList(i);
	}

	public boolean isDescendant(SymbPredAbsAbstractElement c) {
		SymbPredAbsAbstractElement a = this;
		while (a != null) {
			if (a.equals(c)) return true;
			a = a.getArtParent();
		}
		return false;
	}

	public CFANode getAbstractionLocation(){
		return abstractionLocation;
	}

	public void setAbstractionLocation(CFANode absLoc){
		abstractionLocation = absLoc;
	}

	public SymbPredAbsAbstractElement(AbstractDomain d, boolean isAbstractionElement, CFANode abstLoc,
			PathFormula pf, PathFormula initFormula, AbstractFormula a, 
			ParentsList pl, SymbPredAbsAbstractElement artParent, PredicateMap pmap){
		this.elementId = nextAvailableId++;
		this.domain = (SymbPredAbsAbstractDomain)d;
		this.isAbstractionNode = isAbstractionElement;
		this.abstractionLocation = abstLoc;
		this.pathFormula = pf;
		this.initAbstractionFormula = initFormula;
		this.abstraction = a;
		this.parents = pl;
		this.artParent = artParent;
		this.predicates = pmap;
		this.maxIndex = new SSAMap();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		else if(elementId == ((SymbPredAbsAbstractElement)o).elementId){
			return true;
		}

		else{
			SymbPredAbsAbstractElement thisElement = this;
			SymbPredAbsAbstractElement otherElement = (SymbPredAbsAbstractElement)o;

			boolean b = thisElement.isAbstractionNode();
			// if not an abstraction location
			if(!b){
				if(thisElement.getParents().equals(otherElement.getParents())){
					SymbolicFormulaManager mgr = domain.getCPA().getFormulaManager();
					boolean ok = mgr.entails(thisElement.getPathFormula().getSymbolicFormula(),
							otherElement.getPathFormula().getSymbolicFormula()) && 
							mgr.entails(otherElement.getPathFormula().getSymbolicFormula(),
									thisElement.getPathFormula().getSymbolicFormula());
//					// TODO later
////if (ok)
////	{
////					cpa.setCoveredBy(thisElement, otherElement);
////					} else {
////					LazyLogger.log(CustomLogLevel.SpecificCPALevel,
////					"NO, not covered");
////					}
//					return ok;
//					}
//					else{
//					return false;
					return ok;
				}
				return false;
			}
			// if abstraction location
			else{

				// SymbPredAbsCPA cpa = domain.getCPA();

				assert(thisElement.getAbstraction() != null);
				assert(otherElement.getAbstraction() != null);
				if(!thisElement.getParents().equals(otherElement.getParents())){
					return false;
				}
				// TODO check -- we are calling the equals method of the abstract formula
				boolean ok = thisElement.getAbstraction().equals(otherElement.getAbstraction());

				// TODO
//				if (ok) {
//				LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//				"Element: ", element, " COVERED by: ", e2);
//				cpa.setCoveredBy(e1, e2);
//				} else {
//				LazyLogger.log(CustomLogLevel.SpecificCPALevel,
//				"NO, not covered");
//				}
				return ok;
			}
		}
	}

	@Override
	public String toString() {
		BDDAbstractFormula abst = (BDDAbstractFormula)getAbstraction();
		SymbolicFormula symbReprAbst = domain.getCPA().getAbstractFormulaManager().toConcrete(domain.getCPA().getSymbolicFormulaManager(), abst);
		return
		" Abstraction LOCATION: " + getAbstractionLocation() +
		" PF: "+ getPathFormula().getSymbolicFormula() +
		" Abstraction: " + symbReprAbst +
		" Init Formula--> " + (getInitAbstractionSet() != null ? getInitAbstractionSet().getSymbolicFormula() : "null")  +
		" Parents --> " + parents + 
		//" ART Parent --> " + getArtParent() + 
		"\n \n";
		//+ ">(" + Integer.toString(getId()) + ")"
	}

	@Override
	public int hashCode() {
		return elementId;
	}

	public PredicateMap getPredicates() {
		return predicates;
	}

	public void setPredicates(PredicateMap predicates) {
		this.predicates = predicates;
	}

	public void setParents(ParentsList parents2) {
		parents = parents2;
	}

	public PathFormula getInitAbstractionSet() {
		return initAbstractionFormula;
	}

	public void setInitAbstractionSet(PathFormula initFormula) {
		this.initAbstractionFormula = initFormula;
	}

	public SymbPredAbsAbstractElement getArtParent() {
		return this.artParent;
	}

	public void setArtParent(SymbPredAbsAbstractElement artParent) {
		this.artParent = artParent;
	}

	public void updateMaxIndex(SSAMap ssa) {
		assert(maxIndex != null);
		for (String var : ssa.allVariables()) {
			int i = ssa.getIndex(var);
			int i2 = maxIndex.getIndex(var);
			maxIndex.setIndex(var, Math.max(i, i2));
		}
	}

	public SSAMap getMaxIndex() {
		return maxIndex;
	}

	public void setMaxIndex(SSAMap maxIndex) {
		this.maxIndex = maxIndex;
	}

}
