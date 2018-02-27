/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.model;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class BlankEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = 6394933292868202442L;

  private final String description;

  public BlankEdge(String pRawStatement, FileLocation pFileLocation,  CFANode pPredecessor,
      CFANode pSuccessor, String pDescription) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    description = pDescription;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getCode() {
    return "";
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    if (!super.equals(pO)) {
      return false;
    }
    BlankEdge blankEdge = (BlankEdge) pO;
    return Objects.equals(description, blankEdge.description);
  }

  @Override
  public int hashCode() {
    // Add the 31 as magic number to the hash,
    // to circumvent hashes that are equal with other edge types that also include
    // a description String in their hash
    return Objects.hash(super.hashCode(), description) + 31;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.BlankEdge;
  }
}
