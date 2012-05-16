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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

public class TemplateUIF extends TemplateFormula {

  private String name = null;
  private TemplateSumList args = null;
  private Integer index = null; // For use with ternary makeUIF method in FormulaManager
  private TemplateTerm purifiedName = null;
  private TemplateTerm oldPurifiedName = null;

//------------------------------------------------------------------
// Constructors

  public TemplateUIF(String pName, TemplateSumList pArgs) {
    name = pName;
    args = pArgs;
  }

  public TemplateUIF(String pName, TemplateSumList pArgs, int pIdx) {
    name = pName;
    args = pArgs;
    index = new Integer(pIdx);
  }

  //----------------------------------------------------------------
  // copy

  @Override
  public TemplateUIF copy() {
    /*
    TemplateUIF u = null;
    if (index == null) {
      u = new TemplateUIF(name,args.copy());
    } else {
      u = new TemplateUIF(name,args.copy(),index.intValue());
    }
    */

    TemplateUIF u = new TemplateUIF( new String(name), args.copy() );
    if (index != null) {
      u.index = new Integer(index);
    }
    if (oldPurifiedName != null) {
      u.oldPurifiedName = oldPurifiedName.copy();
    }
    if (purifiedName != null) {
      u.purifiedName = purifiedName.copy();
    }
    return u;
  }

//------------------------------------------------------------------
// Alter and Undo

  @Override
  public void alias(AliasingMap amap) {
    if (args!=null) {
      args.alias(amap);
    }
  }

  @Override
  public void unalias() {
    if (args!=null) {
      args.unalias();
    }
  }

  @Override
  public boolean evaluate(Map<String,Rational> map) {
    boolean ans = true;
    if (args!=null) {
      ans &= args.evaluate(map);
    }
    return ans;
  }

  @Override
  public void unevaluate() {
    if (args!=null) {
      args.unevaluate();
    }
  }

  @Override
  public void postindex(Map<String,Integer> indices) {
    if (args!=null) {
      args.postindex(indices);
    }
  }

  @Override
  public void preindex(Map<String,Integer> indices) {
    if (args!=null) {
      args.preindex(indices);
    }
  }

  @Override
  public void unindex() {
    if (args!=null) {
      args.unindex();
    }
  }

  @Override
  public Purification purify(Purification pur) {
    if (args!=null) {
      // When each sum in args is purified, it will also normalize itself.
      pur = args.purify(pur);
    }
    pur.purify(this);
    return pur;
  }

  @Override
  public void unpurify() {
    if (args!=null) {
      args.unpurify();
    }
    oldPurifiedName = purifiedName;
    purifiedName = null;
  }

  public void repurify() {
    purifiedName = oldPurifiedName;
  }

  public void generalize() {
    if (args != null) {
      args.generalize();
    }
  }


//------------------------------------------------------------------
// Other cascade methods

  @Override
  public Set<String> getAllVariables(VariableWriteMode vwm) {
    HashSet<String> vars = new HashSet<String>();
    if (args != null) {
      vars.addAll(args.getAllVariables(vwm));
    }
    return vars;
  }

  @Override
  public Set<TemplateVariable> getAllParameters() {
    HashSet<TemplateVariable> params = new HashSet<TemplateVariable>();
    if (args != null) {
      params.addAll(args.getAllParameters());
    }
    return params;
  }

  @Override
  public HashMap<String,Integer> getMaxIndices(HashMap<String,Integer> map) {
    if (args!=null) {
      map = args.getMaxIndices(map);
    }
    return map;
  }

  @Override
  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager tvm = new TemplateVariableManager();
    if (args!=null) {
      tvm = args.getVariableManager();
    }
    return tvm;
  }

  public void prefixVariables(String prefix) {
    if (args!=null) {
      args.prefixVariables(prefix);
    }
  }

  @Override
  public Formula translate(FormulaManager fmgr) {
  	Formula form = null;
  	FormulaList fl = args.translate(fmgr);
  	if (hasIndex()) {
  		form = fmgr.makeUIF(name, fl, index.intValue());
  	} else {
  		form = fmgr.makeUIF(name, fl);
  	}
  	return form;
  }

//------------------------------------------------------------------
// Other

  public boolean isPurified() {
    return (purifiedName != null);
  }

  public String getName() {
    return name;
  }

  public boolean hasIndex() {
  	return (index != null);
  }

  public Integer getIndex() {
    return index;
  }

  void setPurifiedName(TemplateTerm A) {
    unpurify();
    purifiedName = A;
  }

  public TemplateTerm getPurifiedName() {
    return purifiedName;
  }

  public TemplateSumList getArgs() {
    return args;
  }

  public int getArity() {
    return args.size();
  }

  void writeAsForm(boolean b) {
    if (args != null) {
      args.writeAsForm(b);
    }
  }

  @Override
  public String toString() {
    return toString(VariableWriteMode.PLAIN);
  }

  @Override
  public String toString(VariableWriteMode vwm) {
    String s = null;
    if (isPurified()) {
      s = purifiedName.toString(vwm);
    } else {
      s = name + "(";
      if (args!=null) {
        s += args.toString(",");
      }
      s += ")";
    }
    return s;
  }

}
