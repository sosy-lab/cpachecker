/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien.objects.sll;

import org.sosy_lab.cpachecker.cpa.cpalien.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.cpalien.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.cpalien.objects.SMGObjectVisitor;
import org.sosy_lab.cpachecker.cpa.cpalien.objects.SMGRegion;


public final class SMGSingleLinkedList extends SMGObject implements SMGAbstractObject {
  private int length;

  //TODO: Binding is likely to be more complicated later
  private int bindingOffset;

  SMGSingleLinkedList(SMGRegion pPrototype, int pOffset, int pLength) {
    super(pPrototype.getSize(), "SLL");
    bindingOffset = pOffset;
    length = pLength;
  }

  //TODO: Abstract interface???
  public int getLength() {
    return length;
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  public int getOffset() {
    return bindingOffset;
  }

  @Override
  public String toString() {
    return "SLL(size=" + getSize() + ", bindingOffset=" + bindingOffset + ", len=" + length +")";
  }

  @Override
  public void accept(SMGObjectVisitor visitor) {
    visitor.visit(this);
  }
}
