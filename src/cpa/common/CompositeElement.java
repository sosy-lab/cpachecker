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
package cpa.common;

import java.util.List;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;

public class CompositeElement implements AbstractElementWithLocation
{
    private AbstractElementWithLocation elementWithLocation;
    private final List<AbstractElement> elements;
    private CallStack callStack;

    public CompositeElement (List<AbstractElement> elements, CallStack stack)
    {
        this.elementWithLocation = null;
        if (!elements.isEmpty()) {
          assert (elements.get(0) instanceof AbstractElementWithLocation);
          this.elementWithLocation = (AbstractElementWithLocation) elements.get(0);
        }
        this.elements = elements;
        this.callStack = stack;
    }

    public List<AbstractElement> getElements ()
    {
        return elements;
    }

    public int getNumberofElements(){
    	return elements.size();
    }

    @Override
    public boolean equals (Object other)
    {
        if (other == this)
            return true;

        if (!(other instanceof CompositeElement))
            return false;

        CompositeElement otherComposite = (CompositeElement) other;
        List<AbstractElement> otherElements = otherComposite.elements;


        if (otherElements.size () != this.elements.size ())
            return false;

        for (int idx = 0; idx < elements.size (); idx++)
        {
            AbstractElement element1 = otherElements.get (idx);
            AbstractElement element2 = this.elements.get (idx);

            if (!element1.equals (element2))
                return false;
        }

        if(!otherComposite.getCallStack().equals(this.getCallStack())){
        	return false;
        }

        return true;
    }
    
    @Override
    public int hashCode() {
      int hashCode = 0;
      
      for (AbstractElement lElement : elements) {
        if (lElement == null) {
          continue;
        }
        
        hashCode += lElement.hashCode();
      }
      
      return hashCode;
    }

    @Override
    public String toString ()
    {
        StringBuilder builder = new StringBuilder ();
        builder.append ('(');
        for (AbstractElement element : elements)
            builder.append (element.toString ()).append (',');
        builder.replace (builder.length () - 1, builder.length (), ")");

        return builder.toString ();
    }

	public AbstractElement get(int idx) {
		return elements.get(idx);
	}

	public CallStack getCallStack() {
		return callStack;
	}

	public void setCallStack(CallStack callStack) {
		this.callStack = callStack;
	}

  public CFANode getLocationNode() {
    return elementWithLocation.getLocationNode();
  }

  public AbstractElementWithLocation getElementWithLocation () {
    return elementWithLocation;
  }
}
