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

import java.util.ArrayList;
import java.util.List;

public class DOTNodeShapeWriter {

	private List<ShapePair> shapedNodes;

	public DOTNodeShapeWriter() {
		shapedNodes = new ArrayList<ShapePair>();
	}

	public void add(int no, String shape){
		ShapePair sp = new ShapePair(no, shape);
		if(!shapedNodes.contains(sp)){
			shapedNodes.add(sp);
		}
	}

	public String getDot(){
		String s = "";
		for(ShapePair sp:shapedNodes){
			s = s + "node [shape = " + sp.shape + "]; " + sp.nodeNumber + ";\n";
		}
		return s;
	}

	public class ShapePair {
		public int nodeNumber;
		String shape;

		private ShapePair(int nodeNo, String shape){
			nodeNumber = nodeNo;
			this.shape = shape;
		}
	}
}
