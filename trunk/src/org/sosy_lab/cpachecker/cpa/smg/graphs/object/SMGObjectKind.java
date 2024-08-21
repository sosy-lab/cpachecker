// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

public enum SMGObjectKind {
  REG("Region"),
  DLL("DoublyLinkedList"),
  SLL("SingleyLinkedList"),
  NULL("NullObject"),
  GENERIC("GenericAbstraction"),
  OPTIONAL("OptionalObject");

  private final String name;

  SMGObjectKind(String pName) {
    name = pName;
  }

  @Override
  public String toString() {
    return name;
  }

  /** returns whether the current shape can always be included in the other shape. */
  // TODO looks strange, perhaps rename method to match intended behavior.
  public boolean isContainedIn(SMGObjectKind other) {
    switch (this) {
      case OPTIONAL:
        switch (other) {
          case SLL:
          case DLL:
          case OPTIONAL:
            return false;
          default:
            return true;
        }
      case SLL:
      case DLL:
        return other != OPTIONAL;
      default:
        return true;
    }
  }
}
