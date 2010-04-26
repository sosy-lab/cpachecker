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
package org.sosy_lab.cpachecker.util.octagon;

import java.util.Arrays;


public class Octagon {

	private int dimension;	/* number of variables, aka dimension */
	private int ref;		/* reference counting */
	private int state;		/* empty 0, normal 1, closed 2*/
	private Octagon closed;  	/* pointer to the closed version, or NULL */
	private Num[] matrix;

	public Octagon(int dimension, int ref, int state, Octagon closed, Num[] matrix) {
		this.dimension = dimension;
		this.ref = ref;
		this.state = state;
		this.closed = closed;
		this.matrix = matrix;
	}

	public int getDimension() {
		return dimension;
	}

	public int getRef() {
		return ref;
	}

	public int getState() {
		return state;
	}

	public Octagon getClosed() {
		return closed;
	}

	public Num[] getMatrix() {
		return matrix;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Octagon clone(){
		Num[] mat = new Num[this.matrix.length];
		for(int i=0; i<mat.length; i++){
			mat[i] = new Num(this.matrix[i].f);
		}
		return new Octagon(this.dimension, this.ref, this.state, this.closed, mat);
	}

	public String getContents(){
		String s = "";

		s = s + "Dimension: " + this.dimension + " Ref. Counting: "
		+ this.ref + " State: " + this.state + "\n";

		if(matrix != null){
			for(int i=0; i<this.matrix.length; i++){
				s = s + this.matrix[i] + " ";
			}
		}
		else s = s + "Array is NULL ";

		if(closed != null){
			s = s + " CLOSED is NOT NULL ";
		}
		else s = s + " // closed is NULL ";

		return s;
	}

	public int matSize(){
		return 2 * dimension * (dimension+1);
	}

	public int matPos(int i, int j){
		return (j+((i+1)*(i+1))/2);
	}

	public int matPos2(int i, int j){
		return ((i)<(j)?matPos((j^1),(i^1)):matPos(i,j));
	}
	
	@Override
	public int hashCode() {
    return Arrays.hashCode(matrix);
	}
	
	@Override
	public boolean equals(Object pObj) {
	  Octagon otherOct = (Octagon) pObj;
	  if(this == pObj){
	    return true;
	  }
	  
	  return this.hashCode() == otherOct.hashCode();
	  
	}
}
