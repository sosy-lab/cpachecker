/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblemHolder;

/**
 * Handles problems during CFA generation
 */
public class CFAGenerationRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 6850681425709171716L;

  public CFAGenerationRuntimeException(String msg) {
    super(msg);
  }


  public CFAGenerationRuntimeException(String msg, IASTNode astNode) {
    this(astNode == null ? msg :
         (msg + " in line " + astNode.getFileLocation().getStartingLineNumber()
             + ": " + astNode.getRawSignature()));
  }


  public CFAGenerationRuntimeException(String msg, org.sosy_lab.cpachecker.cfa.ast.IASTNode astNode) {
    this(astNode == null ? msg :
      (msg + " in line " + astNode.getFileLocation().getStartingLineNumber()
          + ": " + astNode.toASTString()));
  }

  public <P extends IASTProblemHolder & IASTNode> CFAGenerationRuntimeException(P problem) {
    this(problem.getProblem().getMessage()
         + " in line " + problem.getFileLocation().getStartingLineNumber()
         + ": " + problem.getRawSignature());
  }
}
