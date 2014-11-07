/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.interfaces;

/**
 * Interface which specifies that the state can be dumped to the
 * [part of] the graphviz label.
 */
public interface Graphable {

  /**
   * Return a string representation of this object
   * that is suitable to be printed inside a label of a node in a DOT graph.
   * @return A non-null but possibly empty string.
   */
  public String toDOTLabel();

  /**
   * Return whether this object is somehow special as opposed
   * to other objects of the same type,
   * and should be highlighted in the output.
   */
  public boolean shouldBeHighlighted();
}
