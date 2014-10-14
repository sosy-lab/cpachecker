package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

@Options(prefix="cpa.stator.policy")
public class TemplateManager {
  @Option(name="generateLowerBound",
      description="Generate templates for the lower bounds of each variable")
  private boolean generateLowerBound = true;

  @Option(name="generateUpperBound",
      description="Generate templates for the upper bounds of each variable")
  private boolean generateUpperBound = true;

  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";

  public TemplateManager(Configuration pConfig)
      throws InvalidConfigurationException{
    pConfig.inject(this, TemplateManager.class);
  }

  public PolicyAbstractState.Templates updateTemplatesForEdge(
      PolicyAbstractState.Templates prevTemplates, CFAEdge edge) {

    Set<LinearExpression> newTemplates = new HashSet<>();
    if (edge instanceof ADeclarationEdge) {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) edge;

      if ((declarationEdge.getDeclaration() instanceof AVariableDeclaration)) {
        String varName = declarationEdge.getDeclaration().getQualifiedName();
        if (!varName.contains(TMP_VARIABLE)) {
          // NOTE: Let's also check for liveness! [other property?
          // CPA communication FTW!!].
          // If the variable is no longer alive at a certain location
          // there is no point in tracking it (deeper analysis -> dependencies).
          if (generateUpperBound) {
            newTemplates.add(LinearExpression.ofVariable(varName));
          }
          if (generateLowerBound) {
            newTemplates.add(LinearExpression.ofVariable(varName).negate());
          }
        }
      }
    }
    return prevTemplates.withTemplates(newTemplates);
  }


}
