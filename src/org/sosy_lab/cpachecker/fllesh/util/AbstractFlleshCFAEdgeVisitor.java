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
package org.sosy_lab.cpachecker.fllesh.util;

import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa.InternalSelfLoop;

public abstract class AbstractFlleshCFAEdgeVisitor<T> extends AbstractCFAEdgeVisitor<T> implements FlleshCFAEdgeVisitor<T> {

  @Override
  public T visit(CFAEdge pEdge) {
    if (pEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)) {
      if (pEdge instanceof InternalSelfLoop) {
        return visit((InternalSelfLoop)pEdge);
      }

      return visit((BlankEdge)pEdge);
    }

    return super.visit(pEdge);
  }

}
