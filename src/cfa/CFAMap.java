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
package cfa;

import java.util.Collection;
import java.util.HashMap;

import cfa.objectmodel.CFAFunctionDefinitionNode;


/**
 * A map to save all CFAs of the program. Provides operations to retrieve and insert
 * new CFAs to the map
 * @author erkan
 *
 */
public class CFAMap {

	private HashMap<String, CFAFunctionDefinitionNode> cfaMap;

	/**
	 * Class constructor. Creates a new {@link HashMap} to map the name of the function
	 * to the first node of the function.
	 */
	public CFAMap (){
		cfaMap = new HashMap<String, CFAFunctionDefinitionNode>();
	}

	/**
	 * Add a new CFA to the map.
	 * @param name name of the function.
	 * @param initNode the first node of the function. All nodes in the CFA of a function
	 * is accessible through its first node.
	 */
	public void addCFA (String name, CFAFunctionDefinitionNode initNode){
		cfaMap.put(name, initNode);
	}

	/**
	 * Retrieves the function's definition (the first) node.
	 * @param name name of the function.
	 * @return the first node of the function.
	 */
	public CFAFunctionDefinitionNode getCFA (String name){
		return cfaMap.get(name);
	}

	/**
	 * @return number of CFAs.
	 */
	public int size (){
		return cfaMap.size();
	}

	/**
	 * @return a collection of function nodes on the map.
	 */
	public Collection <CFAFunctionDefinitionNode> cfaMapIterator (){
		return cfaMap.values();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		String s = "";
		Collection <CFAFunctionDefinitionNode> cfas = cfaMapIterator ();
		for(CFAFunctionDefinitionNode cfa:cfas){
			s = s + cfa.getFunctionName() + "\n";
		}
		return s;
	}
}
