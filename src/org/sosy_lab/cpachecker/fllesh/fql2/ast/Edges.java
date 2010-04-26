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
package org.sosy_lab.cpachecker.fllesh.fql2.ast;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter.Filter;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.ASTVisitor;

public class Edges implements Atom {

  private Filter mFilter;
  
  public Edges(Filter pFilter) {
    mFilter = pFilter;
  }
  
  public Filter getFilter() {
    return mFilter;
  }
  
  @Override
  public String toString() {
    return "EDGES(" + mFilter.toString() + ")";
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <T> T accept(org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
}
