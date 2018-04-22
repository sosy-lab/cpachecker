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

import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;

/**
 * Registry that associates a {@link VariableDeclarationFragment} with a {@link
 * JSVariableDeclaration} as conversion result during CFA generation.
 */
interface VariableDeclarationRegistry {
  /**
   * Register {@code pJSVariableDeclaration} as conversion result of {@code
   * pVariableDeclarationFragment}.
   *
   * @param pVariableDeclarationFragment Node that has been converted to {@code
   *     pJSVariableDeclaration}.
   * @param pJSVariableDeclaration Conversion result of {@code pVariableDeclarationFragment}.
   */
  void add(
      final VariableDeclarationFragment pVariableDeclarationFragment,
      final JSVariableDeclaration pJSVariableDeclaration);

  /**
   * Get the registered conversion result of {@code pVariableDeclarationFragment}.
   *
   * @param pVariableDeclarationFragment Key that has been associated to a conversion result using
   *     {@link VariableDeclarationRegistry#add(VariableDeclarationFragment,
   *     JSVariableDeclaration)}.
   * @return Conversion result of {@code pVariableDeclarationFragment} that has been registered.
   */
  JSVariableDeclaration get(final VariableDeclarationFragment pVariableDeclarationFragment);
}
