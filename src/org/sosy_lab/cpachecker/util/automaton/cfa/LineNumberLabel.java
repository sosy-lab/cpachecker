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
/**
 *
 */
package org.sosy_lab.cpachecker.util.automaton.cfa;

//import org.eclipse.cdt.core.dom.ast.IASTFileLocation;

import org.sosy_lab.cpachecker.util.automaton.Label;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
//import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;

/**
 * @author holzera
 *
 */
public class LineNumberLabel implements Label<CFAEdge> {

  // TODO: Use mFileName for matching in Nodes
  private String mFileName;
	private int mLineNumber;

	public LineNumberLabel(String pFileName, int pLineNumber) {
	  assert(pFileName != null);

	  mFileName = pFileName;
		mLineNumber = pLineNumber;
	}

	/*private boolean matches(IASTFileLocation pFileLocation) {
	  if (!mFileName.equals(pFileLocation.getFileName())) {
	    return false;
	  }

	  return (pFileLocation.getStartingLineNumber() <= mLineNumber && mLineNumber <= pFileLocation.getEndingLineNumber());
	}*/

	@Override
	public boolean matches(CFAEdge pEdge) {
		CFANode successor = pEdge.getSuccessor();
		CFANode predecessor = pEdge.getPredecessor();

		//TODO: Matching line number in edge label
		/*switch (pEdge.getEdgeType()) {
		case BlankEdge: {
		  BlankEdge mBlankEdge = (BlankEdge)pEdge;

		  //mBlankEdge.

		  AssumeEdge assumeEdge = (AssumeEdge)pEdge;

		  assumeEdge.getExpression().

		  assumeEdge.getExpression().getFileLocation().getStartingLineNumber()
		}
		}*/

		return (mLineNumber == predecessor.getLineNumber() || mLineNumber == successor.getLineNumber());

		//return (mLineNumber == successor.getLineNumber() && mLineNumber != predecessor.getLineNumber());
	}

	@Override
	public boolean equals(Object pObject) {
	  if (pObject == null) {
	    return false;
	  }

	  if (!(pObject instanceof LineNumberLabel)) {
	    return false;
	  }

	  LineNumberLabel lLabel = (LineNumberLabel)pObject;

	  return (mFileName.equals(lLabel.mFileName) && mLineNumber == lLabel.mLineNumber);
	}

	@Override
	public int hashCode() {
	  return mFileName.hashCode() + mLineNumber;
	}

	@Override
	public String toString() {
	  return "PC = (" + mFileName + "@LINE " + mLineNumber + ")";
	}

}
