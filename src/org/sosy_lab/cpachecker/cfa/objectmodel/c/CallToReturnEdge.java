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
package org.sosy_lab.cpachecker.cfa.objectmodel.c;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;

public class CallToReturnEdge extends AbstractCFAEdge {

	private final IASTExpression expression;
	private AbstractElement abstractElement;

	public CallToReturnEdge(String rawStatement, int lineNumber, CFANode predecessor, CFANode successor, IASTExpression exp) {
		super(rawStatement, lineNumber, predecessor, successor);
		this.expression = exp;
	}

	@Override
  public void addToCFA() {
		getPredecessor().addLeavingSummaryEdge(this);
		getSuccessor().addEnteringSummaryEdge(this);
	}

	public IASTExpression getExpression ()
	{
		return expression;
	}

	public CFAEdgeType getEdgeType ()
	{
		return CFAEdgeType.CallToReturnEdge;
	}

	public AbstractElement getAbstractElement() {
		return abstractElement;
	}

	public void setAbstractElement(AbstractElement abstractElement) {
		this.abstractElement = abstractElement;
	}

	public <T extends AbstractElement> T extractAbstractElement(Class<T> pType){
		AbstractWrapperElement wrappedElem = (AbstractWrapperElement) abstractElement;
		return wrappedElem.retrieveWrappedElement(pType);
	}
}