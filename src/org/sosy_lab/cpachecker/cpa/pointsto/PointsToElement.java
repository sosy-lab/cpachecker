/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
/**
 *
 */
package org.sosy_lab.cpachecker.cpa.pointsto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IBinding;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.pointsto.PointsToRelation.Address;
import org.sosy_lab.cpachecker.cpa.pointsto.PointsToRelation.InvalidPointer;
import org.sosy_lab.cpachecker.cpa.pointsto.PointsToRelation.NullPointer;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToElement implements AbstractElement {

  public static class InMemoryObject {
    protected final IASTDeclarator variable;
    protected final String name;

    public InMemoryObject (IASTDeclarator variable, String name) {
      this.variable = variable;
      this.name = name;
    }

    @Override
    public int hashCode () {
      return variable.hashCode() + name.hashCode();
    }

    @Override
    public boolean equals (Object o) {
      if (!(o instanceof InMemoryObject)) {
        return false;
      }
      InMemoryObject other = (InMemoryObject)o;

      if (other.variable != variable || !other.name.equals(name)) {
        return false;
      }

      return true;
    }

    @Override
    public InMemoryObject clone () {
      return new InMemoryObject(variable, name);
    }

    public IASTDeclarator getVariable () {
      return variable;
    }

    public String getName () {
      return name;
    }
  }

  // TODO implement ArrayOfPointers extends InMemoryObject (Vector<PointsToRelation>)

  private final HashMap<IASTDeclarator,Pair<PointsToRelation,Set<PointsToRelation>>> pointers;
  private final HashMap<Address,InMemoryObject> memoryMap;

  public PointsToElement () {
    pointers = new HashMap<IASTDeclarator,Pair<PointsToRelation,Set<PointsToRelation>>>();
    memoryMap = new HashMap<Address,InMemoryObject>();
  }

  @Override
  public PointsToElement clone () {
    // TODO I think deep cloning should be ok, all equals methods are overridden
    PointsToElement result = new PointsToElement();
    for (Pair<PointsToRelation,Set<PointsToRelation>> ptr : pointers.values()) {
      Set<PointsToRelation> s = new HashSet<PointsToRelation>();
      for (PointsToRelation r : ptr.getSecond()) {
        s.add(r.clone());
      }
      result.pointers.put(ptr.getFirst().getVariable(),
          new Pair<PointsToRelation,Set<PointsToRelation>>(ptr.getFirst().clone(), s));
    }
    for (Address key : memoryMap.keySet()) {
      result.memoryMap.put(key.clone(), memoryMap.get(key).clone());
    }
    return result;
  }

  @Override
  public String toString () {
    String out = "{";
    Iterator<Pair<PointsToRelation,Set<PointsToRelation>>> iter = pointers.values().iterator();
    while (iter.hasNext()) {
      Pair<PointsToRelation,Set<PointsToRelation>> entry = iter.next();
      out += "(" + entry.getFirst().toString();
      if (!entry.getSecond().isEmpty()) out += ", ";
      Iterator<PointsToRelation> iter2 = entry.getSecond().iterator();
      while (iter2.hasNext()) {
        out += iter2.next().toString();
        if (iter2.hasNext()) out += ", ";
      }
      if (iter.hasNext()) out += ", ";
    }
    out += "}";
    return out;
  }

  @Override
  public boolean isError() {
    return false;
  }

  public PointsToRelation addVariable (IASTDeclarator variable) {
    if (null == pointers.get(variable)) {
      IBinding binding = variable.getName().resolveBinding();
      /*for (int i = derefCount; i > 0; --i) {
          out += "*";
        }*/
      // out += variable.getParent().getRawSignature() + " -> ";
      PointsToRelation entry = new PointsToRelation(variable, binding.getName());
      pointers.put(variable, new Pair<PointsToRelation,Set<PointsToRelation>>(entry,
          new HashSet<PointsToRelation>()));
      return entry;
    } else {
      return pointers.get(variable).getFirst();
    }
  }

  public PointsToRelation addPointer (PointsToRelation pointer) {
    addVariable(pointer.getVariable());
    for (PointsToRelation entry : pointers.get(pointer.getVariable()).getSecond()) {
      if (entry.equals(pointer)) return entry;
    }
    pointers.get(pointer.getVariable()).getSecond().add(pointer);
    return pointer;
  }

  public PointsToRelation lookup (IASTName name) {
    IBinding binding = name.resolveBinding();
    for (IASTDeclarator decl : pointers.keySet()) {
      if (decl.getNestedDeclarator() != null &&
          decl.getNestedDeclarator().getName().resolveBinding() == binding)
        return pointers.get(decl).getFirst();
      if (decl.getName().resolveBinding() == binding)
        return pointers.get(decl).getFirst();
    }

    return null;
  }

  public InMemoryObject deref (Address address) {
    return memoryMap.get(address);
  }

  public Set<Address> addressOf (InMemoryObject obj) {
    Set<Address> result = new HashSet<Address>();
    for (Entry<Address,InMemoryObject> ref : memoryMap.entrySet()) {
      if (ref.getValue().equals(obj)) result.add(ref.getKey());
    }
    if (result.isEmpty()) result.add(new InvalidPointer());
    return result;
  }

  public void writeToMem (Address address, InMemoryObject entry) {
    assert (!(address instanceof NullPointer));
    assert (!(address instanceof InvalidPointer));
    memoryMap.put(address, entry);
  }

  public void join (final PointsToElement other) {
    for (Pair<PointsToRelation,Set<PointsToRelation>> p : other.pointers.values()) {
      addVariable(p.getFirst().getVariable()).join(p.getFirst());
      for (PointsToRelation ptr : p.getSecond()) {
        for (PointsToRelation ptr2 : pointers.get(p.getFirst().getVariable()).getSecond()) {
          assert (ptr.getVariable().equals(ptr2.getVariable()));
          if (ptr2.getName().equals(ptr.getName())) {
            ptr2.join(ptr);
          } else {
            pointers.get(p.getFirst().getVariable()).getSecond().add(ptr);
          }
        }
      }
    }
    memoryMap.putAll(other.memoryMap);
  }

  private boolean containsRecursive (final PointsToRelation pointer) {
    PointsToRelation candidate = pointers.get(pointer.getVariable()).getFirst();
    if (candidate != null) {
      if (pointer.subsetOf(candidate)) return true;
      for (PointsToRelation p : pointers.get(pointer.getVariable()).getSecond()) {
        if (pointer.subsetOf(p)) return true;
      }
    }
    return false;
  }

  public boolean subsetOf(PointsToElement other) {
    for (Pair<PointsToRelation,Set<PointsToRelation>> p : pointers.values()) {
      if (!other.containsRecursive(p.getFirst())) return false;
      for (PointsToRelation ptr : p.getSecond()) {
        if (!other.containsRecursive(ptr)) return false;
      }
    }
    return true;
  }
}
