/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CParser.Dialect;
import org.sosy_lab.cpachecker.cfa.CParser.FileContentToParse;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.AbstractEclipseCParser.StubScannerInfo;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CParserException;

/**
 * Wrapper for Eclipse CDT 7.0 and 8.0 (internal version number 5.2.* and 5.3.*)
 */
public class EclipseCDT7Parser extends AbstractEclipseCParser<FileContent> {

  public EclipseCDT7Parser(Configuration config, LogManager pLogger,
      Dialect dialect, MachineModel pMachine) {
    super(config, pLogger, dialect, pMachine);
  }

  @Override
  protected FileContent wrapCode(FileContentToParse pContent) {
    return FileContent.create(pContent.getFileName(), pContent.getFileContent().toCharArray());
  }

  @Override
  protected FileContent wrapCode(String pFileName, String pCode) {
    return FileContent.create(pFileName, pCode.toCharArray());
  }

  @Override
  protected IASTTranslationUnit getASTTranslationUnit(FileContent pCode) throws CParserException, CFAGenerationRuntimeException, CoreException {
    return language.getASTTranslationUnit(pCode,
                                          StubScannerInfo.instance,
                                          IncludeFileContentProvider.getEmptyFilesProvider(),
                                          null,
                                          PARSER_OPTIONS,
                                          parserLog);
  }
}
