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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/**
 * SMGs consists of two types of nodes: {@link SMGObject}s and {@link SMGValue}s. {@link SMGObject}s
 * are further derived into {@link SMGRegion}s and abstractions like {@link SMGDoublyLinkedList}.
 * There is a special {@link SMGNullObject} representing the target of NULL.
 */
public abstract class SMGObject implements Comparable<SMGObject> {

  private final int size;
  private final String label;
  private final int level;
  private final SMGObjectKind kind;
  private static int count;
  private final int id;

  public SMGObjectKind getKind() {
    return kind;
  }

  protected SMGObject(int pSize, String pLabel, SMGObjectKind pKind) {
    this(pSize, pLabel, 0, pKind);
  }

  protected SMGObject(int pSize, String pLabel, int pLevel, SMGObjectKind pKind) {
    this(pSize, pLabel, pLevel, pKind, getNewId());
  }

  protected SMGObject(SMGObject pOther) {
    this(pOther.size, pOther.label, pOther.level, pOther.kind, pOther.id);
  }

  private SMGObject(int pSize, String pLabel, int pLevel, SMGObjectKind pKind, int pId) {
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

  public int getSize() {
    return size;
  }

  public abstract boolean isAbstract();

  public abstract <T> T accept(SMGObjectVisitor<T> visitor);

  /**
   * Compares objects and determines, if this object is more general than given object.
   * If this object is more general than the given object, then a smg resulting in replacing
   * the given object with this object would cover strictly more states.
   *
   * @param pOther other object to be compared with this object.
   * @return Returns true iff this object is more general than given object.
   *  False otherwise.
   */
  public abstract boolean isMoreGeneral(SMGObject pOther);

  /**
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
