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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.collect.ImmutableSet;

@Options(prefix="cpa.stator.policy")
public class TemplateManager {
  @Option(secure=true,
      description="Generate templates for the lower bounds of each variable")
  private boolean generateLowerBound = true;

  @Option(secure=true,
      description="Generate templates for the upper bounds of each variable")
  private boolean generateUpperBound = true;

  @Option(secure=true,
      description="Generate octagon templates for all combinations of variables. " +
          "This can be expensive.")
  private boolean generateOctagons = false;

  @Option(secure=true,
      description="Ignore the template type and encode with a rational variable")
  private boolean encodeTemplatesAsRationals = false;

  private final CFA cfa;
  private final LogManager logger;
  private final NumeralFormulaManagerView<
      NumeralFormula, NumeralFormula.RationalFormula> rfmgr;
  private final NumeralFormulaManagerView<NumeralFormula.IntegerFormula,
      NumeralFormula.IntegerFormula> ifmgr;
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgrv;

  /**
   * Dummy edge required by the interface to convert the {@code CIdExpression}
   * into formula.
   */
  private final CFAEdge dummyEdge;


  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";
  private static final String RET_VARIABLE = "__retval__";

  public TemplateManager(
      LogManager pLogger,
      Configuration pConfig,
      CFA pCfa,
      FormulaManagerView pFormulaManagerView,
      PathFormulaManager pPfmgr
      ) throws InvalidConfigurationException{
    pfmgr = pPfmgr;
    pConfig.inject(this, TemplateManager.class);
    cfa = pCfa;
    logger = pLogger;
    rfmgr = pFormulaManagerView.getRationalFormulaManager();
    ifmgr = pFormulaManagerView.getIntegerFormulaManager();
    fmgrv = pFormulaManagerView;

    dummyEdge = new BlankEdge("",
        FileLocation.DUMMY,
        new CFANode("dummy-1"), new CFANode("dummy-2"), "Dummy Edge");
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
      CIdExpression idExpression = new CIdExpression(
          FileLocation.DUMMY, (CSimpleDeclaration)s
      );
      logger.log(Level.FINEST, "Processing variable", varName);
      if (generateUpperBound) {
        out.add(new Template(LinearExpression.ofVariable(idExpression), type));
      }
      if (generateLowerBound) {
        out.add(new Template(LinearExpression.ofVariable(idExpression).negate(), type));
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
          CIdExpression idExpression1 = new CIdExpression(
              FileLocation.DUMMY, (CSimpleDeclaration)s1
          );
          CIdExpression idExpression2 = new CIdExpression(
              FileLocation.DUMMY, (CSimpleDeclaration)s2
          );
          LinearExpression<CIdExpression> expr1 = LinearExpression.ofVariable(
              idExpression1);
          LinearExpression<CIdExpression> expr2 = LinearExpression.ofVariable(
              idExpression2);

          out.add(new Template(expr1.add(expr2), type));
          out.add(new Template(expr1.sub(expr2), type));
          out.add(new Template(expr2.sub(expr1), type));
        }
      }
    }
    return out.build();
  }

  public Formula toFormula(Template template, PathFormula pPathFormula) {
    return toFormula(template, pPathFormula, "");
  }

  public Formula toFormula(
      Template template, PathFormula pPathFormula, String customPrefix
  ) {
    boolean useRationals = shouldUseRationals(template);
    Formula sum = null;

    for (Map.Entry<CIdExpression, Rational> entry : template.linearExpression) {
      Rational coeff = entry.getValue();
      CIdExpression declaration = entry.getKey();

      Formula item;
      try {
        item = pfmgr.expressionToFormula(
            pPathFormula, declaration, dummyEdge);
      } catch (UnrecognizedCCodeException e) {
        throw new UnsupportedOperationException();
      }

      if (coeff == Rational.ZERO) {
        continue;
      } else if (coeff == Rational.NEG_ONE) {
        item = fmgrv.makeNegate(item);
      } else if (coeff != Rational.ONE){
        item = fmgrv.makeMultiply(
            item, fmgrv.makeNumber(item, entry.getValue())
        );
      }

      if (sum == null) {
        sum = item;
      } else {
        sum = fmgrv.makePlus(sum, item);
      }
    }

    if (sum == null) {
      if (useRationals) {
        return rfmgr.makeNumber(0);
      } else {
        return ifmgr.makeNumber(0);
      }
    } else {
      if (customPrefix.equals("")) {
        return sum;
      } else {
        return fmgrv.addPrefixToAllVariables(sum, customPrefix);
      }
    }
  }

  public boolean shouldUseRationals(Template template) {
    if (encodeTemplatesAsRationals) {
      return true;
    }
    if (!template.linearExpression.isIntegral()) {
      return true;
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
