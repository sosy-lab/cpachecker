/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

/** The {@link SMGNullObject} represents the target of NULL. */
public final class SMGNullObject extends SMGObject {

  public static final SMGNullObject INSTANCE = new SMGNullObject();

  private SMGNullObject() {
    super(0, "NULL", SMGObjectKind.NULL);
  }

  @Override
  public String toString() {
    return "NULL";
  }

  @Override
  public SMGObject copy(int level) {
    return this;
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    /*There is no object that can replace the null object in an smg.*/
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public <T> T accept(SMGObjectVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SMGObject join(SMGObject pOther, int pDestLevel) {
    throw new UnsupportedOperationException("NULL does not join"); // TODO why not?
  }
}