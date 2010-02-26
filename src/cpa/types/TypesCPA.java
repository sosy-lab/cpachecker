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
package cpa.types;

import java.util.logging.Level;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cpa.common.LogManager;
import cpa.common.defaults.AbstractCPA;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import exceptions.UnrecognizedCCodeException;

/**
 * @author Philipp Wendler
 */
public class TypesCPA extends AbstractCPA {
  
  private static class TypesCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      return new TypesCPA(getLogger());
    }
  }
  
  public static CPAFactory factory() {
    return new TypesCPAFactory();
  }
  
  private LogManager logger;
  
  private TypesCPA(LogManager logger) {
    super("join", "sep", new TypesTransferRelation());
    this.logger = logger;
  }
  
  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    TypesElement element = new TypesElement();
    try {
      IASTFunctionDefinition funcDef = ((FunctionDefinitionNode)pNode).getFunctionDefinition();
      ((TypesTransferRelation)getTransferRelation()).handleFunctionDeclaration(element, null, 
                                          funcDef.getDeclarator(), funcDef.getDeclSpecifier());
    } catch (UnrecognizedCCodeException e) { 
      logger.log(Level.WARNING, "Could not determine return type of entry function, " +
      		                      "not adding entry function to known types");
    }
    return element;
  }
}