/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import java.util.Optional;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;

class SimpleNameResolverImpl implements SimpleNameResolver {

  @Override
  public JSIdExpression resolve(final JavaScriptCFABuilder pBuilder, final SimpleName pSimpleName) {
    // undefined is writable in ES3, but not writable in ES5, see:
    // https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/undefined#Description
    // The used parser of Eclipse JSDT 3.9 does only support ES3.
    // Thereby, it creates a SimpleName for undefined instead of an UndefinedLiteral.
    // We like to be conformant to ES5.
    // That's why we convert it to an JSUndefinedLiteralExpression if it is used as an Expression.
    // However, it can not be resolved to an JSIdExpression.
    assert !pSimpleName.getIdentifier().equals("undefined")
        : "Can not resolve undefined"; // unsupported use of undefined
    final Optional<? extends JSSimpleDeclaration> declaration =
        pBuilder.getScope().findDeclaration(pSimpleName.getIdentifier());
    return new JSIdExpression(
        pBuilder.getFileLocation(pSimpleName),
        declaration.isPresent() ? declaration.get().getName() : pSimpleName.getIdentifier(),
        declaration.orElse(null));
  }

}
