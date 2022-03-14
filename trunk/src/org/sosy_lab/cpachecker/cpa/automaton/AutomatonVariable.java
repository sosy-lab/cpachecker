// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Splitter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/** Represents a local variable of the automaton. So far only integer variables are supported. */
public abstract class AutomatonVariable implements Cloneable, Serializable {
  private static final long serialVersionUID = -6765794863680244559L;
  protected final String name;

  private AutomatonVariable(String pName) {
    name = pName;
  }

  public static AutomatonVariable createAutomatonVariable(
      String pType, String pName, String... args) {
    if (pType.toLowerCase().equals("int") || pType.toLowerCase().equals("integer")) {
      return new AutomatonIntVariable(pName);
    } else if (pType.toLowerCase().equals("set")) {
      if (args.length > 0) {
        String elementType = args[0];
        AutomatonSetVariable<?> result;
        if (elementType.toLowerCase().equals("int")) {
          result = new AutomatonSetVariable<Integer>(pName);
        } else if (elementType.toLowerCase().equals("string")) {
          result = new AutomatonSetVariable<String>(pName);
        } else {
          throw new IllegalArgumentException(
              "Element type '" + elementType + "' is not suppoprted for sets");
        }
        if (args.length > 1) {
          String value = args[1];
          if (!value.trim().isEmpty()) {
            for (String elem : Splitter.on(',').split(value)) {
              elem = elem.trim();
              if (elementType.toLowerCase().equals("int")) {
                try {
                  result.add(Integer.valueOf(elem));
                } catch (NumberFormatException e) {
                  throw new IllegalArgumentException(
                      "Value '" + elem + "' cannot be parsed as integer number");
                }
              } else {
                result.add(elem);
              }
            }
          }
        }
        return result;
      } else {
        throw new IllegalArgumentException("Element type was not specified for set variable");
      }
    } else {
      throw new IllegalArgumentException(
          "Type '" + pType + "' is not supported for automata variables");
    }
  }

  public String getName() {
    return name;
  }

  public abstract String getType();

  public abstract int getValue();

  @Override
  public abstract AutomatonVariable clone();

  // We don't use the hashcode, but it should be redefined every time equals is overwritten.
  @Override
  public abstract int hashCode();

  public static final class AutomatonIntVariable extends AutomatonVariable {

    private static final long serialVersionUID = -5599402008148488971L;
    private int value;

    private AutomatonIntVariable(String pName) {
      super(pName);
      value = 0;
    }

    @Override
    public int getValue() {
      return value;
    }

    public void setValue(int v) {
      value = v;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @Override
    public boolean equals(Object pObj) {
      if (super.equals(pObj)) {
        return true;
      }
      if (!(pObj instanceof AutomatonIntVariable)) {
        return false;
      }
      AutomatonIntVariable otherVar = (AutomatonIntVariable) pObj;
      return (value == otherVar.value) && name.equals(otherVar.name);
    }

    @Override
    public int hashCode() {
      return value + name.hashCode();
    }

    @Override
    public AutomatonVariable clone() {
      AutomatonIntVariable result = new AutomatonIntVariable(name);
      result.setValue(getValue());
      return result;
    }

    @Override
    public String getType() {
      return "int";
    }
  }

  public static final class AutomatonSetVariable<T> extends AutomatonVariable {

    private static final long serialVersionUID = 4293998838719160247L;
    private Set<T> set;

    private AutomatonSetVariable(String pName) {
      super(pName);
      set = new HashSet<>();
    }

    private AutomatonSetVariable(String pName, Set<T> pSet) {
      super(pName);
      set = new HashSet<>(pSet);
    }

    public boolean contains(Object obj) {
      return set.contains(obj);
    }

    public boolean isEmpty() {
      return set.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public void add(Object obj) {
      set.add((T) obj);
    }

    public void remove(Object obj) {
      set.remove(obj);
    }

    @Override
    public int getValue() {
      // Return true, if set is not empty and false otherwise
      if (set.isEmpty()) {
        return 0;
      } else {
        return 1;
      }
    }

    @Override
    public String toString() {
      return String.valueOf(set);
    }

    @Override
    public boolean equals(Object pObj) {
      if (super.equals(pObj)) {
        return true;
      }
      if (!(pObj instanceof AutomatonSetVariable<?>)) {
        return false;
      }
      AutomatonSetVariable<?> otherVar = (AutomatonSetVariable<?>) pObj;
      return this.set.equals(otherVar.set) && name.equals(otherVar.name);
    }

    @Override
    public int hashCode() {
      return this.set.hashCode() + name.hashCode();
    }

    @Override
    public AutomatonVariable clone() {
      return new AutomatonSetVariable<>(name, set);
    }

    @Override
    public String getType() {
      return "set";
    }
  }
}
