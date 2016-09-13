/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.templates;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LiveVariables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Precision object for template-based analysis.
 */
@Options(prefix = "precision.template", deprecatedPrefix = "cpa.lpi")
public class TemplatePrecision implements Precision {

  @Option(secure=true, description="Generate templates from assert statements")
  private boolean generateFromAsserts = true;

  @Option(secure=true, description="Generate templates from all program "
      + "statements")
  private boolean generateFromStatements = false;

  @Option(secure=true, description="Maximum size for the generated template")
  private int maxExpressionSize = 1;

  @Option(secure=true, description="Generate difference constraints."
      + "This option is redundant for `maxExpressionSize` >= 2.")
  private boolean generateDifferences = false;

  @Option(secure=true, description="Allowed coefficients in a template.")
  private Set<Rational> allowedCoefficients = ImmutableSet.of(
      Rational.NEG_ONE, Rational.ONE
  );

  @Option(secure=true,
    description="Strategy for filtering variables out of templates using "
        + "liveness")
  private VarFilteringStrategy varFiltering = VarFilteringStrategy.ALL_LIVE;

  @Option(secure=true,
      description="Do not generate templates with threshold larger than specified."
          + " Set to '-1' for no limit.")
  private long templateConstantThreshold = 100;

  @Option(secure=true,
      description="Force the inclusion of function parameters into the "
          + "generated templates. Required for summaries computation.")
  private boolean includeFunctionParameters = false;

  public enum VarFilteringStrategy {

    /**
     * Generate only templates where all variables are alive.
     */
    ALL_LIVE,

    /**
     * Generate only templates where at least one variable is alive.
     */
    ONE_LIVE,

    /**
     * Generate all templates.
     */
    ALL
  }

  private final CFA cfa;
  private final LogManager logger;

  private final ImmutableSet<Template> extractedFromAssertTemplates;
  private ImmutableSet<Template> extractedTemplates;
  private final Set<Template> extraTemplates;
  private final Set<Template> generatedTemplates;
  private final TemplateToFormulaConversionManager
      templateToFormulaConversionManager;

  // Temporary variables created by CPA checker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";

  // todo: do not hardcode, use automaton.
  private static final String ASSERT_FUNC_NAME = "assert";
  private static final String ASSERT_H_FUNC_NAME = "__assert_fail";

  /**
   * Cache of generated templates.
   */
  private final ListMultimap<CFANode, Template> cache =
      ArrayListMultimap.create();
  private final ImmutableSet<ASimpleDeclaration> allVariables;

  /**
   * Mapping from function name to a set of function parameters.
   * Variables represented parameters should be kept in precision
   * at the return node in order to compute summaries.
   */
  private final ImmutableMap<String, Set<ASimpleDeclaration>> functionParameters;

  public TemplatePrecision(
      LogManager pLogger,
      Configuration pConfig,
      CFA pCfa,
      TemplateToFormulaConversionManager pTemplateToFormulaConversionManager)
        throws InvalidConfigurationException {
    templateToFormulaConversionManager = pTemplateToFormulaConversionManager;

    pConfig.inject(this, TemplatePrecision.class);
    extraTemplates = new HashSet<>();

    cfa = pCfa;
    logger = pLogger;

    if (generateFromAsserts) {
      extractedFromAssertTemplates = ImmutableSet.copyOf(templatesFromAsserts());
    } else {
      extractedFromAssertTemplates = ImmutableSet.of();
    }
    logger.log(Level.FINE, "Generated from assert templates",
        extractedFromAssertTemplates);

    if (generateFromStatements) {
      extractedTemplates = ImmutableSet.copyOf(extractTemplates());
    } else {
      extractedTemplates = ImmutableSet.of();
    }
    logger.log(Level.FINE, "Generated templates", extractedFromAssertTemplates);
    generatedTemplates = new HashSet<>();

    allVariables = ImmutableSet.copyOf(
        cfa.getLiveVariables().get().getAllLiveVariables());

    ImmutableMap.Builder<String, Set<ASimpleDeclaration>> builder = ImmutableMap.builder();
    if (includeFunctionParameters) {
      for (FunctionEntryNode node : cfa.getAllFunctionHeads()) {
        CFunctionEntryNode casted = (CFunctionEntryNode) node;

        Set<ASimpleDeclaration> qualifiedNames = casted.getFunctionParameters()
            .stream()
            .map(p -> p.asVariableDeclaration()).collect(Collectors.toSet());
        builder.put(node.getFunctionName(), qualifiedNames);
      }
    }
    functionParameters = builder.build();
  }

  /**
   * Get templates associated with the given node.
   */
  public Collection<Template> getTemplatesForNode(final CFANode node) {
    if (cache.containsKey(node)) {
      return cache.get(node);
    }

    Builder<Template> out = ImmutableSet.builder();
    out.addAll(extractTemplatesForNode(node)::iterator);
    out.addAll(extraTemplates);
    out.addAll(extractedFromAssertTemplates);

    out.addAll(generateTemplates(node));
    Set<Template> outBuild = out.build();

    if (varFiltering == VarFilteringStrategy.ONE_LIVE) {

      // Filter templates to make sure at least one is alive.
      outBuild = Sets.filter(outBuild, input -> shouldUseTemplate(input, node));
    }

    // Sort.
    List<Template> sortedTemplates =
        Ordering.from(
            Comparator.<Template>comparingInt(
                (template) -> template.getLinearExpression().size())
                .thenComparingInt(t -> t.toString().trim().startsWith("-") ? 1 : 0)
                .thenComparing(Template::toString))
                .immutableSortedCopy(outBuild);

    cache.putAll(node, sortedTemplates);
    return cache.get(node);
  }


  /**
   * Generate all linear expressions of size up to {@code maxExpressionSize}
   * with coefficients in {@code allowedCoefficients},
   * over the variables returned by {@link #getVarsForNode(CFANode)}.
   */
  private Set<Template> generateTemplates(final CFANode node) {

    Set<ASimpleDeclaration> varsForNode = getVarsForNode(node);
    Set<CIdExpression> vars = varsForNode.stream()
        .filter(this::shouldProcessVariable)
        .map(
            d -> new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) d)
        ).collect(Collectors.toSet());
    int maxLength = Math.min(maxExpressionSize, vars.size());
    allowedCoefficients = allowedCoefficients.stream().filter(
        x -> !x.equals(Rational.ZERO)).collect(Collectors.toSet());

    // Copy the {@code vars} multiple times for the cartesian product.
    List<Set<CIdExpression>> lists = Collections.nCopies(maxLength, vars);

    // All lists of size {@code maxExpressionSize}.
    Set<List<CIdExpression>> product = Sets.cartesianProduct(lists);

    // Eliminate duplicates, and ensure that all combinations are unique.
    // As a by-product, produces the expressions of all sizes less than
    // {@code maxExpressionSize} as well.
    Set<Set<CIdExpression>> combinations =
        product.stream().map(HashSet<CIdExpression>::new).collect(Collectors.toSet());

    Set<Template> returned = new HashSet<>();
    for (Set<CIdExpression> variables : combinations) {

      // For of every variable: instantiate with every coefficient.
      List<List<LinearExpression<CIdExpression>>> out =
          variables.stream().map(
              x -> allowedCoefficients.stream().map(
                  coeff -> LinearExpression.monomial(x, coeff)
              ).collect(Collectors.toList())
          ).collect(Collectors.toList());

      // Convert to a list of all possible linear expressions.
      List<LinearExpression<CIdExpression>> linearExpressions =
          Lists.cartesianProduct(out).stream().map(
              list -> list.stream().reduce(
                  LinearExpression.empty(), LinearExpression::add)
          ).collect(Collectors.toList());

      Set<Template> generated =
          filterToSameType(
              filterRedundantExpressions(linearExpressions)
          ).stream()
              .filter(t -> !t.isEmpty())
              .map(Template::of).collect(Collectors.toSet());
      returned.addAll(generated);
    }

    if (generateDifferences) {
      returned.addAll(generateDifferenceTemplates(vars));
    }

    return returned;
  }

  private Set<Template> generateDifferenceTemplates(Collection<CIdExpression> vars) {
    List<LinearExpression<CIdExpression>> out = new ArrayList<>();
    for (CIdExpression v1 : vars) {
      for (CIdExpression v2 : vars) {
        out.add(LinearExpression.ofVariable(v1).sub(LinearExpression.ofVariable(v2)));
      }
    }
    out = filterToSameType(out);
    return out.stream()
        .filter(t -> !t.isEmpty())
        .map(t -> Template.of(t))
        .collect(Collectors.toSet());
  }

  /**
   * Filter out the redundant expressions: that is, expressions already
   * contained in the list with a multiplier {@code >= 1}.
   */
  private List<LinearExpression<CIdExpression>> filterRedundantExpressions(
      List<LinearExpression<CIdExpression>> pLinearExpressions
  ) {
    Predicate<Optional<Rational>> existsAndMoreThanOne =
        (coeff -> coeff.isPresent() && coeff.get().compareTo(Rational.ONE) > 0);
    return pLinearExpressions.stream().filter(
            l -> !pLinearExpressions.stream().anyMatch(
                l2 -> l2 != l && existsAndMoreThanOne.test(l2.divide(l))
            )
        ).collect(Collectors.toList());
  }

  /**
   * Filter out the expressions where not all variables inside have the
   * same type.
   */
  private List<LinearExpression<CIdExpression>> filterToSameType(
      List<LinearExpression<CIdExpression>> pLinearExpressions
  ) {
    Function<CIdExpression, CBasicType> getType =
        x -> ((CSimpleType)x.getDeclaration().getType()).getType();
    return pLinearExpressions.stream().filter(
            expr -> expr.getMap().keySet().stream().allMatch(
                x -> getType.apply(x).equals(
                    getType.apply(expr.iterator().next().getKey())
                )
            )
        ).collect(Collectors.toList());
  }


  /**
   * Ignore temporary variables and pointers.
   */
  private boolean shouldProcessVariable(ASimpleDeclaration var) {
    return !var.getQualifiedName().contains(TMP_VARIABLE)
        && var.getType() instanceof CSimpleType
        && !var.getType().toString().contains("*");
  }

  /**
   * Generate templates from the calls to assert() functions.
   */
  private Set<Template> templatesFromAsserts() {
    Set<Template> templates = new HashSet<>();

    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        String statement = edge.getRawStatement();
        Optional<Template> template = Optional.empty();

        // todo: use the automaton instead to derive the error conditions,
        // do not hardcode the function names.
        if (statement.contains(ASSERT_H_FUNC_NAME)
            && edge instanceof CStatementEdge) {

          for (CFAEdge enteringEdge : CFAUtils.enteringEdges(node)) {
            if (enteringEdge instanceof CAssumeEdge) {
              CAssumeEdge assumeEdge = (CAssumeEdge) enteringEdge;
              CExpression expression = assumeEdge.getExpression();

              template = expressionToSingleTemplate(expression);
            }
          }

        } else if (statement.contains(ASSERT_FUNC_NAME) &&
            edge instanceof CFunctionCallEdge) {

          CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
          if (callEdge.getArguments().isEmpty()) {
            continue;
          }
          CExpression expression = callEdge.getArguments().get(0);
          template = expressionToSingleTemplate(expression);
        }

        if (template.isPresent()) {
          Template t = template.get();
          if (t.getLinearExpression().isEmpty()) {
            continue;
          }

          // Add template and its negation.
          templates.add(t);
          templates.add(Template.of(t.getLinearExpression().negate()));
        }

      }
    }
    return templates;
  }

  private Stream<Template> extractTemplatesForNode(CFANode node) {
    return extractedTemplates.stream().filter(t -> shouldUseTemplate(t, node));
  }

  private boolean shouldUseTemplate(Template t, CFANode node) {
    if (varFiltering == VarFilteringStrategy.ALL) {
      return true;
    }
    LiveVariables liveVariables = cfa.getLiveVariables().get();
    for (Entry<CIdExpression, Rational> e : t.getLinearExpression()) {
      CIdExpression id = e.getKey();
      if (varFiltering == VarFilteringStrategy.ONE_LIVE &&
          !liveVariables.isVariableLive(id.getDeclaration(), node)) {
        return true;
      }
      if (varFiltering == VarFilteringStrategy.ALL_LIVE
          && !liveVariables.isVariableLive(id.getDeclaration(), node)

          // Enforce inclusion of function parameters.
          && !functionParameters.getOrDefault(node.getFunctionName(), ImmutableSet.of())
            .contains(id.getDeclaration())) {
        return false;
      }
    }
    return true;
  }

  private Set<Template> extractTemplates() {
    Set<Template> out = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allEnteringEdges(node)) {
        out.addAll(
            extractTemplatesFromEdge(edge).stream().filter(t -> t.size() >= 1)
                .collect(Collectors.toSet()));
      }
    }
    return out;
  }

  private Set<Template> extractTemplatesFromEdge(CFAEdge edge) {
    Set<Template> out = new HashSet<>();
    switch (edge.getEdgeType()) {
      case ReturnStatementEdge:
        CReturnStatementEdge e = (CReturnStatementEdge) edge;
        if (e.getExpression().isPresent()) {
          out.addAll(expressionToTemplate(e.getExpression().get()));
        }
        break;
      case FunctionCallEdge:
        CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
        for (CExpression arg : callEdge.getArguments()) {
          out.addAll(expressionToTemplate(arg));
        }
        break;
      case AssumeEdge:
        CAssumeEdge assumeEdge = (CAssumeEdge) edge;
        out.addAll(expressionToTemplate(assumeEdge.getExpression()));
        break;
      case StatementEdge:
        out.addAll(extractTemplatesFromStatementEdge((CStatementEdge) edge));
        break;
      default:
        // nothing to do here
    }
    return out;
  }

  private Set<Template> extractTemplatesFromStatementEdge(CStatementEdge edge) {
    Set<Template> out = new HashSet<>();
    CStatement statement = edge.getStatement();
    if (statement instanceof CExpressionStatement) {
      out.addAll(expressionToTemplate(
          ((CExpressionStatement)statement).getExpression()));
    } else if (statement instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement assignment =
          (CExpressionAssignmentStatement)statement;
      CLeftHandSide lhs =
          assignment.getLeftHandSide();
      if (lhs instanceof CIdExpression) {
        CIdExpression id = (CIdExpression)lhs;
        out.addAll(expressionToTemplate(assignment.getRightHandSide()));
        if (!shouldProcessVariable(id.getDeclaration())) {
          return out;
        }

        Template tLhs = Template.of(LinearExpression.ofVariable(id));
        Optional<Template> x =
            expressionToSingleTemplate(assignment.getRightHandSide()).flatMap(
                t -> t.getLinearExpression().isEmpty() ? Optional.empty() : Optional.of(t)
            );
        if (x.isPresent()) {
          Template tX = x.get();
          out.add(Template.of(tLhs.getLinearExpression().sub(tX.getLinearExpression())));
        }
      }
    }
    return out;

  }

  private Set<Template> expressionToTemplate(CExpression expression) {
    HashSet<Template> out = new HashSet<>(2);
    Optional<Template> t = expressionToSingleTemplate(expression);
    if (!t.isPresent()) {
      return out;
    }
    out.add(t.get());
    out.add(Template.of(t.get().getLinearExpression().negate()));
    return out;
  }

  private Optional<Template> expressionToSingleTemplate(CExpression expression) {
    return recExpressionToTemplate(expression).flatMap(
        t -> t.getLinearExpression().isEmpty() ?
             Optional.empty() : Optional.of(t)
    );
  }

  private Optional<Template> recExpressionToTemplate(CExpression expression) {
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression)expression;
      CExpression operand1 = binaryExpression.getOperand1();
      CExpression operand2 = binaryExpression.getOperand2();

      BinaryOperator operator = binaryExpression.getOperator();
      Optional<Template> templateA = recExpressionToTemplate(operand1);
      Optional<Template> templateB = recExpressionToTemplate(operand2);

      // Special handling for constants and multiplication.
      if (operator == BinaryOperator.MULTIPLY
          && (templateA.isPresent() || templateB.isPresent())) {

        if (operand1 instanceof CIntegerLiteralExpression
            && templateB.isPresent()) {

          return Optional.of(useCoeff(
              (CIntegerLiteralExpression) operand1, templateB.get()
          ));
        } else if (operand2 instanceof CIntegerLiteralExpression
            && templateA.isPresent()) {

          return Optional.of(
              useCoeff((CIntegerLiteralExpression) operand2, templateA.get())
          );
        } else {
          return Optional.empty();
        }
      }

      // Otherwise just add/subtract templates.
      if (templateA.isPresent() && templateB.isPresent()
          && binaryExpression.getCalculationType() instanceof CSimpleType) {
        LinearExpression<CIdExpression> a = templateA.get().getLinearExpression();
        LinearExpression<CIdExpression> b = templateB.get().getLinearExpression();

        // Calculation type is the casting of both types to a suitable "upper"
        // type.
        Template t;
        if (operator == BinaryOperator.PLUS) {
          t = Template.of(a.add(b));
        } else {
          t = Template.of(a.sub(b));
        }
        return Optional.of(t);
      } else {
        return Optional.empty();
      }
    } else if (expression instanceof CLiteralExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      return Optional.of(Template.of(LinearExpression.empty()));
    } else if (expression instanceof CIdExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      CIdExpression idExpression = (CIdExpression)expression;
      return Optional.of(Template.of(LinearExpression.ofVariable(idExpression)));
    } else {
      return Optional.empty();
    }
  }

  private Template useCoeff(
      CIntegerLiteralExpression literal, Template other) {
    Rational coeff = Rational.ofBigInteger(literal.getValue());
    return Template.of(other.getLinearExpression().multByConst(coeff));
  }

  public void addGeneratedTemplates(Set<Template> templates) {
    generatedTemplates.addAll(templates);
  }

  public boolean adjustPrecision() {
    boolean changed = false;
    for (Template t : generatedTemplates) {
      changed |= addTemplateToExtra(t);
    }
    try {
      if (changed) {
        logger.log(Level.INFO,
            "Template Refinement: using templates generated with convex hull",
            generatedTemplates);
        generatedTemplates.clear();
        return true;
      }

      if (!generateFromStatements) {
        logger.log(Level.INFO, "Generating templates from all program statements.");
        generateFromStatements = true;
        extractedTemplates = ImmutableSet.copyOf(extractTemplates());
        return true;
      }

      if (maxExpressionSize == 1) {
        logger.log(Level.INFO, "Template Refinement: Generating octagons");
        maxExpressionSize = 2;
        return true;
      }
      if (maxExpressionSize == 2
          && !allowedCoefficients.contains(Rational.ofLong(2))) {
        logger.log(Level.INFO, "Template Refinement: increasing the "
            + "coefficient size to 2");
        allowedCoefficients = Sets.union(
            allowedCoefficients, ImmutableSet.of(
                Rational.ofLong(2), Rational.ofLong(-2)));
        return true;
      }
      if (maxExpressionSize == 2
          && allowedCoefficients.contains(Rational.ofLong(2))) {
        logger.log(Level.INFO, "Template Refinement: increasing the "
            + "expression size to 3");
        maxExpressionSize = 3;
        return true;
      }

      return false;
    } finally {
      cache.clear();
    }
  }

  private boolean addTemplateToExtra(Template t) {
    // Do not add intervals.
    if (t.size() == 1) {
      return false;
    }
    for (Entry<CIdExpression, Rational> e : t.getLinearExpression()) {

      // Do not add templates whose coefficients are already overflowing.
      if (templateToFormulaConversionManager.isOverflowing(t, e.getValue())) {
        return false;
      } else if (templateConstantThreshold != -1 &&
          e.getValue().compareTo(Rational.ofLong(templateConstantThreshold)) >= 1) {
        return false;
      }
    }

    return extraTemplates.add(t);
  }


  private Set<ASimpleDeclaration> getVarsForNode(CFANode node) {
    if (varFiltering == VarFilteringStrategy.ALL_LIVE) {
      return Sets.union(
          cfa.getLiveVariables().get().getLiveVariablesForNode(node).toSet(),
          functionParameters.getOrDefault(node.getFunctionName(), ImmutableSet.of())
      );
    } else {
      return allVariables;
    }
  }
}
