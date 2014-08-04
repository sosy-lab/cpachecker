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
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.templates.Purification;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFalse;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTrue;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariable;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;

public class Template {

  private final TemplateFormula templateFormula;
  private final TemplateBoolean nonzeroParameterClause;
  private final Set<TemplateVariable> allParameters;
  private final Set<TemplateVariable> allVariables;

  /*
   * If you do not wish to specify any particular nonzero parameter
   * clause, then we take it to be 'true', since it will be conjoined at
   * the end of our formula, whence 'true' is inert.
   */
  public Template(TemplateFormula tF) {
    this.templateFormula = tF;
    this.nonzeroParameterClause = new TemplateTrue();
    this.allParameters = tF.getAllParameters();
    this.allVariables = tF.getAllVariables();
  }

  public Template(TemplateFormula tF, TemplateFormula nzPC) {
    this.templateFormula = tF;
    this.nonzeroParameterClause = (TemplateBoolean) nzPC;
    this.allParameters = tF.getAllParameters();
    this.allVariables = tF.getAllVariables();
  }

  public static Template makeTrueTemplate() {
    TemplateFormula f = new TemplateTrue();
    Template t = new Template(f);
    return t;
  }

  public static Template makeFalseTemplate() {
    TemplateFormula f = new TemplateFalse();
    Template t = new Template(f);
    return t;
  }

  public TemplateFormula getTemplateFormula() {
    return templateFormula;
  }

  public TemplateBoolean getNonzeroParameterClause() {
    return nonzeroParameterClause;
  }

  public Set<TemplateVariable> getAllParmeters() {
    return allParameters;
  }

  public Set<String> writeAllParameters(VariableWriteMode vwm) {
    Set<String> params = new HashSet<>();
    for (TemplateVariable p : allParameters) {
      params.add(p.toString(vwm));
    }
    return params;
  }

  public Set<TemplateVariable> getAllVariables() {
    return allVariables;
  }

  public Purification purify(Purification pur) {
    return templateFormula.purify(pur);
  }

  public boolean evaluate(Map<String, Rational> vals) {
    return templateFormula.evaluate(vals);
  }

  @Override
  public String toString() {
    return templateFormula.toString();
  }

}
