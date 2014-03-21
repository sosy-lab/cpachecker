/*
* CPAchecker is a tool for configurable software verification.
* This file is part of CPAchecker.
*
* Copyright (C) 2007-2014 Dirk Beyer
* All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*
* CPAchecker web page:
* http://cpachecker.sosy-lab.org
*/
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;


public class InterpolationFormulaEnhancer
{
  private BooleanFormulaManagerView bfmgr;
  private FormulaManagerView fmv;
  private FormulaManagerFactory factory;
  private UnsafeFormulaManager ufm;
  private ShutdownNotifier shutdownNotifier;

  public InterpolationFormulaEnhancer(ShutdownNotifier pShutdownNotifier, FormulaManagerView pFmgr,FormulaManagerFactory fmf)
  {
    fmv = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    factory = fmf;
    ufm = fmv.getUnsafeFormulaManager();
    shutdownNotifier = pShutdownNotifier;
  }

  public void enhance(List<BooleanFormula> orderedFormulas)
  {
    List<String> loopVariablePrefixes = getLoopVariablePrefixes();

    List<Integer> abstractionPoints = getAbstractionPoints(orderedFormulas);

    for(int ap : abstractionPoints) {
      if(shutdownNotifier.shouldShutdown()) {
        return;
      }
      List<Pair<Formula,Formula>> replacements = replaceCommonVariables(orderedFormulas,ap);
      BooleanFormula abstraction = getBestLatticeElement(orderedFormulas,replacements,loopVariablePrefixes);
      if(abstraction!=null) { // null means empty subset
        abstraction = bfmgr.and(abstraction, orderedFormulas.get(ap));
        orderedFormulas.set(ap, abstraction);
      }
    }

    // This is a debug message for demonstration purposes. Feel free to delete if you don't need it.
    /*System.out.println("##############");
for(int s=0;s<orderedFormulas.size();s++) {
System.out.println(orderedFormulas.get(s).toString());
}*/
  }

  public List<BooleanFormula> clean(List<BooleanFormula> orderedFormulas)
  {
    List<BooleanFormula> result = new ArrayList<>();
    for(BooleanFormula f : orderedFormulas) {
      result.add(clean(f));
    }
    return result;
  }

  public BooleanFormula clean(BooleanFormula f)
  {
    Set<String> names = fmv.extractVariables(f);

    Map<String,String> replacements = new HashMap<>();

    for(String name : names) {
      if(name.charAt(name.length()-1)=='\'') {
        String repl = name;
        while(name.charAt(name.length()-1)=='\'') {
          name = name.substring(0,name.length()-1);
        }
        replacements.put(repl, name);
      }
    }

    if(replacements.size()==0) {
      return f;
    }

    List<Pair<Formula,Formula>> dummy = new ArrayList<>();
    return ufm.typeFormula(FormulaType.BooleanType, renameRek(f,replacements,dummy));
  }

  private List<String> getLoopVariablePrefixes()
  {
    List<String> result = new ArrayList<>();

    if(!GlobalInfo.getInstance().getCFAInfo().isPresent()) {
      return result;
    }

    VariableClassification vc = GlobalInfo.getInstance().getCFAInfo().get().getVariableClassification();
    Set<String> loopExitCondVars = vc.getLoopExitConditionVariables();
    for(String var : loopExitCondVars) {
      result.add(var);
    }

    return result;
  }

  private BooleanFormula getBestLatticeElement(List<BooleanFormula> orderedFormulas,
                                          List<Pair<Formula,Formula>> variablePairs,
                                          List<String> loopVariablePrefixes)
  {
    List<BooleanFormula> topElements = getBestLatticeElements(orderedFormulas,variablePairs);

    BooleanFormula bestFit = null;
    int cost = Integer.MAX_VALUE;
    for(BooleanFormula f : topElements) {
      int calculatedCost = formulaCost(f,loopVariablePrefixes);
      if(calculatedCost<cost) {
        cost = calculatedCost;
        bestFit = f;
      }
    }

    return bestFit;
  }

  private int formulaCost(BooleanFormula f,List<String> loopVariablePrefixes)
  {
    /* The price for a formula are defined as follows: i=loop variable, x=other variable
* i > i1+-i2 > x > x+-i > x1+-x2
* 5 4 3 2 1
* price = summ of subprices (as defined in "exploring interpolants")
*/
    List<Formula> formulas = new ArrayList<>();
    if(!bfmgr.isAnd(f)) {
      formulas.add(f);
    } else {
      Stack<BooleanFormula> stack = new Stack<>();
      stack.push(f);
      while(!stack.isEmpty()) {
        BooleanFormula formula = stack.pop();
        if(bfmgr.isAnd(formula)) {
          stack.push(ufm.typeFormula(FormulaType.BooleanType,ufm.getArg(formula, 0)));
          stack.push(ufm.typeFormula(FormulaType.BooleanType,ufm.getArg(formula, 1)));
        } else {
          formulas.add(formula);
        }
      }
    }

    int result = 0;
    for(Formula formula : formulas) {
      formula = ufm.getArg(formula, 0);

      if(ufm.getArity(formula)<2) {
        // x=x' type
        if(isLoopVariable(formula,loopVariablePrefixes)) {
          result += 5;
        } else {
          result += 3;
        }
      } else {
        //x+-y type
        List<Formula> param = extractVariables(formula);
        if(isLoopVariable(param.get(1),loopVariablePrefixes)) {
          if(isLoopVariable(param.get(2),loopVariablePrefixes)) {
            result += 4;
          } else {
            result += 2;
          }
        } else {
          if(isLoopVariable(param.get(2),loopVariablePrefixes)) {
            result += 2;
          } else {
            result += 1;
          }
        }
      }
    }

    return result;
  }

  private boolean isLoopVariable(Formula variable, List<String> loopVariablePrefixes)
  {
    String name = ufm.getName(variable).split("@")[0];
    for(String loopVar : loopVariablePrefixes) {
      if(loopVar.equals(name)) {
        return true;
      }
    }
    return false;
  }

  private List<BooleanFormula> getBestLatticeElements(List<BooleanFormula> orderedFormulas, List<Pair<Formula,Formula>> variablePairs)
  {
    List<BooleanFormula> templates = getTemplateFormulas(variablePairs);
    Stack<BitSet> stack = new Stack<>();
    List<BitSet> topElements = new ArrayList<>();
    List<BooleanFormula> result = new ArrayList<>();
    SATTest<?> tester = new SATTest<>(orderedFormulas);

    // first, check if no formula is needed at all
    if(!tester.checkFormula()) {
      return result;
    }

    BitSet all = new BitSet(templates.size());
    //all.set(0,templates.size());
    // the command above does what [...],templates.size()-1); is supposed to do after the api 7 doc.
    // this is fishy... to be sure fill it manually.
    for(int i=0;i<templates.size();i++) {
      all.set(i);
    }
    stack.push(all);

    while(!stack.empty()) {
      if(shutdownNotifier.shouldShutdown()) {
        // return a valid element even on shutdown, to not break anything during the shutdown.
        result = new ArrayList<>();
        result.add(formulaFromSet(templates,all));
        return result;
      }
      BitSet elem = stack.pop();

      // test if a better element exists already
      boolean betterTopExists = false;
      for(BitSet top : topElements) {
        BitSet copy = (BitSet) top.clone();
        copy.andNot(elem);
        if(copy.isEmpty()) {
          betterTopExists = true;
          break;
        }
      }
      if(betterTopExists) {
        continue;
      }

      List<BitSet> subsets = getSubsets(elem);

      // test if element is a single template (returns only one empty BitSet)
      if(subsets.size()==1) {
        topElements.add(elem);
        result.add(formulaFromSet(templates,elem));
        continue;
      }

      // check for acceptable subgroups and add if applicable
      boolean isTop = true;
      for(BitSet subset : subsets) {
        if(!tester.checkFormula(formulaFromSet(templates,subset))) { // must be unsat here
          isTop = false;
          stack.add(subset);
        }
      }

      // if this is a top element => add to top element list
      if(isTop) {
        topElements.add(elem);
        result.add(formulaFromSet(templates,elem));
      }
    }

    return result;
  }

  private List<BitSet> getSubsets(BitSet set)
  {
    List<BitSet> result = new ArrayList<>();

    for(int i=set.nextSetBit(0);i>=0;i=set.nextSetBit(i+1)) {
      BitSet copy = (BitSet) set.clone();
      copy.clear(i);
      result.add(copy);
    }

    return result;
  }

  private BooleanFormula formulaFromSet(List<BooleanFormula> templates, BitSet set)
  {
    if(set.isEmpty())
     {
      return null; // In theory, this will never happen
    }

    int i = set.nextSetBit(0);
    BooleanFormula result = templates.get(i);

    for (i=set.nextSetBit(i+1); i>=0; i=set.nextSetBit(i+1)) {
      result = bfmgr.and(result, templates.get(i));
    }

    return result;
  }

  private List<BooleanFormula> getTemplateFormulas(List<Pair<Formula,Formula>> variablePairs)
  {
    List<BooleanFormula> result = new ArrayList<>();

    // important: "real" variable has to come first, e.g. x=x' is allowed, x'=x is not!

    // equivalences: x=x'
    for(Pair<Formula,Formula> p : variablePairs) {
      result.add(fmv.makeEqual(p.getFirst(), p.getSecond()));
    }

    // differences and combinations: x-y=x'-y' and x+y=x'+y'
    for(int i=0;i<variablePairs.size();i++) {
      for(int k=i+1;k<variablePairs.size();k++) {
        Formula fx1 = variablePairs.get(i).getFirst();
        Formula fy1 = variablePairs.get(k).getFirst();
        if(!((fx1 instanceof NumeralFormula && fy1 instanceof NumeralFormula)||
            (fx1 instanceof RationalFormula && fy1 instanceof RationalFormula))) {
          continue;
        }
        Formula fx2 = variablePairs.get(i).getSecond();
        Formula fy2 = variablePairs.get(k).getSecond();
        Formula eq1 = fmv.makeMinus(fx1, fy1);
        Formula eq2 = fmv.makeMinus(fx2, fy2);
        result.add(fmv.makeEqual(eq1, eq2));
        eq1 = fmv.makePlus(fx1, fy1);
        eq2 = fmv.makePlus(fx2, fy2);
        result.add(fmv.makeEqual(eq1, eq2));
      }
    }

    return result;
  }

  private List<Pair<Formula,Formula>> replaceCommonVariables(List<BooleanFormula> orderedFormulas, int abstractionPoint)
  {
    List<Pair<Formula,Formula>> result = new ArrayList<>();

    List<String> commonNames = getCommonVariableNames(orderedFormulas,abstractionPoint);
    Map<String,String> replacementList = new HashMap<>();
    for(String name : commonNames) {
      replacementList.put(name, name+"'");
    }

    for(int i=0;i<=abstractionPoint;i++) {
      BooleanFormula f = orderedFormulas.get(i);

      f = ufm.typeFormula(FormulaType.BooleanType, renameRek(f,replacementList,result));
      orderedFormulas.set(i, f);
    }

    return result;
  }

  private Formula renameRek(Formula f, Map<String,String> replacementList, List<Pair<Formula,Formula>> replacedVariables)
  {
    if(ufm.isVariable(f)) {
      try {
        String newName = replacementList.get(ufm.getName(f));
        if(newName==null) {
          return f;
        }
        Formula replacement = ufm.replaceName(f, newName);

        Pair<Formula,Formula> p = Pair.of((Formula)ufm.typeFormula(FormulaType.RationalType, f),
                                          (Formula)ufm.typeFormula(FormulaType.RationalType,replacement));
        if(!replacedVariables.contains(p)) {
          replacedVariables.add(p);
        }
        return replacement;
      } catch(Exception e) {
        return f;
      }
    }
    ArrayList<Formula> args = new ArrayList<>();
    for(int i=0;i<ufm.getArity(f);i++) {
      args.add(renameRek(ufm.getArg(f, i),replacementList,replacedVariables));
    }
    if(args.size()>0) {
      return ufm.replaceArgs(f, args.toArray(new Formula[0]));
    }
    return f;
  }

  private List<String> getCommonVariableNames(List<BooleanFormula> orderedFormulas,int abstractionPoint)
  {
    ArrayList<String> result = new ArrayList<>();
    HashMap<String,String> mapA = new HashMap<>();

    for(int i=0;i<=abstractionPoint;i++) {
      for(String name : fmv.extractVariables(orderedFormulas.get(i))) {
        mapA.put(name,name);
      }
    }

    for(int i=abstractionPoint+1;i<orderedFormulas.size();i++) {
      for(String name : fmv.extractVariables(orderedFormulas.get(i))) {
        if(mapA.containsKey(name) && !result.contains(name)) {
          result.add(name);
        }
      }
    }

    return result;
  }

  private List<Formula> extractVariables(Formula f)
  {
    Stack<Formula> stack = new Stack<>();
    stack.push(f);
    List<Formula> result = new ArrayList<>();

    while(!stack.isEmpty()) {
      Formula formula = stack.pop();
      if(ufm.isVariable(formula) || ufm.isUF(formula)) {
        if(!result.contains(formula)) {
          result.add(formula);
        }
      } else {
        for(int i=0;i<ufm.getArity(formula);i++) {
          stack.push(ufm.getArg(formula, i));
        }
      }
    }

    return result;
  }

  private List<Integer> getAbstractionPoints(List<BooleanFormula> orderedFormulas)
  {
    List<Integer> result = new ArrayList<>();

    // only two formulas => only one abstraction point possible
    if(orderedFormulas.size()<3) {
      result.add(0);
      return result;
    }

    // Strategy: Look for similar looking formulas to determine loop bodies.
    int[] locations = new int[orderedFormulas.size()];
    String[] simplifications = new String[orderedFormulas.size()];

    for(int line=0;line<orderedFormulas.size();line++) {
      BooleanFormula f = orderedFormulas.get(line);
      String rep = f.toString();
      rep = rep.replaceAll("@[0-9]+","");
      simplifications[line] = rep;
    }

    for(int line=0;line<orderedFormulas.size();line++) {
      String formula = simplifications[line];
      for(int i=0;i<locations.length;i++) {
        if(line==i || simplifications[i].equals(formula)) {
          locations[i]++;
        }
      }
    }

    /* whenever the location counter gets smaller from one location to another,
* we are likely at the end of a loop (This is a heuristic!)
* => interpolate-enhance.
*/

    for(int i=1;i<locations.length;i++) {
      if(locations[i]<locations[i-1]) {
        result.add(i-1);
      }
    }

    // no suitable location found => assume loop has only be ran once,
    // use line before last line as abstraction point (heuristic)
    if(result.size()==0) {
      result.add(orderedFormulas.size()-2);
    }

    return result;
  }

  private class SATTest<T>
  {
    private InterpolatingProverEnvironment<T> itpProver;

    @SuppressWarnings("unchecked")
    public SATTest(List<BooleanFormula> formulas, int abstractionPoint)
    {
      itpProver = (InterpolatingProverEnvironment<T>)factory.newProverEnvironmentWithInterpolation(false);
      for(int i=0;i<=abstractionPoint;i++) {
        itpProver.push(formulas.get(i));
      }
    }

    public SATTest(List<BooleanFormula> formulas)
    {
      this(formulas,formulas.size()-1);
    }

    public boolean checkFormula(BooleanFormula f)
    {
      itpProver.push(f);
      boolean result = false;
      try {
        result = !itpProver.isUnsat();
      } catch (InterruptedException e) { }
      itpProver.pop();
      return result;
    }

    public boolean checkFormula()
    {
      try {
        return !itpProver.isUnsat();
      } catch(InterruptedException e) { }
      return false;
    }
  }
}