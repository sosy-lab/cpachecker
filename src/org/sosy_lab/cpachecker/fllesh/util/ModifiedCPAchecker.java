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
package org.sosy_lab.cpachecker.fllesh.util;

import java.util.Map;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.CPAchecker;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;


// TODO: where is the right place to collect statistics?
public class ModifiedCPAchecker extends CPAchecker {
  private Map<String, CFAFunctionDefinitionNode> mCFAMap;
  private CFAFunctionDefinitionNode mMainFunction;

  public ModifiedCPAchecker(Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    super(pConfiguration, pLogManager);

    // get code file name
    String[] names = pConfiguration.getPropertiesArray("analysis.programNames");
    if (names.length != 1) {
      getLogger().log(Level.SEVERE, "Exactly one code file has to be given!");

      throw new RuntimeException();
    }

    // parse code file
    IASTTranslationUnit lAst = null;
    try {
      lAst = super.parse(names[0]);
    } catch (Exception e) {
      e.printStackTrace();
      
      throw new RuntimeException(e);
    }

    CFACreator lCFACreator = new CFACreator(getConfiguration(), getLogger());
    lCFACreator.createCFA(lAst);

    mCFAMap = lCFACreator.getFunctions();
    mMainFunction = lCFACreator.getMainFunction();
  }

  public Map<String, CFAFunctionDefinitionNode> getCFAMap() {
    return mCFAMap;
  }

  public CFAFunctionDefinitionNode getMainFunction() {
    return mMainFunction;
  }

}
