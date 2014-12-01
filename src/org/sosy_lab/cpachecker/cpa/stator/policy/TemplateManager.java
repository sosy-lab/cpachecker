package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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

  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";

  public TemplateManager(LogManager pLogger, Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException{
    pConfig.inject(this, TemplateManager.class);
    cfa = pCfa;
    logger = pLogger;
  }

  public ImmutableSet<Template> templatesForNode(CFANode node) {
    ImmutableSet.Builder<Template> out = ImmutableSet.builder();
    LiveVariables liveVariables = cfa.getLiveVariables().get();
    for (ASimpleDeclaration s : liveVariables.getLiveVariablesForNode(node)) {
      String varName = s.getQualifiedName();
      logger.log(Level.FINEST, "Processing variable", varName);
      if (varName.contains(TMP_VARIABLE)) {
        continue;
      }
      if (generateUpperBound) {
        out.add(new Template(LinearExpression.ofVariable(varName), s));
      }
      if (generateLowerBound) {
        out.add(new Template(LinearExpression.ofVariable(varName).negate(), s));
      }
    }
    return out.build();
  }
}
