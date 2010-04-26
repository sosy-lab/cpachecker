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
package org.sosy_lab.cpachecker.cpa.errorlocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAErrorNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ErrorLocationTransferRelation implements TransferRelation {

  private static Set<Integer> messages = new HashSet<Integer>();

  private final AbstractElement errorElement;
  
  public ErrorLocationTransferRelation(AbstractElement errorElement) {
    this.errorElement = errorElement;
  }
  

  public static void addError(String message, CFAEdge edge) {
    int lineNumber = edge.getLineNumber();
    
    if (!messages.contains(lineNumber)) {
      messages.add(lineNumber);
      CPAchecker.logger.log(Level.WARNING, "ERROR: " + message + " in line " + lineNumber + "!");
    }
  }
  
  private AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge, Precision precision)
                                              throws CPATransferException {
    
    CFANode successorNode = cfaEdge.getSuccessor();
    if (successorNode instanceof CFALabelNode) {
      String label = ((CFALabelNode)successorNode).getLabel(); 
      if (label.toLowerCase().startsWith("error")) {
        addError("Reaching error location " + label + " with edge " + cfaEdge.getRawStatement(), cfaEdge);
        return errorElement;
      }
    }
    
    if (successorNode instanceof CFAErrorNode) {
      addError("Reaching error node with edge " + cfaEdge.getRawStatement(), cfaEdge);
      return errorElement;
    }
    
    if (cfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      IASTExpression expression = ((StatementEdge)cfaEdge).getExpression();
      if (expression instanceof IASTFunctionCallExpression) {
        IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
        
        String functionName = funcExpression.getFunctionNameExpression().getRawSignature();
        if (functionName.equals("__assert_fail")) {

          addError("Hit assertion " + expression.getRawSignature(), cfaEdge);
          return errorElement;
        }
      }
    }
    
    return element;
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
                      AbstractElement element, Precision precision, CFAEdge cfaEdge)
                      throws CPATransferException {
    return Collections.singleton(getAbstractSuccessor(element, cfaEdge, precision));
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                                    List<AbstractElement> otherElements,
                                    CFAEdge cfaEdge, Precision precision)
                                    throws CPATransferException {
    return null;
  }

}