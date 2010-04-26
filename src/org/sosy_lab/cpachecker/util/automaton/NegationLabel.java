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
/**
 * 
 */
package org.sosy_lab.cpachecker.util.automaton;

/**
 * @author holzera
 *
 */
public class NegationLabel<E> implements Label<E> {
	
	private Label<E> mLabel;
	
	public NegationLabel(Label<E> pLabel) {
		assert(pLabel != null);
	  
		mLabel = pLabel;
	}

	@Override
	public boolean matches(E pE) {
		return !mLabel.matches(pE);
	}
	
	@Override
	public boolean equals(Object pObject) {
	  if (pObject == null) {
	    return false;
	  }
	  
	  if (!(pObject instanceof NegationLabel<?>)) {
	    return false;
	  }
	  
	  NegationLabel<?> lLabel = (NegationLabel<?>)pObject;
	  
	  return mLabel.equals(lLabel);
	}
	
	@Override
	public int hashCode() {
	  return mLabel.hashCode();
	}

	@Override
	public String toString() {
	  return "NOT(" + mLabel + ")";
	}
}
