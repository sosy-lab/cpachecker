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

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IPDOMASTProcessor.Abstract;
import org.sosy_lab.cpachecker.cfa.CFA;;

public class LoopAST extends Abstract {

  private CFA cfa;
  private IASTTranslationUnit trans;

  public LoopAST(CFA cfa) {
    this.cfa = cfa;
    trans = new CFileToAST(cfa).getAST();
    loopstructure();
  }

  private void loopstructure() {
    for (IASTNode x : trans.getChildren()) {
    }
  }

}
