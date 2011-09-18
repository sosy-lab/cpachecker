package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.InfixReln;


public class TemplateChooser {

  // Options:
  private TemplateChooserStrategy strategy;
  private InfixReln relation = InfixReln.EQUAL;

  private final TemplateFormula entryFormula;
  private final TemplateFormula loopFormula;
  private final TemplateFormula exitFormula;

  public enum TemplateChooserStrategy {
    LINCOMBLEQ, EXIT_FORMULA;
  }

  public TemplateChooser(TemplateFormula entryFormula,
      TemplateFormula loopFormula, TemplateFormula exitFormula, TemplateChooserStrategy strategy) {
    this.entryFormula = entryFormula;
    this.loopFormula = loopFormula;
    this.exitFormula = exitFormula;
    this.strategy = strategy;
  }

  public TemplateFormula chooseTemplate() {
    TemplateFormula choice = null;
    switch (strategy) {
    case LINCOMBLEQ:
      choice = linCombAllVarsAndUIFsStrategy(); break;
    case EXIT_FORMULA:
      choice = exitFormulaStrategy(); break;
    }
    return choice;
  }

  private TemplateFormula linCombAllVarsAndUIFsStrategy() {
    TemplateFormula choice = null;

    // Get all forms.
    Set<TermForm> forms = entryFormula.getTopLevelTermForms();
    forms.addAll( loopFormula.getTopLevelTermForms() );
    forms.addAll( exitFormula.getTopLevelTermForms() );

    // Convert to terms, and sum up for LHS.
    Vector<TemplateTerm> terms = new Vector<TemplateTerm>();
    for (TermForm f : forms) {
      terms.add( f.getTemplate() );
    }
    TemplateSum LHS = new TemplateSum(terms);

    // Make RHS parameter.
    TemplateVariable param = TemplateTerm.getNextFreshParameter();
    TemplateTerm RHS = new TemplateTerm();
    RHS.setParameter(param);

    // Build template as constraint.
    choice = new TemplateConstraint(LHS, relation, RHS);
    return choice;
  }

  private TemplateFormula exitFormulaStrategy() {
    // TODO: exit formula strategy
    // simply turns exitFormula into a template (dropping indices, etc.)
    return null;
  }

}
