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
package org.sosy_lab.cpachecker.cpa.seplogic;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.seplogic.interfaces.Handle;
import org.sosy_lab.cpachecker.cpa.seplogic.interfaces.PartingstarInterface;

import com.google.common.collect.ImmutableSet;

public class SeplogicState implements AbstractState, Cloneable, Targetable {
  private Handle heap;
  private boolean breakFlag;
  private Deque<String> namespaces = new ArrayDeque<>();
  private Exception causeForError = null;

  static class ExceptionBasedProperty implements Property {

    private final Exception cause;

    public ExceptionBasedProperty(Exception pE) {
      this.cause = pE;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((cause == null) ? 0 : cause.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ExceptionBasedProperty other = (ExceptionBasedProperty) obj;
      if (cause == null) {
        if (other.cause != null) {
          return false;
        }
      } else if (!cause.equals(other.cause)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return cause.toString();
    }

  }

  public SeplogicState(Handle pHeap, Handle pMissing, Deque<String> namespaces) {
    super();
    heap = pHeap;
    this.namespaces = namespaces;
  }

  public SeplogicState(Handle pHeap) {
    super();
    heap = pHeap;
  }

  public SeplogicState(Handle pHeap, Deque<String> namespaces) {
    super();
    heap = pHeap;
    this.namespaces = namespaces;
  }

  public SeplogicState(Handle pHeap, Deque<String> pNamespaces, Exception pE) {
    super();
    heap = pHeap;
    this.namespaces = pNamespaces;
    causeForError = pE;
    breakFlag = true;
  }

  private static int freshVarIndex = 0;

  public Handle getHeap() {
    return heap;
  }

  public static class SeplogicQueryUnsuccessful extends RuntimeException {

    private static final long serialVersionUID = 8201481945509804089L;
    private Boolean isPureGuard = null;

    public Boolean isPureGuard() {
      return isPureGuard;
    }

    public void setIsPureGuard(boolean pg) {
      isPureGuard = pg;
    }

    public SeplogicQueryUnsuccessful(String pString) {
      super(pString);
    }}


  public boolean entails(SeplogicState pOtherElement) {
    PartingstarInterface psInt = PartingstarInterface.getInstance();
    return psInt.entails(heap, pOtherElement.heap);
  }

  public SeplogicState abstract_() {
    PartingstarInterface psInt = PartingstarInterface.getInstance();
    List<Handle> handles = psInt.abstract_(heap);
    if (handles.size() != 1) {
      throw new RuntimeException("Internal error, not exactly one abstraction result");
    }
    return new SeplogicState(handles.get(0), namespaces);
  }

  public SeplogicState performSpecificationAssignment(Handle pPre, Handle pPost, String ident) throws SeplogicQueryUnsuccessful {
    PartingstarInterface psInt = PartingstarInterface.getInstance();
    return new SeplogicState(psInt.specAss(heap, pPre, pPost, ident), namespaces);
  }

  public Long extractExplicitValue(String varName) {
    PartingstarInterface psInt = PartingstarInterface.getInstance();
    return psInt.extractExplicitValue(heap, varName);
  }

  @Override
  public String toString() {
    PartingstarInterface psInt = PartingstarInterface.getInstance();
    return psInt.repr(heap);
  }

  public SeplogicState freshenVariable(String pLeftVarName) {
    PartingstarInterface psInt = PartingstarInterface.getInstance();
    return new SeplogicState(psInt.renameIdent(heap, pLeftVarName, makeFreshVar()), namespaces);
  }

  private String makeFreshVar() {
    return "_FRESH_" + freshVarIndex++;
  }

  public boolean doBreak() {
    return breakFlag;
  }

  public boolean isFalse() {
    PartingstarInterface psInt = PartingstarInterface.getInstance();
    return causeForError != null || psInt.entails(heap, psInt.makeFalse());
  }

  public String getNamespace() {
    String namespace = namespaces.peek();
    if (namespace == null) {
      throw new NullPointerException();
    }
    return namespace;
  }

  public SeplogicState pushNamespace(String pString) {
    Deque<String> newNamespaces = new ArrayDeque<>(namespaces);
    newNamespaces.push(pString);
    return new SeplogicState(heap, newNamespaces);
  }

  public SeplogicState popNamespace() {
    Deque<String> newNamespaces = new ArrayDeque<>(namespaces);
    newNamespaces.pop();
    return new SeplogicState(heap, newNamespaces);
  }

  public Iterable<String> getNamespaces() {
    return namespaces;
  }

  public SeplogicState makeExceptionState(Exception e) {
    PartingstarInterface psInt = PartingstarInterface.getInstance();
    return new SeplogicState(psInt.makeEmp(), namespaces, e);
  }

  @Override
  public boolean isTarget() {
    return causeForError != null;
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    return ImmutableSet.<Property>of(new ExceptionBasedProperty(causeForError));
  }
}
