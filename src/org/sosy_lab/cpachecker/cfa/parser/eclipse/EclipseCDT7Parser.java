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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.exceptions.CParserException;

/**
 * Wrapper for Eclipse CDT 7.0 and 8.0 (internal version number 5.2.* and 5.3.*)
 */
public class EclipseCDT7Parser extends AbstractEclipseCParser<FileContent> {

  public EclipseCDT7Parser(LogManager pLogger, Dialect dialect) {
    super(pLogger, dialect);
  }

  @Override
  protected FileContent wrapCode(String pCode) {
    return FileContent.create("", pCode.toCharArray());
  }

  @Override
  protected FileContent wrapFile(String pFilename) {
    return FileContent.createForExternalFileLocation(pFilename);
  }

  @Override
  protected IASTTranslationUnit getASTTranslationUnit(FileContent pCode) throws CParserException, CFAGenerationRuntimeException, CoreException {
    try {
      return language.getASTTranslationUnit(pCode,
                                            StubScannerInfo.instance,
                                            IncludeFileContentProvider.getSavedFilesProvider(),
                                            null,
                                            PARSER_OPTIONS,
                                            parserLog);
    } catch (NoClassDefFoundError e) {
      if ("org/eclipse/core/runtime/jobs/ISchedulingRule".equals(e.getMessage())) {
        // This error occurs if Eclipse finds an #include and tries to load that file.
        // Unfortunately we have to catch it in this ugly way because we cannot
        // use a stub implementation for the IncludeFileProvider.

        throw new CParserException("#include is not supported");

      } else {
        throw e;
      }
    }
  }
}