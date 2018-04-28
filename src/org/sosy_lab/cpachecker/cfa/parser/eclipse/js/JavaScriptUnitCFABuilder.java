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

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Statement;

@SuppressWarnings("ResultOfMethodCallIgnored")
class JavaScriptUnitCFABuilder implements JavaScriptUnitAppendable {

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final JavaScriptUnit pUnit) {
    for (final ASTNode node : pUnit.statements()) {
      if (node instanceof Statement) {
        pBuilder.append((Statement) node);
      } else if (node instanceof FunctionDeclaration) {
        pBuilder.append((FunctionDeclaration) node);
      } else {
        throw new CFAGenerationRuntimeException(
            "Unknown kind of node (not handled yet): " + node.getClass().getSimpleName(), node);
      }
    }
  }
}
