/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package LoopAcc;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.sosy_lab.cpachecker.cfa.CFA;

public class CFileToAST {

  CFileReader cfr;
  IASTTranslationUnit iast;

  public CFileToAST(CFA cfa) {
    cfr = new CFileReader(cfa);

    try {
      iast = getIASTTranslationUnit(cfr.getContentCharArray());
    } catch (Exception e) {
      // TODO Auto-generated catch block
    }

  }

  public static IASTTranslationUnit getIASTTranslationUnit(char[] code) throws Exception {
    FileContent fc = FileContent.create("", code);
    Map<String, String> macroDefinitions = new HashMap<>();
    String[] includeSearchPaths = new String[0];
    IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
    IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
    IIndex idx = null;
    int options = ILanguage.OPTION_IS_SOURCE_UNIT;
    IParserLogService log = new DefaultLogService();
    return GPPLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp, idx, options, log);
  }

  public IASTTranslationUnit getAST() {
    return iast;
  }

}
