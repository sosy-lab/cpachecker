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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariable;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;


public class Transition {

  private final TemplateMap tmap;
  private final Location start;
  private final TemplateFormula constraint;
  private final Location end;
  private String eliminationFormula = null;
  private AssumptionSet rrefAssumptions = null;

  public Transition(TemplateMap tmap, Location s, TemplateFormula c, Location e) {
    this.tmap = tmap;
    this.start = s;
    this.constraint = c;
    this.end = e;
  }

  public Location getStart() {
    return start;
  }

  public TemplateFormula getConstraint() {
    return constraint;
  }

  public Location getEnd() {
    return end;
  }

  public AssumptionSet getRREFassumptions() {
    return rrefAssumptions;
  }

  public void setRREFassumptions(AssumptionSet aset) {
    rrefAssumptions = aset;
  }

  public String getEliminationFormula() {
    return eliminationFormula;
  }

  public void setEliminationFormula(String pEliminationFormula) {
    eliminationFormula = pEliminationFormula;
  }

  public boolean hasEliminationFormula() {
    return (eliminationFormula != null);
  }

  public Set<String> writeAllParameters(VariableWriteMode vwm) {
    Set<String> params = new HashSet<>();
    // First build the set of all TemplateVariables.
    Template t = tmap.getTemplate(start);
    Set<TemplateVariable> pvars = t.getAllParmeters();
    t = tmap.getTemplate(end);
    pvars.addAll(t.getAllParmeters());
    // Now write out as a list of Strings.
    for (TemplateVariable p : pvars) {
      params.add(p.toString(vwm));
    }
    return params;
  }

  public Vector<TemplateBoolean> getAllNonzeroParameterClauses() {
    Vector<TemplateBoolean> clauses = new Vector<>();
    Template t = tmap.getTemplate(start);
    clauses.add(t.getNonzeroParameterClause());
    t = tmap.getTemplate(end);
    clauses.add(t.getNonzeroParameterClause());
    return clauses;
  }

}


















