/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.objectmodel.c;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

public class MultiDeclarationEdge extends AbstractCFAEdge {
  
  private final List<IASTSimpleDeclaration> declarations;
  private final List<String> rawStatements;

  public MultiDeclarationEdge (String rawStatement, int lineNumber, CFANode predecessor, CFANode successor,
                            List<IASTSimpleDeclaration> declarations,
                            List<String> rawStatements) {
    super(rawStatement, lineNumber, predecessor, successor);
    this.declarations = Preconditions.checkNotNull(declarations);
    this.rawStatements = Preconditions.checkNotNull(rawStatements);
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.MultiDeclarationEdge;
  }

  public List<IASTSimpleDeclaration> getDeclarators() {
    return declarations;
  }

  public List<String> getRawStatements() {
    return rawStatements;
  }

  @Override
  public String getRawStatement() {
    return Joiner.on('\n').join(rawStatements);
  }
}
