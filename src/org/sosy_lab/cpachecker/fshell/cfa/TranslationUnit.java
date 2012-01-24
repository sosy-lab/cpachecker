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
package org.sosy_lab.cpachecker.fshell.cfa;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.CFATopologicalSort;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import com.google.common.collect.Lists;

class TranslationUnit {

  private final Map<String, CFAFunctionDefinitionNode> mCFAs = new HashMap<String, CFAFunctionDefinitionNode>();
  private final List<Pair<IASTDeclaration, String>> mGlobalDeclarations = Lists.newLinkedList();

  public TranslationUnit() {
  }

  private TranslationUnit(Map<String, CFAFunctionDefinitionNode> pCFAs, List<Pair<IASTDeclaration, String>> pGlobalDeclarations) {
    assert pCFAs != null;

    mCFAs.putAll(pCFAs);
    mGlobalDeclarations.addAll(pGlobalDeclarations);
  }

  public void add(TranslationUnit pTranslationUnit) {
    add(pTranslationUnit.mCFAs);

    // TODO check for conflicting declarations
    mGlobalDeclarations.addAll(pTranslationUnit.mGlobalDeclarations);
  }

  public void add(Map<String, CFAFunctionDefinitionNode> pCFAs) {
    if (pCFAs == null) {
      throw new IllegalArgumentException();
    }

    for (String lFunctionName : pCFAs.keySet()) {
      if (mCFAs.containsKey(lFunctionName)) {
        throw new IllegalArgumentException("Function " + lFunctionName + " is already defined!");
      }
    }

    mCFAs.putAll(pCFAs);
  }

  public CFAFunctionDefinitionNode getFunction(String pFunctionName) {
    if (!mCFAs.containsKey(pFunctionName)) {
      throw new IllegalArgumentException("Function " + pFunctionName + " not present!");
    }

    return mCFAs.get(pFunctionName);
  }

  public Iterable<String> functionNames() {
    return mCFAs.keySet();
  }

  public List<Pair<IASTDeclaration, String>> getGlobalDeclarations() {
    return mGlobalDeclarations;
  }

  public static TranslationUnit parseString(String pSource, LogManager pLogManager) {
    ParseResult c;
    try {
      CParser parser = CParser.Factory.getParser(pLogManager, CParser.Factory.getDefaultOptions());
      c = parser.parseString(pSource);
    } catch (ParserException e) {
      throw new RuntimeException("Error during parsing C code \""
          + pSource + "\": " + e.getMessage());
    }

    Map<String, CFAFunctionDefinitionNode> cfas = c.getFunctions();

    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : cfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }

    TranslationUnit lTranslationUnit = new TranslationUnit(cfas, c.getGlobalDeclarations());

    return lTranslationUnit;
  }

  public void insertCallEdgesRecursively(String pEntryFunction) {
    CFASecondPassBuilder lBuilder = new CFASecondPassBuilder(mCFAs);
    try {
      lBuilder.insertCallEdgesRecursively();
    } catch (ParserException e) {
      throw new RuntimeException("Error during parsing C code \""
          + pEntryFunction + "\": " + e.getMessage());
    }
  }

  public void toDot(String pFunction, File pFile) throws IOException {
    CFAFunctionDefinitionNode lEntry = getFunction(pFunction);
    Files.writeFile(pFile, DOTBuilder.generateDOT(mCFAs.values(), lEntry));
  }

}
