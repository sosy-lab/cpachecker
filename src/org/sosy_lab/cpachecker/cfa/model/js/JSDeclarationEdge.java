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
package org.sosy_lab.cpachecker.cfa.model.js;

import com.google.common.base.Optional;
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JSDeclarationEdge extends ADeclarationEdge {

  private static final long serialVersionUID = 6319915232080396018L;

  public static BiFunction<CFANode, CFANode, CFAEdge> of(final JSDeclaration pDeclaration) {
    return (pPredecessor, pSuccessor) ->
        new JSDeclarationEdge(
            pDeclaration.toASTString(),
            pDeclaration.getFileLocation(),
            pPredecessor,
            pSuccessor,
            pDeclaration);
  }

  public JSDeclarationEdge(
      final String pRawSignature,
      final FileLocation pFileLocation,
      final CFANode pPredecessor,
      final CFANode pSuccessor,
      final JSDeclaration pDeclaration) {

    super(pRawSignature, pFileLocation, pPredecessor, pSuccessor, pDeclaration);
  }

  @Override
  public JSDeclaration getDeclaration() {
    return (JSDeclaration) declaration;
  }

  @Override
  public Optional<JSDeclaration> getRawAST() {
    return Optional.of((JSDeclaration) declaration);
  }
}
