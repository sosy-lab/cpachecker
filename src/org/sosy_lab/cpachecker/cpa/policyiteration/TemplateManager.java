package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.collect.ImmutableSet;

@Options(prefix="cpa.stator.policy")
public class TemplateManager {
  private final CFA cfa;
  private final LogManager logger;

  @Option(secure=true, name="generateLowerBound",
      description="Generate templates for the lower bounds of each variable")
  private boolean generateLowerBound = true;

  @Option(secure=true, name="generateUpperBound",
      description="Generate templates for the upper bounds of each variable")
  private boolean generateUpperBound = true;

  @Option(secure=true, name="generateOctagons",
      description="Generate octagon templates for all combinations of variables. " +
          "This can be very expensive.")
  private boolean generateOctagons = false;

  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";
  private static final String RET_VARIABLE = "__retval__";

  public TemplateManager(LogManager pLogger, Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException{
    pConfig.inject(this, TemplateManager.class);
    cfa = pCfa;
    logger = pLogger;
  }

  public ImmutableSet<Template> templatesForNode(CFANode node) {
    ImmutableSet.Builder<Template> out = ImmutableSet.builder();
    LiveVariables liveVariables = cfa.getLiveVariables().get();
    Set<ASimpleDeclaration> liveVars = liveVariables.getLiveVariablesForNode(node);
    for (ASimpleDeclaration s : liveVars) {

      if (!shouldProcessVariable(s)) {
        continue;
      }
      String varName = s.getQualifiedName();
      Type type = s.getType();
      logger.log(Level.FINEST, "Processing variable", varName);
      if (generateUpperBound) {
        out.add(new Template(LinearExpression.ofVariable(varName), type));
      }
      if (generateLowerBound) {
        out.add(new Template(LinearExpression.ofVariable(varName).negate(),
            type));
      }
    }

    if (generateOctagons) {
      for (ASimpleDeclaration s1 : liveVars) {
        for (ASimpleDeclaration s2 : liveVars) {
          if (!shouldProcessVariable(s1)
              || !shouldProcessVariable(s2)) {
            continue;
          }
          if (s1.equals(s2)) { // Don't pair up the same var.
            continue;
          }
          if (!s1.getType().equals(s2.getType())) {

            // Don't pair up variables of different types.
            continue;
          }

          Type type = s1.getType();
          String varName1 = s1.getQualifiedName();
          LinearExpression expr1 = LinearExpression.ofVariable(varName1);
          String varName2 = s2.getQualifiedName();
          LinearExpression expr2 = LinearExpression.ofVariable(varName2);

          out.add(new Template(expr1.add(expr2), type));
          out.add(new Template(expr1.sub(expr2), type));
          out.add(new Template(expr2.sub(expr1), type));
        }
      }
    }
    return out.build();
  }

  /**
   * Ignore temporary variables and pointers.
   */
  private boolean shouldProcessVariable(ASimpleDeclaration var) {
    return !var.getQualifiedName().contains(TMP_VARIABLE)
        && !var.getType().toString().contains("*")
        && !var.getQualifiedName().contains(RET_VARIABLE);

  }
}
