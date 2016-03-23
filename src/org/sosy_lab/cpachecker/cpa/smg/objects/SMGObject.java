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
package org.sosy_lab.cpachecker.cpa.smg.objects;



public abstract class SMGObject {
  private final int size;
  private final String label;
  private final int level;


  static private final SMGObject nullObject = new SMGObject(0, "NULL") {
    @Override
    public String toString() {
      return "NULL";
    }

    @Override
    public SMGObject copy() {
      // fancy way of referencing itself
      return SMGObject.getNullObject();
    }
  };

  static public SMGObject getNullObject() {
    return nullObject;
  }

  protected SMGObject(int pSize, String pLabel) {
    size = pSize;
    label = pLabel;
    level = 0;
  }

  protected SMGObject(int pSize, String pLabel, int pLevel) {
    size = pSize;
    label = pLabel;
    level = pLevel;
  }

  protected SMGObject(SMGObject pOther) {
    size = pOther.size;
    label = pOther.label;
    level = pOther.getLevel();
  }

  public abstract SMGObject copy();

  public String getLabel() {
    return label;
  }

  public int getSize() {
    return size;
  }

  public boolean notNull() {
    return (! equals(nullObject));
  }

  public boolean isAbstract() {
    if (equals(nullObject)) {
      return false;
    }

    throw new UnsupportedOperationException("isAbstract() called on SMGObject instance, not on a subclass");
  }

  /**
   * @param visitor the visitor to accept
   */
  public void accept(SMGObjectVisitor visitor) {
    throw new UnsupportedOperationException("accept() called on SMGObject instance not on a subclass");
  }

  public boolean isMoreGeneral(SMGObject pOther) {
    if (size != pOther.size) {
      throw new IllegalArgumentException("isMoreGeneral called on incompatible pair of objects");
    }
    return false;
  }

  /**
   * @param pOther object to join with
   * @param pIncreaseLevel increase Nesting level.
   */
  public SMGObject join(SMGObject pOther, boolean pIncreaseLevel) {
    throw new UnsupportedOperationException("join() called on SMGObject instance, not on a subclass");
  }

  public int getLevel() {
    return level;
  }
}
