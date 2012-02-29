/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.seplogic.csif.CorestarInterface;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Empty;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Formula;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SeplogicNode;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SeplogicNode.IntegerEqualityArgumentExtractingNodeVisitor;

public class SeplogicElement implements AbstractElement, Cloneable, Targetable {
  private Formula heap, missing;
  private boolean breakFlag;
  private Deque<String> namespaces = new ArrayDeque<String>();
  private Exception causeForError = null;

  public SeplogicElement(Formula pHeap, Formula pMissing, Deque<String> namespaces) {
    super();
    heap = pHeap;
    missing = pMissing;
    this.namespaces = namespaces;
  }

  public SeplogicElement(Formula pHeap) {
    super();
    heap = pHeap;
    missing = new Empty();
  }

  public SeplogicElement(Formula pHeap, Deque<String> namespaces) {
    super();
    heap = pHeap;
    missing = new Empty();
    this.namespaces = namespaces;
  }

  public SeplogicElement(Formula pHeap, Formula pMissing, Deque<String> pNamespaces, Exception pE) {
    super();
    heap = pHeap;
    missing = pMissing;
    this.namespaces = pNamespaces;
    causeForError = pE;
    breakFlag = true;
  }

  private static int freshVarIndex = 0;

  public Formula getMissing() {
    return missing;
  }

  public Formula getHeap() {
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


  public boolean entails(SeplogicElement pOtherElement) {
    CorestarInterface csInt = CorestarInterface.getInstance();
    return csInt.entails(heap.getRepr(), pOtherElement.heap.getRepr());
  }

  public SeplogicElement abstract_() {
    CorestarInterface csInt = CorestarInterface.getInstance();
    List<String> strings = csInt.abstract_(heap.getRepr());
    if (strings.size() != 1)
      throw new RuntimeException("Internal error, not exactly one abstraction result");
    return new SeplogicElement(csInt.parse(strings.get(0)), missing, namespaces);
  }

  public SeplogicElement performSpecificationAssignment(Formula pPre, Formula pPost, String ident) throws SeplogicQueryUnsuccessful {
    CorestarInterface csInt = CorestarInterface.getInstance();

    /*
    Formula frameFormula, antiFrameFormula;
    if (true) {
      List<String> strings = csInt.biabduct(heap.getRepr(), pPre.getRepr());
      if (strings.size() != 1) {
        throw new SeplogicQueryUnsuccessful("Unhandled - not exactly one biabduction result but " + strings.size());
      } else {
        String result, frame, antiFrame;
        result = strings.get(0);
        if (result.equals("")) {
          frameFormula = antiFrameFormula = new Empty();
        } else {
          int divPos = result.indexOf(" | "); // XXX disallow in strings!
          frame = result.substring(0, divPos - 1);
          antiFrame = result.substring(divPos + 4);
          frameFormula = csInt.parse(frame);
          antiFrameFormula = csInt.parse(antiFrame);
        }
      }

      Formula newPost = pPost;
      if (ident != null) {
        newPost = (Formula) pPost.accept(new SeplogicNode.RenamingNodeVisitor(Formula.RETVAR, ident));
      }
      return new SeplogicElement(new SeparatingConjunction(frameFormula, newPost), new SeparatingConjunction(missing, antiFrameFormula));
    } else {
      List<String> strings = csInt.frame(heap.getRepr(), pPre.getRepr());
      if (strings.size() != 1) {
        throw new SeplogicQueryUnsuccessful("Unhandled - not exactly one frame result but " + strings.size());
      } else {
        frameFormula = csInt.parse(strings.get(0));
      }

      Formula newPost = pPost;
      if (ident != null) {
        newPost = (Formula) pPost.accept(new SeplogicNode.RenamingNodeVisitor(Formula.RETVAR, ident));
      }
      return new SeplogicElement(new SeparatingConjunction((Formula) frameFormula.accept(new SeplogicNode.RenamingNodeVisitor(ident, makeFreshVar())), newPost));
    }
    */
    List<String> strings = csInt.specAss(pPre.getRepr(), pPost.getRepr(), heap.getRepr(), ident);

    if (strings.size() != 1) {
      throw new SeplogicQueryUnsuccessful("Unhandled - not exactly one spec ass result but " + strings.size());
    }
    String result = strings.get(0);
    int divPos = result.indexOf(" | "); // XXX disallow in strings!
    String frame = result.substring(0, divPos - 1);
    return new SeplogicElement(csInt.parse(frame), namespaces);
  }

  public Long extractExplicitValue(String varName) {
    IntegerEqualityArgumentExtractingNodeVisitor eaenv = new IntegerEqualityArgumentExtractingNodeVisitor(varName);
    heap.accept(eaenv);
    return eaenv.getArg();
  }

  @Override
  public String toString() {
    return heap.toString() + " | " + missing.toString();
  }

  public SeplogicElement freshenVariable(String pLeftVarName) {
    Formula newHeap = (Formula) heap.accept(new SeplogicNode.RenamingNodeVisitor(pLeftVarName, makeFreshVar()));
    return new SeplogicElement(newHeap, namespaces);
  }

  private String makeFreshVar() {
    return "_se_" + freshVarIndex++;
  }

  public boolean doBreak() {
    return breakFlag;
  }

  public boolean isFalse() {
    CorestarInterface csInt = CorestarInterface.getInstance();
    return causeForError != null || csInt.entails(heap.getRepr(), Formula.FALSE.getRepr());
  }

  public String getNamespace() {
    String namespace = namespaces.peek();
    if (namespace == null) {
      throw new NullPointerException();
    }
    return namespace;
  }

  public SeplogicElement pushNamespace(String pString) {
    Deque<String> newNamespaces = new ArrayDeque<String>(namespaces);
    newNamespaces.push(pString);
    return new SeplogicElement(heap, missing, newNamespaces);
  }

  public SeplogicElement popNamespace() {
    Deque<String> newNamespaces = new ArrayDeque<String>(namespaces);
    newNamespaces.pop();
    return new SeplogicElement(heap, missing, newNamespaces);
  }

  public Iterable<String> getNamespaces() {
    return namespaces;
  }

  public SeplogicElement makeExceptionState(Exception e) {
    return new SeplogicElement(new Empty(), new Empty(), namespaces, e);
  }

  @Override
  public boolean isTarget() {
    return causeForError != null;
  }
}
