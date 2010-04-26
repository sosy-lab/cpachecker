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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class ErrorLocationCPA extends AbstractCPA {

  private static class ErrorLocationCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      return new ErrorLocationCPA();
    }
  }
  
  public static CPAFactory factory() {
    return new ErrorLocationCPAFactory();
  }
  
  private static enum ErrorLocationElement implements AbstractElement {
    
    NORMAL(false),
    ERROR(true),
    TOP(true),
    BOTTOM(false);
    
    private final boolean isError;
    
    private ErrorLocationElement(boolean isError) {
      this.isError = isError;
    }
    
    @Override
    public String toString() {
      return "<" + super.toString() + ">";
    }
    
    @Override
    public boolean isError() {
      return isError;
    }
  }

  private static final TransferRelation transferRelation = new ErrorLocationTransferRelation(ErrorLocationElement.ERROR);
  
  private ErrorLocationCPA() {
    super("sep", "sep", transferRelation);
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    return ErrorLocationElement.NORMAL;
  }
}