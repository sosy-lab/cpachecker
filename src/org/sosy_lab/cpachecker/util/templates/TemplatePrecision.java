// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.templates;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Equivalence;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.sosy_lab.common.collect.Collections3;
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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LiveVariables;

/** Precision object for template-based analysis. */
@Options(prefix = "precision.template", deprecatedPrefix = "cpa.lpi")
public class TemplatePrecision implements Precision {

  private static final Comparator<Template> TEMPLATE_COMPARATOR =
      Comparator.<Template>comparingInt((template) -> template.getLinearExpression().size())
          .thenComparingInt(t -> t.toString().trim().startsWith("-") ? 1 : 0)
          .thenComparing(Template::toString);

  @Option(secure = true, description = "Generate templates from assert statements")
  private boolean generateFromAsserts = true;

  @Option(secure = true, description = "Generate templates from all program " + "statements")
  private boolean generateFromStatements = false;

  @Option(secure = true, description = "Maximum size for the generated template")
  private int maxExpressionSize = 1;

  @Option(secure = true, description = "Perform refinement using enumerative template synthesis.")
  private boolean performEnumerativeRefinement = true;

  @Option(
      secure = true,
      description =
          "Generate difference constraints."
              + "This option is redundant for `maxExpressionSize` >= 2.")
  private boolean generateDifferences = false;

  @Option(secure = true, description = "Allowed coefficients in a template.")
  private ImmutableSet<Rational> allowedCoefficients =
      ImmutableSet.of(Rational.NEG_ONE, Rational.ONE);

  @Option(
      secure = true,
      description = "Strategy for filtering variables out of templates using " + "liveness")
  private VarFilteringStrategy varFiltering = VarFilteringStrategy.ALL_LIVE;

  @Option(
      secure = true,
      description =
          "Do not generate templates with threshold larger than specified."
              + " Set to '-1' for no limit.")
  private long templateConstantThreshold = 100;

  @Option(
      secure = true,
      description =
          "Force the inclusion of function parameters into the "
              + "generated templates. Required for summaries computation.")
  private boolean includeFunctionParameters = false;

  public enum VarFilteringStrategy {

    /** Generate only templates from variables contained in the created interpolants. */
    INTERPOLATION_BASED,

    /**
     * Generate only templates where all variables are alive. Complete for integers and octagons.
     * Can be extended to more complicated cases using projection.
     */
    ALL_LIVE,

    /** Generate only templates where at least one variable is alive. */
    ONE_LIVE,

    /** Generate all templates. */
    ALL
  }

  private final CFA cfa;
  private final LogManager logger;

  private final ImmutableSet<Template> extractedFromAssertTemplates;
  private ImmutableSet<Template> extractedTemplates;
  private final Set<Template> extraTemplates;
  private final Set<Template> generatedTemplates;

  /** Variables contained in the interpolant. */
  private final Multimap<CFANode, ASimpleDeclaration> varsInInterpolant;

  private final TemplateToFormulaConversionManager templateToFormulaConversionManager;

  // Temporary variables created by CPAchecker.
  private static final String TMP_VARIABLE = "__CPAchecker_TMP";

  // todo: do not hardcode, use automaton.
  private static final String ASSERT_FUNC_NAME = "assert";
  private static final String ASSERT_H_FUNC_NAME = "__assert_fail";

  /** Cache of generated templates. */
  private final Map<CFANode, ImmutableList<Template>> cache = new HashMap<>();

  private final ImmutableSet<ASimpleDeclaration> allVariables;

  /**
   * Mapping from function name to a set of function parameters. Variables represented parameters
   * should be kept in precision at the return node in order to compute summaries.
   */
  private final ImmutableSetMultimap<String, ASimpleDeclaration> functionParameters;

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

    allowedCoefficients = from(allowedCoefficients).filter(c -> !c.equals(Rational.ZERO)).toSet();

    if (generateFromAsserts) {
      extractedFromAssertTemplates = ImmutableSet.copyOf(templatesFromAsserts());
    } else {
      extractedFromAssertTemplates = ImmutableSet.of();
    }
    logger.log(Level.FINE, "Generated from assert templates", extractedFromAssertTemplates);

    if (generateFromStatements) {
      extractedTemplates = extractTemplates();
    } else {
      extractedTemplates = ImmutableSet.of();
    }
    logger.log(Level.FINE, "Generated templates", extractedFromAssertTemplates);
    generatedTemplates = new HashSet<>();

    allVariables = ImmutableSet.copyOf(cfa.getLiveVariables().orElseThrow().getAllLiveVariables());

    ImmutableSetMultimap.Builder<String, ASimpleDeclaration> builder =
        ImmutableSetMultimap.builder();
    if (includeFunctionParameters) {
      for (FunctionEntryNode node : cfa.getAllFunctionHeads()) {
        CFunctionEntryNode casted = (CFunctionEntryNode) node;

        casted.getFunctionParameters().stream()
            .map(p -> p.asVariableDeclaration())
            .forEach(qualifiedName -> builder.put(node.getFunctionName(), qualifiedName));
      }
    }
    functionParameters = builder.build();
    varsInInterpolant = HashMultimap.create();
  }

  /** Get templates associated with the given node. */
  public ImmutableList<Template> getTemplatesForNode(final CFANode node) {
    if (cache.containsKey(node)) {
      return cache.get(node);
    }

    ImmutableSet<CIdExpression> vars =
        getVarsForNode(node).stream()
            .filter(this::shouldProcessVariable)
            .map(d -> new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) d))
            .collect(toImmutableSet());

    Stream<Template> out =
        Streams.concat(
            extractTemplatesForNode(node),
            extraTemplates.stream(),
            extractedFromAssertTemplates.stream(),
            generateTemplates(vars));

    if (generateDifferences) {
      out = Stream.concat(out, generateDifferenceTemplates(vars));
    }

    if (varFiltering == VarFilteringStrategy.ONE_LIVE) {
      // Filter templates to make sure at least one is alive.
      out = out.filter(input -> shouldUseTemplate(input, node));
    }

    // Sort.
    ImmutableList<Template> sortedTemplates =
        out.unordered()
            .distinct()
            .sorted(TEMPLATE_COMPARATOR)
            .collect(ImmutableList.toImmutableList());

    cache.put(node, sortedTemplates);
    return sortedTemplates;
  }

  /**
   * Generate all linear expressions of size up to {@code maxExpressionSize} with coefficients in
   * {@code allowedCoefficients}, over the given variables.
   */
  private Stream<Template> generateTemplates(final ImmutableSet<CIdExpression> vars) {
    int maxLength = Math.min(maxExpressionSize, vars.size());
    assert !allowedCoefficients.contains(Rational.ZERO);

    // Copy the {@code vars} multiple times for the cartesian product.
    List<Set<CIdExpression>> lists = Collections.nCopies(maxLength, vars);

    // All lists of size {@code maxExpressionSize}.
    Set<List<CIdExpression>> product = Sets.cartesianProduct(lists);

    // As a by-product, produces the expressions of all sizes less than
    // {@code maxExpressionSize} as well.
    return product.stream()
        .map(HashSet::new)
        .distinct() // Eliminate duplicates, and ensure that all combinations are unique.
        .flatMap(
            variables -> {
              // For of every variable: instantiate with every coefficient.
              List<List<LinearExpression<CIdExpression>>> out =
                  Collections3.transformedImmutableListCopy(
                      variables,
                      x ->
                          Collections3.transformedImmutableListCopy(
                              allowedCoefficients, coeff -> LinearExpression.monomial(x, coeff)));

              // Convert to a list of all possible linear expressions.
              Stream<LinearExpression<CIdExpression>> linearExpressions =
                  Lists.cartesianProduct(out).stream()
                      .map(l -> l.stream().reduce(LinearExpression.empty(), LinearExpression::add));

              return filterRedundantExpressions(linearExpressions)
                  .filter(this::hasSameType)
                  .filter(t -> !t.isEmpty())
                  .map(Template::of);
            });
  }

  private Stream<Template> generateDifferenceTemplates(Collection<CIdExpression> vars) {
    Collection<LinearExpression<CIdExpression>> varExpression =
        Collections2.transform(vars, LinearExpression::ofVariable);
    return varExpression.stream()
        .flatMap(v1 -> varExpression.stream().map(v2 -> v1.sub(v2)))
        .filter(this::hasSameType)
        .filter(t -> !t.isEmpty())
        .map(Template::of);
  }

  /**
   * Filter out the redundant expressions: that is, expressions already contained in the list with a
   * multiplier {@code >= 1}.
   */
  private Stream<LinearExpression<CIdExpression>> filterRedundantExpressions(
      Stream<LinearExpression<CIdExpression>> pLinearExpressions) {
    Predicate<Optional<Rational>> existsAndMoreThanOne =
        coeff -> coeff.isPresent() && coeff.orElseThrow().compareTo(Rational.ONE) > 0;
    Set<LinearExpression<CIdExpression>> linearExpressions =
        pLinearExpressions.collect(toImmutableSet());
    return linearExpressions.stream()
        .filter(
            l ->
                linearExpressions.stream()
                    .noneMatch(l2 -> l2 != l && existsAndMoreThanOne.test(l2.divide(l))));
  }

  private static final Equivalence<CIdExpression> BASIC_TYPE_EQUIVALENCE =
      Equivalence.equals().onResultOf(x -> ((CSimpleType) x.getDeclaration().getType()).getType());

  /** Check whether all variables inside a expression have the same type. */
  private boolean hasSameType(LinearExpression<CIdExpression> expr) {
    return expr.getMap().keySet().stream()
        .allMatch(x -> BASIC_TYPE_EQUIVALENCE.equivalent(x, expr.iterator().next().getKey()));
  }

  /** Ignore temporary variables and pointers. */
  private boolean shouldProcessVariable(ASimpleDeclaration var) {
    return !var.getQualifiedName().contains(TMP_VARIABLE)
        && var.getType() instanceof CSimpleType
        && !var.getType().toString().contains("*");
  }

  /** Generate templates from the calls to assert() functions. */
  private Set<Template> templatesFromAsserts() {
    Set<Template> templates = new HashSet<>();

    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        String statement = edge.getRawStatement();
        Optional<LinearExpression<CIdExpression>> template = Optional.empty();

        // todo: use the automaton instead to derive the error conditions,
        // do not hardcode the function names.
        if (statement.contains(ASSERT_H_FUNC_NAME) && edge instanceof CStatementEdge) {

          for (CFAEdge enteringEdge : CFAUtils.enteringEdges(node)) {
            if (enteringEdge instanceof CAssumeEdge) {
              CAssumeEdge assumeEdge = (CAssumeEdge) enteringEdge;
              CExpression expression = assumeEdge.getExpression();

              template = expressionToSingleTemplate(expression);
            }
          }

        } else if (statement.contains(ASSERT_FUNC_NAME) && edge instanceof CFunctionCallEdge) {

          CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
          if (callEdge.getArguments().isEmpty()) {
            continue;
          }
          CExpression expression = callEdge.getArguments().get(0);
          template = expressionToSingleTemplate(expression);
        }

        // Add template and its negation.
        template
            .filter(t -> !t.isEmpty())
            .ifPresent(
                t -> {
                  templates.add(Template.of(t));
                  templates.add(Template.of(t.negate()));
                });
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
    LiveVariables liveVariables = cfa.getLiveVariables().orElseThrow();
    for (Entry<CIdExpression, Rational> e : t.getLinearExpression()) {
      CIdExpression id = e.getKey();
      if (varFiltering == VarFilteringStrategy.ONE_LIVE
          && !liveVariables.isVariableLive(id.getDeclaration(), node)) {
        return true;
      }
      if (varFiltering == VarFilteringStrategy.ALL_LIVE
          && !liveVariables.isVariableLive(id.getDeclaration(), node)

          // Enforce inclusion of function parameters.
          && !functionParameters.containsEntry(node.getFunctionName(), id.getDeclaration())) {
        return false;
      }
    }
    return true;
  }

  private ImmutableSet<Template> extractTemplates() {
    return cfa.getAllNodes().stream()
        .flatMap(node -> CFAUtils.allEnteringEdges(node).stream())
        .flatMap(edge -> extractTemplatesFromEdge(edge).stream())
        .filter(t -> t.size() >= 1)
        .map(Template::of)
        .collect(toImmutableSet());
  }

  private Collection<LinearExpression<CIdExpression>> extractTemplatesFromEdge(CFAEdge edge) {
    switch (edge.getEdgeType()) {
      case ReturnStatementEdge:
        CReturnStatementEdge e = (CReturnStatementEdge) edge;
        if (e.getExpression().isPresent()) {
          return expressionToTemplate(e.getExpression().orElseThrow());
        }
        break;
      case FunctionCallEdge:
        CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
        return from(callEdge.getArguments()).transformAndConcat(this::expressionToTemplate).toSet();
      case AssumeEdge:
        CAssumeEdge assumeEdge = (CAssumeEdge) edge;
        return expressionToTemplate(assumeEdge.getExpression());
      case StatementEdge:
        return extractTemplatesFromStatementEdge((CStatementEdge) edge);
      default:
        // nothing to do here
    }
    return ImmutableSet.of();
  }

  private Collection<LinearExpression<CIdExpression>> extractTemplatesFromStatementEdge(
      CStatementEdge edge) {
    CStatement statement = edge.getStatement();
    if (statement instanceof CExpressionStatement) {
      return expressionToTemplate(((CExpressionStatement) statement).getExpression());
    } else if (statement instanceof CExpressionAssignmentStatement) {
      Set<LinearExpression<CIdExpression>> out = new HashSet<>();
      CExpressionAssignmentStatement assignment = (CExpressionAssignmentStatement) statement;
      CLeftHandSide lhs = assignment.getLeftHandSide();
      if (lhs instanceof CIdExpression) {
        CIdExpression id = (CIdExpression) lhs;
        out.addAll(expressionToTemplate(assignment.getRightHandSide()));
        if (!shouldProcessVariable(id.getDeclaration())) {
          return out;
        }

        expressionToSingleTemplate(assignment.getRightHandSide())
            .ifPresent(t -> out.add(LinearExpression.ofVariable(id).sub(t)));
      }
      return out;
    }
    return ImmutableList.of();
  }

  private Collection<LinearExpression<CIdExpression>> expressionToTemplate(CExpression expression) {
    Optional<LinearExpression<CIdExpression>> t = expressionToSingleTemplate(expression);
    return t.isPresent()
        ? ImmutableList.of(t.orElseThrow(), t.orElseThrow().negate())
        : ImmutableList.of();
  }

  private Optional<LinearExpression<CIdExpression>> expressionToSingleTemplate(
      CExpression expression) {
    return recExpressionToTemplate(expression).filter(t -> !t.isEmpty());
  }

  private Optional<LinearExpression<CIdExpression>> recExpressionToTemplate(
      CExpression expression) {
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) expression;
      CExpression operand1 = binaryExpression.getOperand1();
      CExpression operand2 = binaryExpression.getOperand2();

      BinaryOperator operator = binaryExpression.getOperator();
      Optional<LinearExpression<CIdExpression>> templateA = recExpressionToTemplate(operand1);
      Optional<LinearExpression<CIdExpression>> templateB = recExpressionToTemplate(operand2);

      // Special handling for constants and multiplication.
      if (operator == BinaryOperator.MULTIPLY && (templateA.isPresent() || templateB.isPresent())) {

        if (operand1 instanceof CIntegerLiteralExpression && templateB.isPresent()) {

          return Optional.of(
              useCoeff((CIntegerLiteralExpression) operand1, templateB.orElseThrow()));
        } else if (operand2 instanceof CIntegerLiteralExpression && templateA.isPresent()) {

          return Optional.of(
              useCoeff((CIntegerLiteralExpression) operand2, templateA.orElseThrow()));
        } else {
          return Optional.empty();
        }
      }

      // Otherwise just add/subtract templates.
      if (templateA.isPresent()
          && templateB.isPresent()
          && binaryExpression.getCalculationType() instanceof CSimpleType) {
        LinearExpression<CIdExpression> a = templateA.orElseThrow();
        LinearExpression<CIdExpression> b = templateB.orElseThrow();

        // Calculation type is the casting of both types to a suitable "upper"
        // type.
        LinearExpression<CIdExpression> t;
        if (operator == BinaryOperator.PLUS) {
          t = a.add(b);
        } else {
          t = a.sub(b);
        }
        return Optional.of(t);
      } else {
        return Optional.empty();
      }
    } else if (expression instanceof CLiteralExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      return Optional.of(LinearExpression.empty());
    } else if (expression instanceof CIdExpression
        && expression.getExpressionType() instanceof CSimpleType) {
      CIdExpression idExpression = (CIdExpression) expression;
      return Optional.of(LinearExpression.ofVariable(idExpression));
    } else {
      return Optional.empty();
    }
  }

  private LinearExpression<CIdExpression> useCoeff(
      CIntegerLiteralExpression literal, LinearExpression<CIdExpression> other) {
    Rational coeff = Rational.ofBigInteger(literal.getValue());
    return other.multByConst(coeff);
  }

  public void addGeneratedTemplates(Set<Template> templates) {
    generatedTemplates.addAll(templates);
  }

  public boolean injectPrecisionFromInterpolant(CFANode pNode, Set<String> usedVars) {
    LiveVariables liveVars = cfa.getLiveVariables().orElseThrow();

    Map<String, ASimpleDeclaration> map =
        Maps.uniqueIndex(liveVars.getAllLiveVariables(), ASimpleDeclaration::getQualifiedName);

    List<ASimpleDeclaration> out =
        usedVars.stream()
            .filter(v -> map.containsKey(v))
            .map(v -> map.get(v))
            .collect(ImmutableList.toImmutableList());

    boolean returned = varsInInterpolant.putAll(pNode, out);
    logger.log(Level.FINE, "Generated vars", out);
    logger.log(Level.FINE, "Got input", usedVars);
    if (returned) {
      // Invalidate the cache.
      cache.remove(pNode);
    }
    return returned;
  }

  /**
   * Generate a new set of templates for each location subject to a higher precision. Invalidates
   * the cache of already generated templates.
   *
   * @return Whether the number of templates was changed.
   */
  public boolean adjustPrecision() {
    boolean changed = false;
    for (Template t : generatedTemplates) {
      changed |= addTemplateToExtra(t);
    }
    try {
      if (changed) {
        logger.log(
            Level.INFO,
            "Template Refinement: using templates generated with convex hull",
            generatedTemplates);
        generatedTemplates.clear();
        return true;
      }

      if (!generateFromStatements) {
        logger.log(
            Level.INFO,
            "Template Refinement: Generating templates from all program " + "statements.");
        generateFromStatements = true;
        extractedTemplates = extractTemplates();
        return true;
      }

      if (!performEnumerativeRefinement) {
        return false;
      }

      if (maxExpressionSize == 1) {
        logger.log(Level.INFO, "Template Refinement: Generating octagons");
        maxExpressionSize = 2;
        return true;
      }
      if (maxExpressionSize == 2 && !allowedCoefficients.contains(Rational.ofLong(2))) {
        logger.log(Level.INFO, "Template Refinement: increasing the " + "coefficient size to 2");
        allowedCoefficients =
            from(allowedCoefficients).append(Rational.ofLong(2), Rational.ofLong(-2)).toSet();
        return true;
      }
      if (maxExpressionSize == 2 && allowedCoefficients.contains(Rational.ofLong(2))) {
        logger.log(Level.INFO, "Template Refinement: increasing the " + "expression size to 3");
        maxExpressionSize = 3;
        return true;
      }

      return false;
    } finally {
      cache.clear();
    }
  }

  /**
   * Add template {@code t} to a set {@code extraTemplates}. Ignore the template if it is not valid.
   *
   * @return Whether the template was added.
   */
  private boolean addTemplateToExtra(Template t) {
    return shouldAddTemplate(t) && extraTemplates.add(t);
  }

  private boolean shouldAddTemplate(Template t) {
    // Do not add intervals.
    if (t.size() == 1) {
      return false;
    }
    for (Entry<CIdExpression, Rational> e : t.getLinearExpression()) {

      // Do not add templates whose coefficients are already overflowing.
      if (templateToFormulaConversionManager.isOverflowing(t, e.getValue())) {
        return false;
      } else if (templateConstantThreshold != -1
          && e.getValue().abs().compareTo(Rational.ofLong(templateConstantThreshold)) > 0) {
        return false;
      }
    }
    return true;
  }

  private Collection<ASimpleDeclaration> getVarsForNode(CFANode node) {
    if (varFiltering == VarFilteringStrategy.ALL_LIVE) {
      return Sets.union(
          cfa.getLiveVariables().orElseThrow().getLiveVariablesForNode(node),
          functionParameters.get(node.getFunctionName()));
    } else if (varFiltering == VarFilteringStrategy.INTERPOLATION_BASED) {
      return varsInInterpolant.get(node);
    } else {
      return allVariables;
    }
  }
}
