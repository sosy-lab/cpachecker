package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Map;
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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.collect.ImmutableSet;

@Options(prefix="cpa.stator.policy")
public class TemplateManager {
  private final CFA cfa;
  private final LogManager logger;
  private final NumeralFormulaManagerView<
        NumeralFormula, NumeralFormula.RationalFormula> rfmgr;
  private final NumeralFormulaManagerView<NumeralFormula.IntegerFormula,
      NumeralFormula.IntegerFormula> ifmgr;

  @Option(secure=true, name="generateLowerBound",
      description="Generate templates for the lower bounds of each variable")
  private boolean generateLowerBound = true;

  @Option(secure=true, name="generateUpperBound",
      description="Generate templates for the upper bounds of each variable")
  private boolean generateUpperBound = true;

  @Option(secure=true, name="generateOctagons",
      description="Generate octagon templates for all combinations of variables. " +
          "This can be expensive.")
  private boolean generateOctagons = false;

  @Option(secure=true, name="encodeTemplatesAsRationals",
      description="Ignore the template type and encode with a rational variable")
  private boolean encodeTemplatesAsRationals = false;

  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";
  private static final String RET_VARIABLE = "__retval__";

  public TemplateManager(
      LogManager pLogger,
      Configuration pConfig,
      CFA pCfa,
      FormulaManagerView pFormulaManagerView
  ) throws InvalidConfigurationException{
    pConfig.inject(this, TemplateManager.class);
    cfa = pCfa;
    logger = pLogger;
    rfmgr = pFormulaManagerView.getRationalFormulaManager();
    ifmgr = pFormulaManagerView.getIntegerFormulaManager();
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
      CSimpleType type = (CSimpleType) s.getType();
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
          if (s1 == s2) { // Don't pair up the same var.
            continue;
          }
          if (!s1.getType().equals(s2.getType())) {

            // Don't pair up variables of different types.
            continue;
          }

          CSimpleType type = (CSimpleType) s1.getType();
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

  public NumeralFormula toFormula(
      Template template, SSAMap pSSAMap
  ) {
    return toFormula(template, pSSAMap, "");
  }

  public NumeralFormula toFormula(
      Template template, SSAMap pSSAMap, String customPrefix
  ) {

    boolean useRationals = shouldUseRationals(template);
    NumeralFormula sum = null;
    for (Map.Entry<String, Rational> entry : template.linearExpression) {
      Rational coeff = entry.getValue();
      String origVarName = entry.getKey();

      // SSA index shouldn't be zero.
      int idx = Math.max(pSSAMap.getIndex(origVarName), 1);

      NumeralFormula item;
      if (useRationals) {
        item = rfmgr.makeVariable(customPrefix + origVarName, idx);
      } else {
        item = ifmgr.makeVariable(customPrefix + origVarName, idx);
      }

      if (coeff == Rational.ZERO) {
        continue;
      } else if (coeff == Rational.NEG_ONE) {
        if (useRationals) {
          item = rfmgr.negate(item);
        } else {
          item = ifmgr.negate((NumeralFormula.IntegerFormula)item);
        }
      } else if (coeff != Rational.ONE){
        if (useRationals) {
          item = rfmgr.multiply(
              item, rfmgr.makeNumber(entry.getValue().toString()));
        } else {
          item = ifmgr.multiply(
              (NumeralFormula.IntegerFormula)item,
              ifmgr.makeNumber(entry.getValue().toString()));
        }
      }

      if (sum == null) {
        sum = item;
      } else {
        if (useRationals) {
          sum = rfmgr.add(sum, item);
        } else {
          sum = ifmgr.add(
              (NumeralFormula.IntegerFormula)sum,
              (NumeralFormula.IntegerFormula)item);
        }
      }
    }

    if (sum == null) {
      if (useRationals) {
        return rfmgr.makeNumber(0);
      } else {
        return ifmgr.makeNumber(0);
      }
    } else {
      return sum;
    }
  }

  public boolean shouldUseRationals(Template template) {
    if (encodeTemplatesAsRationals) {
      return true;
    }
    for (Map.Entry<String, Rational> e : template.linearExpression) {
      if (!e.getValue().isIntegral()) {
        return false;
      }
    }
    switch (template.type.getType()) {
      case BOOL:
      case INT:
        return false;
      case UNSPECIFIED:
      case CHAR:
      case FLOAT:
      case DOUBLE:
        return true;
      default:
        throw new IllegalArgumentException("Unexpected type: " + template.type);
    }
  }

  /**
   * Ignore temporary variables and pointers.
   */
  private boolean shouldProcessVariable(ASimpleDeclaration var) {
    return !var.getQualifiedName().contains(TMP_VARIABLE)
        && var.getType() instanceof CSimpleType
        && !var.getType().toString().contains("*")
        && !var.getQualifiedName().contains(RET_VARIABLE);

  }
}
