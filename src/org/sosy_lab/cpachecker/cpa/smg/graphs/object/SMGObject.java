// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/**
 * SMGs consists of two types of nodes: {@link SMGObject}s and {@link SMGValue}s. {@link SMGObject}s
 * are further derived into {@link SMGRegion}s and abstractions like {@link SMGDoublyLinkedList}.
 * There is a special {@link SMGNullObject} representing the target of NULL.
 */
public abstract class SMGObject implements Comparable<SMGObject> {

  private final long size;
  private final String label;
  private final int level;
  private final SMGObjectKind kind;
  private static int count;
  private final int id;

  public SMGObjectKind getKind() {
    return kind;
  }

  protected SMGObject(long pSize, String pLabel, SMGObjectKind pKind) {
    this(pSize, pLabel, 0, pKind);
  }

  protected SMGObject(long pSize, String pLabel, int pLevel, SMGObjectKind pKind) {
    this(pSize, pLabel, pLevel, pKind, getNewId());
  }

  protected SMGObject(SMGObject pOther) {
    this(pOther.size, pOther.label, pOther.level, pOther.kind, pOther.id);
  }

  private SMGObject(long pSize, String pLabel, int pLevel, SMGObjectKind pKind, int pId) {
    size = pSize;
    label = pLabel;
    level = pLevel;
    kind = pKind;
    id = pId;
  }

  public abstract SMGObject copy(int pNewLevel);

  public String getLabel() {
    return label;
  }

  public long getSize() {
    return size;
  }

  public abstract boolean isAbstract();

  public abstract <T> T accept(SMGObjectVisitor<T> visitor);

  /**
   * Compares objects and determines, if this object is more general than given object. If this
   * object is more general than the given object, then a smg resulting in replacing the given
   * object with this object would cover strictly more states.
   *
   * @param pOther other object to be compared with this object.
   * @return Returns true iff this object is more general than given object. False otherwise.
   */
  public abstract boolean isMoreGeneral(SMGObject pOther);

  /**
   * Compute the result of a join.
   *
   * @param pOther object to join with
   * @param pDestLevel increase Nesting level.
   */
  public abstract SMGObject join(SMGObject pOther, int pDestLevel);

  public int getLevel() {
    return level;
  }

  private static int getNewId() {
    count++;
    return count;
  }

  public int getId() {
    return id;
  }

  @Override
  public int compareTo(SMGObject o) {
    return Integer.compare(getId(), o.getId());
  }
}
