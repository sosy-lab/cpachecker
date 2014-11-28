package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

@Options(prefix="cpa.stator.policy")
public class TemplateManager {
  @Option(secure=true, name="generateLowerBound",
      description="Generate templates for the lower bounds of each variable")
  private boolean generateLowerBound = true;

  @Option(secure=true, name="generateUpperBound",
      description="Generate templates for the upper bounds of each variable")
  private boolean generateUpperBound = true;

  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";

  public TemplateManager(Configuration pConfig)
      throws InvalidConfigurationException{
    pConfig.inject(this, TemplateManager.class);
  }

  public Set<Template> templatesForEdge(CFAEdge edge) {
    Set<Template> templates = new HashSet<>();
    if (edge instanceof ADeclarationEdge) {
      CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
      CDeclaration declaration = declarationEdge.getDeclaration();

      // Only C is handled for now.
      if (declaration instanceof CVariableDeclaration) {
        CVariableDeclaration variableDeclaration =
            (CVariableDeclaration)declaration;

        String varName = declarationEdge.getDeclaration().getQualifiedName();
        if (!varName.contains(TMP_VARIABLE)) {

          // TODO: check for liveness, this information is available now.
          // If the variable is no longer alive at a certain location
          // there is no point in tracking it (deeper analysis -> dependencies).
          if (generateUpperBound) {
            templates.add(
                new Template(
                    LinearExpression.ofVariable(varName),
                    variableDeclaration
                )
            );
          }
          if (generateLowerBound) {
            templates.add(
                new Template(
                    LinearExpression.ofVariable(varName).negate(),
                    variableDeclaration));
          }
        }
      }
    }
    return templates;
  }


}
