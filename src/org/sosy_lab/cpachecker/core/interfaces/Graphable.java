// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

/** Interface which specifies that the state can be dumped to the [part of] the graphviz label. */
public interface Graphable {

  /**
   * Return a string representation of this object that is suitable to be printed inside a label of
   * a node in a DOT graph.
   *
   * @return A non-null but possibly empty string.
   */
  String toDOTLabel();

  /**
   * Return whether this object is somehow special as opposed to other objects of the same type, and
   * should be highlighted in the output.
   */
  boolean shouldBeHighlighted();
}
