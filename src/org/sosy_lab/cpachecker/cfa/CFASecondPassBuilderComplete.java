/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * This class is responsible for creating call edges. 
 * Additionally to super class it creates
 * 1. In case of function pointer call (option functionPointerCalls) 
 *  it creates calls to each potential function matching some criteria (defined by functionPointerCalls)
 * 2. Summary call statement edges (option summaryEdges). 
 *  If functionPointerCalls is on it creates summary edges for each potential regular call
 * @author Vadim Mutilin
 *
 */
@Options
public class CFASecondPassBuilderComplete extends CFASecondPassBuilder {

  @Option(name="analysis.functionPointerCalls",
      description="create all potential function pointer call edges")
  private boolean fptrCallEdges = true;
  
  @Option(name="analysis.summaryEdges",
      description="create summary call statement edges")
  private boolean summaryEdges = true;
  
  private enum FunctionSet {
    ALL, //all defined functions considered (Warning: some CPAs require at least EQ_PARAM_SIZES)
    EQ_PARAM_SIZES //all functions with matching number of parameters considered
  };
  
  private FunctionSet functionSet = FunctionSet.EQ_PARAM_SIZES;
  
  public CFASecondPassBuilderComplete(MutableCFA pCfa, Language pLanguage, Configuration config) throws InvalidConfigurationException {
    super(pCfa, pLanguage);
    config.inject(this);
  }

  protected void buildCallEdges(IAStatement expr, AStatementEdge statement) throws ParserException {
    
  }  
}
