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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.util.invariants.interfaces.GeneralVariable;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class TemplateVariable extends TemplateNumeralValue implements BooleanFormula, GeneralVariable {

  private String name = null;
  private Integer index = null;
  private String realName = null;
  private Integer realIndex = null;

  private boolean writingAsForm = false;

  public TemplateVariable(FormulaType<?> type, String name, int i) {
    super(type);
    index = Integer.valueOf(i);
    build(name, index);
  }

  public TemplateVariable(FormulaType<?> type, String name) {
    super(type);
    build(name, null);
  }

  public TemplateVariable(FormulaType<?> type, String name, Integer index) {
    super(type);
    build(name, index);
  }

  private void build(String name, Integer index) {
    this.name = name;
    this.index = index;
  }

  /**
   * Parse a String representing a variable in plain form, i.e. in
   * the form: name@index. Return the variable so constructed.
   * @param vn
   * @return
   */
  public static TemplateVariable parse(FormulaType<?> type, String vn) {
    TemplateVariable V;
    int i = vn.lastIndexOf("@");
    if (i < 0) {
      V = new TemplateVariable(type, vn);
    } else {
      String s = vn.substring(0, i);
      String ind = vn.substring(i+1);
      Integer I = Integer.valueOf(ind);
      int j = I.intValue();
      V = new TemplateVariable(type, s, j);
    }
    return V;
  }

  @Override
  public void alias(AliasingMap amap) {
    amap.alias(this);
  }

  @Override
  public void unalias() {
    name = realName;
    index = realIndex;
  }

  void setAlias(String n, Integer i) {
    realName = name;
    realIndex = index;
    name = n;
    index = i;
  }

  @Override
  public void postindex(Map<String, Integer> indices) {
    if (name!=null && indices.containsKey(name)) {
      index = indices.get(name);
    } else {
      index = null;
    }
  }

  @Override
  public void preindex(Map<String, Integer> indices) {
    if (name!=null && indices.containsKey(name)) {
      index = Integer.valueOf(1);
    } else {
      index = null;
    }
  }

  @Override
  public void unindex() {
    index = null;
  }

  public void addPrefix(String prefix) {
    name = prefix+name;
  }

  @Override
  public HashMap<String, Integer> getMaxIndices(HashMap<String, Integer> map) {
    if (name!=null && index!=null) {
      if (map.containsKey(name)) {
        Integer J = map.get(name);
        if (index.compareTo(J) > 0) {
          map.put(name, index);
        }
      } else {
        map.put(name, index);
      }
    }
    return map;
  }

  public boolean equals(TemplateVariable v) {
    // Call these variables equal if they produce the same
    // string.
    return ( toString().equals(v.toString() ));
  }

  public String getName() {
    return name;
  }

  public Integer getIndex() {
    return index;
  }

  public boolean hasIndex() {
    return (index != null);
  }

  @Override
  public Formula translate(FormulaManagerView fmgr) {
    Formula form = null;
    if (hasIndex()) {
      form = fmgr.makeVariable(getFormulaType(), name, index);
    } else {
      form = fmgr.makeVariable(getFormulaType(), name);
    }
    return form;
  }

  @Override
  public TemplateVariable copy() {
    return withFormulaType(getFormulaType());
  }

  @Override
  public TemplateVariable withFormulaType(FormulaType<?> pNewType) {

    TemplateVariable v = null;
    if (index == null) {
      v = new TemplateVariable(pNewType, name);
    } else {
      v = new TemplateVariable(pNewType, name, Integer.valueOf(index));
    }
    return v;
  }

  public void generalize() {
    index = null;
  }

  void writeAsForm(boolean b) {
    writingAsForm = b;
  }

  /**
   * If we have an alias, return that.
   * Otherwise, return the PLAIN mode toString.
   */
  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    String s = "";
    switch (vwm) {
    case REDLOG:
      s = name;
      if (index!=null && !writingAsForm) {
        // Make redlog-style index.
        //s = "mkid("+s+","+I.toString()+")";
        s = s+index.toString();
      }
      break;
    case PLAIN:
      // Same as default.
    default:
      // If no special write mode is specified, then we just
      // write the name and affix the index with an @ symbol.
      s = name;
      if (index!=null && !writingAsForm) {
        s += "@"+index.toString();
      }
    }
    return s;
  }


}
