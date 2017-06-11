/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import java.util.logging.Level;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

class ASTConverter {

  private final Scope scope;
  private final LogManager logger;

  ASTConverter(final Scope pScope, final LogManager pLogger) {
    scope = pScope;
    logger = pLogger;
  }

  /**
   * Takes a ASTNode, and tries to get Information of its Placement in the Source Code. If it
   * doesnt't find such information, returns an empty FileLocation Object.
   *
   * @param pNode A Code piece wrapped in an ASTNode
   * @return FileLocation with Placement Information of the Code Piece, or null if such Information
   *     could not be obtained.
   */
  public FileLocation getFileLocation(final ASTNode pNode) {
    if (pNode == null) {
      return FileLocation.DUMMY;
    } else if (pNode.getRoot().getNodeType() != ASTNode.JAVASCRIPT_UNIT) {
      logger.log(Level.WARNING, "Can't find Placement Information for :" + pNode.toString());
      return FileLocation.DUMMY;
    }

    final JavaScriptUnit javaScriptUnit = (JavaScriptUnit) pNode.getRoot();

    return new FileLocation(
        scope.getFileName(),
        pNode.getStartPosition(),
        pNode.getLength(),
        javaScriptUnit.getLineNumber(pNode.getStartPosition()),
        javaScriptUnit.getLineNumber(pNode.getLength() + pNode.getStartPosition()));
  }
}
