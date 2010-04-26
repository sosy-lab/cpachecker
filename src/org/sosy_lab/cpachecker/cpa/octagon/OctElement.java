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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.util.octagon.LibraryAccess;
import org.sosy_lab.cpachecker.util.octagon.OctWrapper;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

/**
 * An element of octagon abstract domain. This element contains an {@link Octagon} which
 * is the concrete representation of the octagon and a map which
 * provides a mapping from variable names to variables.
 * see {@link Variable}.
 *
 * @author erkan
 *
 */
public class OctElement implements AbstractElement{

	private Octagon oct;
	// mapping from variable name to its identifier
	private HashMap<String, Integer> variables;
	private long elemId;
  private static long nextAvailableId = 0;
  private boolean isBottom = false;
	
	/**
	 * Class constructor creating a new octagon and an empty variables list.
	 */
	public OctElement(){
		oct = LibraryAccess.universe(0);
		this.variables = new HashMap<String, Integer>();
		elemId = ++nextAvailableId;
	}

	/**
	 * Class constructor with an existing octagon and map to variables.
	 * @param oct Octagon.
	 * @param variables Variables map.
	 */
	public OctElement(Octagon oct, HashMap<String, Integer> variables){
		this.oct = oct;
		this.variables = variables;
		elemId = ++nextAvailableId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public OctElement clone(){
		HashMap<String, Integer> newMap = new HashMap<String, Integer>();
		Set<String> keys = variables.keySet();
		for(String key:keys){
			Integer id = variables.get(key);
			newMap.put(key, id);
		}
		return new OctElement(oct.clone(), newMap);
	}

	/**
	 * Update the element with a new octagon element. This method is accessed frequently by {@link OctTransferRelation}
	 * to update the current region.
	 * @param ent
	 */
	public void update(OctElement ent){
		this.oct = ent.oct;
		this.variables = ent.variables;
	}
	
	@Override
	public boolean isError() {
	  return false;
	}
	
	@Override
	public int hashCode() {
	  if (oct == null) return (variables == null) ? 0 : variables.hashCode() + 1;
    else if (variables == null) return oct.hashCode() + 2;
    else return oct.hashCode() * 17 + variables.hashCode();
	}

	@Override
	public boolean equals (Object other)
	{
	  OctElement otherOctagon = (OctElement) other;

	  if(this.elemId == otherOctagon.elemId){
	    return true;
	  }
	  
		if (this == other){
			return true;
		}
		
		if(otherOctagon.hashCode() == this.hashCode()){
		  return true;
		}

		// we check for equality on native library because it optimizes octagons first and
		// check for equality
		boolean isEq = LibraryAccess.isEqual(this, otherOctagon);
		
		return isEq;
	}

	public String getVarNameForId(int i){
		for(String varName:variables.keySet()){
			if(variables.get(varName) == i){
				return varName;
			}
		}
		assert(false);
		return null;
	}

	@Override
	public String toString() {

		String s = "";

		if (oct.getState() == 0){
			s = s + "[ empty ] \n";
			return s;
		}

		if (oct.getState() == 2){
			s = s + " [ closed ]";
		}

		for (int i=0; i<oct.getDimension(); i++) {
			String varName = getVarNameForId(i);
			if (oct.getMatrix()[oct.matPos(2*i,2*i)].f > 0) {
				s = s + "\n  " + varName + "-" + varName + " <= " + oct.getMatrix()[oct.matPos(2*i, 2*i)].f;
			}
			if (oct.getMatrix()[oct.matPos(2*i+1,2*i+1)].f > 0) {
				s = s + "\n  " + "-"+ varName + "+" + varName + " <= " + oct.getMatrix()[oct.matPos(2*i+1,2*i+1)].f;
			}
			if ((oct.getMatrix()[oct.matPos(2*i+1,2*i)].f != Double.NEGATIVE_INFINITY) &&
					(oct.getMatrix()[oct.matPos(2*i+1,2*i)].f != Double.POSITIVE_INFINITY)) {
				s = s + "\n  " + varName + " <= " + (oct.getMatrix()[oct.matPos(2*i+1,2*i)].f)/2;
			}
			if ((oct.getMatrix()[oct.matPos(2*i,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
					(oct.getMatrix()[oct.matPos(2*i,2*i+1)].f != Double.POSITIVE_INFINITY)) {
				s = s + "\n  " + "-" + varName + " <= " + (oct.getMatrix()[oct.matPos(2*i,2*i+1)].f)/2;
			}
		}

		for (int i=0; i<oct.getDimension(); i++){
			for (int j=i+1; j<oct.getDimension(); j++) {
				String iVarName = getVarNameForId(i);
				String jVarName = getVarNameForId(j);

				if((oct.getMatrix()[oct.matPos(2*j,2*i)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j,2*i)].f != Double.POSITIVE_INFINITY)){
					s = s + "\n  " + iVarName + "-" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j,2*i)].f);
				}
				// 2*j,2*i+1
				if ((oct.getMatrix()[oct.matPos(2*j,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j,2*i+1)].f != Double.POSITIVE_INFINITY)) {
					s = s + "\n  " + "-" + iVarName + "-" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j,2*i+1)].f);
				}
				// 2*j+1,2*i
				if ((oct.getMatrix()[oct.matPos(2*j+1,2*i)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j+1,2*i)].f != Double.POSITIVE_INFINITY)) {
					s = s + "\n  " + iVarName + "+" + jVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j+1,2*i)]);
				}
				// 2*j+1,2*i+1
				if ((oct.getMatrix()[oct.matPos(2*j+1,2*i+1)].f != Double.NEGATIVE_INFINITY) &&
						(oct.getMatrix()[oct.matPos(2*j+1,2*i+1)].f != Double.POSITIVE_INFINITY)){
					s = s + "\n  " + jVarName + "-" + iVarName +" <= " + (oct.getMatrix()[oct.matPos(2*j+1,2*i+1)]);
				}
			}
		}
		s = s + "\n";
		return s;

	}

	/**
	 * Adds a new variable in form of functionName::variableName. For example if a
	 * new variable a is declared in function main(), main::a is added to the
	 * list of variables. After this operation you have to increase octagon
	 * dimension manually!
	 * @param varName Name of the variable.
	 * @param funcName Name of the function that contains the variable
	 * @return true if variable is added succesfully
	 */
	public boolean addVar(String varName, String funcName) {
		String variableName = funcName + "::" + varName;
		// add if the variable is not already in the variables set
		if(!variables.containsKey(variableName)){
			variables.put(variableName, getNumberOfVars());
			return true;
		}
		return false;
	}

	/**
	 * Remove a variable from the list by its variable id.
	 * @param varId Variable id.
	 */
	public void removeVar(int varId, int numOfDims) {
		// TODO
		OctWrapper ow = new OctWrapper();
		oct = ow.J_removeDimensionAtPosition(oct, varId, numOfDims, true);
		variables.remove(getVarNameForId(varId));
	}

	/**
	 * Retrieve the variable's id by its name.
	 * @param globalVars List of global variables
	 * @param varName Name of the variable.
	 * @param functionName Name of the function.
	 * @return id of the variable
	 */
	public int getVariableId(List<String> globalVars, String varName, String functionName){
		String variableName = "";
		if(globalVars.contains(varName)){
			variableName = "::" + varName;
		}
		else{
			variableName = functionName + "::" + varName;
		}
		return variables.get(variableName);
	}

	/**
	 * Total number of variables.
	 * @return
	 */
	public int getNumberOfVars() {
//		assert(variables.size() == oct.getDimension());
		return variables.size();
	}

	/** Is octagon empty?
	 * @return true if octagon is empty, false o.w.
	 */
	public boolean isEmpty(){
		return LibraryAccess.isEmpty(this);
	}

	/**
	 * Retrieves the octagon of this element.
	 * @return the octagon.
	 */
	public Octagon getOctagon(){
		return oct;
	}

	/**
	 * Retrieve the map of variables.
	 * @return map of variables.
	 */
	public HashMap<String, Integer> getVariableMap(){
		return variables;
	}

	/**
	 * Asks if the variable list contains a variable.
	 * @param varName Name of the variable.
	 * @return true if the list contains variable, false o.w.
	 */
	public boolean contains(String varName) {
		return variables.containsKey(varName);
	}

	public void changeVarId(String varName, int id){
		variables.remove(varName);
		variables.put(varName, id);
	}

	public void changeVarName(String formerName, String newName){
		//sdf
	}

	public void setVariableMap(HashMap<String, Integer> newVariablesMap) {
		variables = newVariablesMap;

	}

  public boolean isBottom() {
    return isBottom;
  }

  public void setBottom() {
    isBottom = true;
  }

	// TODO fix this
//	public void addVariablesFrom(OctElement octEl1) {
//	VariableMap oct1Map = octEl1.getVarMap();
//	for(int i=0; i<oct1Map.getSize(); i++){
//	String s = oct1Map.getVarName(i);
//	if(!this.variables.contains(s)){
//	this.update(LibraryAccess.addDimension(this, 1));
//	this.addVar(s);
//	}
//	}
//	}

}
