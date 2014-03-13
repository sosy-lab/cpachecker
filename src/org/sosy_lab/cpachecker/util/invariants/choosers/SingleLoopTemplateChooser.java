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
package org.sosy_lab.cpachecker.util.invariants.choosers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.balancer.Template;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConjunction;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConstraint;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateDisjunction;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateNegation;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateSum;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateSumList;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateUIF;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariable;
import org.sosy_lab.cpachecker.util.invariants.templates.TermForm;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;


public class SingleLoopTemplateChooser implements TemplateChooser {

  private final LogManager logger;
  private final TemplateFormula entryFormula;
  private final TemplateFormula loopFormula;
  private final TemplateFormula loopFormulaHead;
  private final TemplateFormula loopFormulaTail;
  private final TemplateFormula exitFormula;
  private final TemplateFormula exitFormulaHead;
  private final TemplateFormula exitFormulaTail;
  private final TemplateChooserStrategy strategy;
  private FormulaType<? extends NumeralFormula> type;


  public SingleLoopTemplateChooser(LogManager logger,
      TemplateFormula entryFormula,
      TemplateFormula loopFormula, TemplateFormula loopFormulaHead, TemplateFormula loopFormulaTail,
      TemplateFormula exitFormula, TemplateFormula exitFormulaHead, TemplateFormula exitFormulaTail) {
    this.logger = logger;
    this.entryFormula = entryFormula;
    this.loopFormula = loopFormula;
    this.loopFormulaHead = loopFormulaHead;
    this.loopFormulaTail = loopFormulaTail;
    this.exitFormula = exitFormula;
    this.exitFormulaHead = exitFormulaHead;
    this.exitFormulaTail = exitFormulaTail;
    this.strategy = new TemplateChooserStrategy();

    // TODO: review this change
    type = FormulaType.RationalType;
  }

  private enum TemplateChooserMethod {
    TOPLEVELTERMFORMS,
    LOOPVARSFREECOMB,
    LOOPVARSFREECOMBANDLOOPHEADFREECOMB,
    EXITHEADNEGATION,
    EXITTAILCOMB;
  }

  /*
   * Cycles through combinations of methods and relations.
   * To use, call:
   * (0) advance
   * (1) getMethod
   * (2) getRelation
   */
  // FIXME: For methods that do not depend on an infix relation, we should save
  // time by not trying them repeatedly, once for each possible infix relation!
  private class TemplateChooserStrategy {

    private TemplateChooserMethod[] methods = {
        TemplateChooserMethod.LOOPVARSFREECOMBANDLOOPHEADFREECOMB,
        TemplateChooserMethod.LOOPVARSFREECOMB,
        TemplateChooserMethod.EXITTAILCOMB,
        TemplateChooserMethod.EXITHEADNEGATION,
        TemplateChooserMethod.TOPLEVELTERMFORMS
    };
    private InfixReln[] relns = {
        InfixReln.EQUAL, InfixReln.LT, InfixReln.LEQ
    };
    private int methodIndex = 0;
    private int relnIndex = -1;

    /*
     * Advances counters to point to next combination.
     * Returns true if there is indeed a next; false if not.
     */
    public boolean advance() {
      if (relnIndex >= relns.length - 1) {
        relnIndex = 0;
        methodIndex++;
      } else {
        relnIndex++;
      }
      return (methodIndex < methods.length);
    }

    public TemplateChooserMethod getMethod() {
      TemplateChooserMethod m = methods[0];
      if (methodIndex < methods.length) {
        m = methods[methodIndex];
      }
      return m;
    }

    public InfixReln getRelation() {
      InfixReln r = relns[0];
      if (relnIndex < relns.length) {
        r = relns[relnIndex];
      }
      return r;
    }

  }

  @Override
  public Template chooseNextTemplate() {
    Template choice = null;
    boolean another = strategy.advance();
    if (!another) {
      return null;
    }
    TemplateChooserMethod method = strategy.getMethod();
    InfixReln relation = strategy.getRelation();

    logger.log(Level.ALL, "Template choice method:", method, "Template infix relation:", relation);

    switch (method) {
    case TOPLEVELTERMFORMS:
      choice = topLevelTermFormsMethod(relation); break;
    case EXITHEADNEGATION:
      choice = exitHeadNegationMethod(relation); break;
    case EXITTAILCOMB:
      choice = exitTailIndexOneFreeCombMethod(relation); break;
    case LOOPVARSFREECOMB:
      choice = loopVarsFreeCombMethod(relation); break;
    case LOOPVARSFREECOMBANDLOOPHEADFREECOMB:
      choice = loopVarsAndHeadFreeCombMethod(relation); break;
    }
    return choice;
  }

  private Template loopVarsAndHeadFreeCombMethod(InfixReln relation) {
    Template choice = null;
    // Get all loop head and loop tail variables.
    Set<TemplateTerm> headVars = getUnindexedVarsAsTerms(loopFormulaHead);
    Set<TemplateTerm> tailVars = getUnindexedVarsAsTerms(loopFormulaTail);

    // Declare a list of terms for each conjunct.
    List<TemplateTerm> headTerms = new Vector<>();
    List<TemplateTerm> tailTerms = new Vector<>();

    // Add the loop vars.
    headTerms.addAll(headVars);
    tailTerms.addAll(tailVars);

    // Create a fresh parameter linear combination of the terms for each conjunct.
    TemplateSum headLHS = TemplateSum.freshParamLinComb(type, headTerms);
    TemplateSum tailLHS = TemplateSum.freshParamLinComb(type, tailTerms);

    // Make RHS parameters.
    TemplateVariable param = TemplateTerm.getNextFreshParameter(type);
    TemplateTerm headRHS = new TemplateTerm(type);
    headRHS.setParameter(param);
    param = TemplateTerm.getNextFreshParameter(type);
    TemplateTerm tailRHS = new TemplateTerm(type);
    tailRHS.setParameter(param);

    // Build the conjuncts as constraints.
    TemplateFormula headFormula = new TemplateConstraint(headLHS, InfixReln.LEQ, headRHS);
    TemplateFormula tailFormula = new TemplateConstraint(tailLHS, InfixReln.EQUAL, tailRHS);

    // Form the conjunction.
    TemplateFormula formula = TemplateConjunction.conjoin((TemplateBoolean)headFormula, (TemplateBoolean)tailFormula);

    // Make nonzero parameter clause.
    // Namely, we simply ask that not all of the top-level parameters on
    // the LHS be zero.
    Set<TemplateVariable> topLevelLHSparams = headLHS.getTopLevelParameters();
    topLevelLHSparams.addAll(tailLHS.getTopLevelParameters());
    TemplateFormula nzpc = makeBasicParamClause(topLevelLHSparams);

    choice = new Template(formula, nzpc);
    return choice;
  }

  private Template loopVarsFreeCombMethod(InfixReln relation) {
    Template choice = null;
    // Get all loop variables.
    Set<TemplateTerm> loopVars = getUnindexedVarsAsTerms(loopFormula);

    // Get all distinct UIF names occurring at the top level in the loop formula,
    // and their arities.
    Map<String, Integer> uifNA = getTopLevelUIFnamesAndArities(loopFormula);

    // Declare the list of terms for the template.
    List<TemplateTerm> templateTerms = new Vector<>();

    // First add one term for each UIF name.
    int arity;
    TemplateUIF uif;
    for (String name : uifNA.keySet()) {
      arity = uifNA.get(name).intValue();
      TemplateSum[] args = new TemplateSum[arity];
      // each arg is a fresh parameter linear combination over loopVars
      for (int i = 0; i < arity; i++) {
        args[i] = TemplateSum.freshParamLinComb(type, loopVars);
      }
      // create the uif
      uif = new TemplateUIF(name, type, new TemplateSumList(args));
      // put it in a term
      templateTerms.add(new TemplateTerm(uif));
    }

    // Now add the loop vars themselves.
    templateTerms.addAll(loopVars);

    // Finally create a fresh parameter linear combination of all the terms.
    TemplateSum LHS = TemplateSum.freshParamLinComb(type, templateTerms);

    // Make RHS parameter.
    TemplateVariable param = TemplateTerm.getNextFreshParameter(type);
    TemplateTerm RHS = new TemplateTerm(type);
    RHS.setParameter(param);

    // Build template formula as constraint.
    TemplateFormula formula = new TemplateConstraint(LHS, relation, RHS);

    // Make nonzero parameter clause.
    // Namely, we simply ask that not all of the top-level parameters on
    // the LHS be zero.
    Set<TemplateVariable> topLevelLHSparams = LHS.getTopLevelParameters();
    TemplateFormula nzpc = makeBasicParamClause(topLevelLHSparams);

    choice = new Template(formula, nzpc);
    return choice;
  }

  private Set<TemplateTerm> getUnindexedVarsAsTerms(TemplateFormula f) {
    Set<TemplateVariable> vars = f.getAllVariables();
    Set<String> varNames = new HashSet<>();
    for (TemplateVariable v : vars) {
      varNames.add(v.getName());
    }
    Set<TemplateTerm> terms = new HashSet<>();
    TemplateTerm t;
    for (String name : varNames) {
      t = new TemplateTerm(type);
      t.setVariable(new TemplateVariable(type, name));
      terms.add(t);
    }
    return terms;
  }

  private Map<String, Integer> getTopLevelUIFnamesAndArities(TemplateFormula f) {
    Set<TemplateUIF> topLevelUIFs = f.getAllTopLevelUIFs();
    HashMap<String, Integer> map = new HashMap<>();
    String name;
    Integer arity;
    for (TemplateUIF u : topLevelUIFs) {
      name = u.getName();
      arity = Integer.valueOf(u.getArity());
      map.put(name, arity);
    }
    return map;
  }

  private Template topLevelTermFormsMethod(InfixReln relation) {
    Template choice = null;

    // Get all forms.
    Set<TermForm> forms = entryFormula.getTopLevelTermForms();
    forms.addAll(loopFormula.getTopLevelTermForms());
    forms.addAll(exitFormula.getTopLevelTermForms());

    // Convert to terms, and sum up for LHS.
    Vector<TemplateTerm> terms = new Vector<>();
    for (TermForm f : forms) {
      terms.add(f.getTemplate());
    }
    TemplateSum LHS = new TemplateSum(FormulaType.RationalType, terms);

    // Make RHS parameter.
    TemplateTerm RHS = makeRHSParameter();

    // Build template formula as constraint.
    TemplateFormula formula = new TemplateConstraint(LHS, relation, RHS);

    // Make nonzero parameter clause.
    // Namely, we simply ask that not all of the top-level parameters on
    // the LHS be zero.
    Set<TemplateVariable> topLevelLHSparams = LHS.getTopLevelParameters();
    TemplateFormula nzpc = makeBasicParamClause(topLevelLHSparams);

    choice = new Template(formula, nzpc);
    return choice;
  }

  private TemplateTerm makeRHSParameter() {
    TemplateVariable param = TemplateTerm.getNextFreshParameter(type);
    TemplateTerm RHS = new TemplateTerm(type);
    RHS.setParameter(param);
    return RHS;
  }

  /*
   * Negates the path formula for the lead edge of the exit path, puts this in
   * strong DNF, as D1 v D2 v ... Dn, and then attempts to establish one of the Di
   * as template.
   *
   * The idea here is that maybe we never actually exit the loop along this edge.
   */
  private Template exitHeadNegationMethod(InfixReln relation) {
    // FIXME: we shouldn't have to do this type cast here.
    TemplateBoolean head = (TemplateBoolean) exitFormulaHead;
    // negate
    head = head.logicNegate();
    // put in strong DNF
    TemplateDisjunction headD = (TemplateDisjunction) head.makeSDNF();
    // get list of disjuncts
    List<TemplateBoolean> disjuncts = headD.getDisjuncts();
    // TODO: Refactor so that it is possible for a method, like this one,
    // to create all at once a list of several templates to try.
    // For we would like to try one for each disjunct.
    // For now, as a quick fix, we simply take the first.
    TemplateBoolean temp = disjuncts.get(0);
    Set<TemplateVariable> tllhsp = temp.getTopLevelLHSParameters();
    TemplateFormula nzpc = makeBasicParamClause(tllhsp);
    Template t = new Template(temp, nzpc);
    return t;
  }

  /*
   * Let the "tail formula" of the exit path be the path formula for all edges of the
   * exit path except the first. We find the set of all terms in the tail formula in which
   * all program variables appear with SSA index 1 (meaning this regards their state upon exit
   * of the loop, and prior to any subsequent modification), and we form a fresh-parameter
   * linear combination of these terms for the LHS of our template. The RHS is another
   * fresh parameter, and the relation is the one passed as argument.
   *
   * The idea is to try this method after the exitHeadNegationMethod. If that one failed,
   * then probably we didn't enter the error state simply because of the edge along which
   * we exited the loop; instead, it might have been because something which is actually
   * true of the program variables on loop exit was supposed not to be. For example, we might
   * have reached the error state because of a failed 'assert' statement, which simply asserts
   * something about the variables involved in the loop. Our hope is to build as invariant the
   * very statement that is asserted.
   */
  private Template exitTailIndexOneFreeCombMethod(InfixReln relation) {
    // Get top-level terms.
    Set<TemplateTerm> terms = exitFormulaTail.getTopLevelTerms();

    // Keep just those that have max index 1.
    Set<TemplateTerm> indexone = new HashSet<>();
    int n;
    for (TemplateTerm t : terms) {
      n = t.getMaxIndex();
      if (n == 1) {
        indexone.add(t);
      }
    }

    // Combine these for LHS.
    TemplateSum LHS = TemplateSum.freshParamLinComb(type, indexone);

    // Make RHS parameter.
    TemplateTerm RHS = makeRHSParameter();

    // Build template formula as constraint.
    TemplateFormula formula = new TemplateConstraint(LHS, relation, RHS);
    formula.unindex();

    // Make nonzero parameter clause.
    // Namely, we simply ask that not all of the top-level parameters on
    // the LHS be zero.
    Set<TemplateVariable> topLevelLHSparams = LHS.getTopLevelParameters();
    TemplateFormula nzpc = makeBasicParamClause(topLevelLHSparams);

    Template choice = new Template(formula, nzpc);
    return choice;
  }

  /*
   * Creates the basic nonzero parameter clause saying that not all
   * of the parameters in the passed set be zero.
   */
  private TemplateFormula makeBasicParamClause(Set<TemplateVariable> params) {
    Vector<TemplateBoolean> conjuncts = new Vector<>();
    TemplateConstraint c;
    for (TemplateVariable p : params) {
      c = new TemplateConstraint(p, InfixReln.EQUAL);
      conjuncts.add(c);
    }
    TemplateConjunction conj = new TemplateConjunction(conjuncts);
    TemplateBoolean neg = TemplateNegation.negate(conj);
    return neg;
  }


}
