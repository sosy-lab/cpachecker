// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.loopAbstraction;

public class NotKnownLoopEdgeTypeException extends Exception {

  private static final long serialVersionUID = -6518645691571199738L;

  public NotKnownLoopEdgeTypeException() {
    super("This type of edge is not known to the loopabstractionframework right now.");
  }
}
