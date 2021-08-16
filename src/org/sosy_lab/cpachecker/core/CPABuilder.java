// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.graph.Traverser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidComponentException;

/** Constructs a tree of CPA instances according to configuration. */
@Options
public class CPABuilder {

  private static final String CPA_OPTION_NAME = "cpa";
  private static final String CPA_CLASS_PREFIX = "org.sosy_lab.cpachecker";

  private static final Splitter LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  @Option(secure=true, name=CPA_OPTION_NAME,
      description="CPA to use (see doc/Configuration.md for more documentation on this)")
  private String cpaName = CompositeCPA.class.getCanonicalName();

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ReachedSetFactory reachedSetFactory;

  public CPABuilder(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      ReachedSetFactory pReachedSetFactory) throws InvalidConfigurationException {
    this.config = pConfig;
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;
    this.reachedSetFactory = pReachedSetFactory;
    config.inject(this);
  }

  /**
   * Build (tree of) CPA instances according to config. CPAs for the given specification automata
   * will be inserted in the appropriate place automatically.
   *
   * @return The root/main CPA.
   */
  public ConfigurableProgramAnalysis buildCPAs(
      final CFA cfa,
      final Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException {
    return buildCPAs(cfa, specification, ImmutableList.of(), pAggregatedReachedSets);
  }

  /**
   * Build (tree of) CPA instances according to config. CPAs for the given specification automata
   * and the additional automata will be inserted in the appropriate place automatically.
   *
   * @return The root/main CPA.
   */
  public ConfigurableProgramAnalysis buildCPAs(
      final CFA cfa,
      final Specification specification,
      final List<Automaton> additionalAutomata,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException {
    final FluentIterable<Automaton> allAutomata =
        FluentIterable.concat(specification.getSpecificationAutomata(), additionalAutomata);

    // 1. Parse config
    final CPAConfig rootCpaConfig = collectCPAConfigs(CPA_OPTION_NAME, cpaName);
    final FluentIterable<CPAConfig> allCpaConfigs =
        from(Traverser.forTree(CPAConfig::getAllChildren).depthFirstPreOrder(rootCpaConfig));

    // 2. Sanity checks

    if (rootCpaConfig.cpaClass == CompositeCPA.class
        && rootCpaConfig.getAllChildren().isEmpty()
        && allAutomata.isEmpty()) {
      // By default there is a top-level CompositeCPA, and if it has no children, this means that
      // the user did not specify any meaningful configuration.
      throw new InvalidConfigurationException(
          "Please specify a configuration with '-config CONFIG_FILE' or '-CONFIG' "
              + "(for example, '-default', '-predicateAnalysis', or '-valueAnalysis'). "
              + "See README.md for more details.");
    }

    checkAliasUniqueness(allCpaConfigs, allAutomata);

    // 3. Instantiate CPAs for automata

    List<ConfigurableProgramAnalysis> cpas = new ArrayList<>();
    for (Automaton automaton : allAutomata) {
      String cpaAlias = automaton.getName();

      CPAFactory factory = ControlAutomatonCPA.factory();
      factory.setConfiguration(Configuration.copyWithNewPrefix(config, cpaAlias));
      factory.setLogger(logger.withComponentName(cpaAlias));
      factory.set(cfa, CFA.class);
      factory.set(pAggregatedReachedSets, AggregatedReachedSets.class);
      factory.set(automaton, Automaton.class);
      factory.setShutdownNotifier(shutdownNotifier);

      cpas.add(factory.createInstance());
    }

    // 4. Instantiate configured CPAs

    ConfigurableProgramAnalysis cpa =
        instantiateCPAandChildren(rootCpaConfig, cpas, cfa, specification, pAggregatedReachedSets);
    if (!cpas.isEmpty()) {
      throw new InvalidConfigurationException(
          "Option specification gave specification automata, but no CompositeCPA was used");
    }
    return cpa;
  }

  /**
   * Analyze config and return appropriate {@link CPAConfig} instances that specify what should be
   * created.
   *
   * @param optionName The name of the current config option that is being analyzed (for messages)
   * @param optionValue The config value that should be analyzed. Must refer to a single CPA
   *     (possibly with an alias).
   * @return config for given optionValue
   */
  private CPAConfig collectCPAConfigs(final String optionName, final String optionValue)
      throws InvalidConfigurationException {
    List<String> optionParts = Splitter.onPattern("\\s+").splitToList(optionValue.trim());
    String cpaNameFromOption = optionParts.get(0);
    String cpaAlias = getCPAAlias(optionValue, optionName, optionParts, cpaNameFromOption);
    Class<?> cpaClass = getCPAClass(optionName, cpaNameFromOption);

    String childOptionName = cpaAlias + ".cpa";
    String childrenOptionName = cpaAlias + ".cpas";

    // Here we need to use these deprecated methods because we dynamically create the key.
    @SuppressWarnings("deprecation")
    String childCpaName = config.getProperty(childOptionName);
    @SuppressWarnings("deprecation")
    String childrenCpaNames = config.getProperty(childrenOptionName);

    CPAConfig child = null;
    ImmutableList.Builder<CPAConfig> childrenCpas = ImmutableList.builder();

    if (childCpaName != null) {
      // only one child CPA
      if (childrenCpaNames != null) {
        throw new InvalidConfigurationException(
            "Ambiguous configuration: both "
                + childOptionName
                + " and "
                + childrenOptionName
                + " are specified!");
      }

      child = collectCPAConfigs(childOptionName, childCpaName);

      logger.log(Level.FINER, "CPA", cpaAlias, "got child", childCpaName);

    } else if (childrenCpaNames != null) {
      // several children CPAs
      for (String currentChildCpaName : LIST_SPLITTER.split(childrenCpaNames)) {
        childrenCpas.add(collectCPAConfigs(childrenOptionName, currentChildCpaName));
      }
      logger.log(Level.FINER, "CPA", cpaAlias, "got children", childrenCpaNames);
    }

    return new CPAConfig(
        cpaNameFromOption, cpaAlias, cpaClass, child, childrenCpas.build());
  }

  /** Check that aliases for each CPA and each given automaton are unique. */
  private void checkAliasUniqueness(
      final FluentIterable<CPAConfig> cpas, final FluentIterable<Automaton> automata)
      throws InvalidConfigurationException {

    final ImmutableMultiset<String> automataAliases =
        automata.transform(Automaton::getName).toMultiset();
    final ImmutableMultiset<String> cpaAliases = cpas.transform(cpa -> cpa.alias).toMultiset();

    for (Multiset.Entry<String> entry : automataAliases.entrySet()) {
      if (entry.getCount() > 1) {
        throw new InvalidConfigurationException(
            "Alias " + entry.getElement() + " used twice for an automaton.");
      }
    }

    for (Multiset.Entry<String> entry : cpaAliases.entrySet()) {
      if (entry.getCount() > 1) {
        throw new InvalidConfigurationException(
            "Alias " + entry.getElement() + " used twice for a CPA.");
      }
    }

    final Multiset<String> aliasIntersection = Multisets.intersection(automataAliases, cpaAliases);
    if (!aliasIntersection.isEmpty()) {
      throw new InvalidConfigurationException(
          "The following aliases are used for both automata and CPAs: "
              + Joiner.on(", ").join(aliasIntersection.elementSet()));
    }
  }

  /**
   * Instantiate CPA(s) according to given config, including any necessary children.
   *
   * @param cpas Additional list of CPAs to inject at first possible place. Will be cleared
   *     afterwards.
   */
  private ConfigurableProgramAnalysis instantiateCPAandChildren(
      final CPAConfig cpaConfig,
      List<ConfigurableProgramAnalysis> cpas,
      final CFA cfa,
      final Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException {

    String cpaAlias = cpaConfig.alias;

    // shortcut for a ControlAutomatonCPA which was already instantiated, but is wrapped in a
    // cpa other than CompositeCPA (such as e.g. an AbstractSingleWrapperCPA)
    if (cpaAlias.equals(ControlAutomatonCPA.class.getSimpleName())) {
      Optional<ConfigurableProgramAnalysis> first =
          cpas.stream().filter(x -> x instanceof ControlAutomatonCPA).findFirst();
      if (first.isPresent()) {
        ConfigurableProgramAnalysis cpa = first.orElseThrow();
        cpas.remove(cpa);
        return cpa;
      }
    }

    // first get instance of appropriate factory

    Class<?> cpaClass = cpaConfig.cpaClass;

    logger.log(Level.FINER, "Instantiating CPA " + cpaClass.getName() + " with alias " + cpaAlias);

    CPAFactory factory = getFactoryInstance(cpaConfig.name, cpaClass);

    // now use factory to get an instance of the CPA

    factory.setConfiguration(Configuration.copyWithNewPrefix(config, cpaAlias));
    factory.setLogger(logger.withComponentName(cpaAlias));
    factory.setShutdownNotifier(shutdownNotifier);
    factory.set(pAggregatedReachedSets, AggregatedReachedSets.class);
    factory.set(specification, Specification.class);
    if (reachedSetFactory != null) {
      factory.set(reachedSetFactory, ReachedSetFactory.class);
    }
    if (cfa != null) {
      factory.set(cfa, CFA.class);
    }

    createAndSetChildrenCPAs(
        cpaConfig, factory, cpas, cfa, specification, pAggregatedReachedSets);

    // finally call createInstance
    ConfigurableProgramAnalysis cpa;
    try {
      cpa = factory.createInstance();
    } catch (IllegalStateException e) {
      throw new InvalidComponentException(cpaClass, "CPA", e);
    }
    if (cpa == null) {
      throw new InvalidComponentException(cpaClass, "CPA", "Factory returned null.");
    }
    logger.log(Level.FINER, "Sucessfully instantiated CPA " + cpa.getClass().getName() + " with alias " + cpaAlias);
    return cpa;
  }

  private String getCPAAlias(
      String optionValue, String optionName, List<String> optionParts, String pCpaName)
      throws InvalidConfigurationException {

    if (optionParts.size() == 1) {
      // no user-specified alias, use last part of class name
      int dotIndex = pCpaName.lastIndexOf('.');
      return (dotIndex >= 0 ? pCpaName.substring(dotIndex + 1) : pCpaName);

    } else if (optionParts.size() == 2) {
      return optionParts.get(1);

    } else {
      throw new InvalidConfigurationException("Option " + optionName + " contains invalid CPA specification \"" + optionValue + "\"!");
    }
  }

  private Class<?> getCPAClass(String optionName, String pCpaName)
      throws InvalidConfigurationException {
    Class<?> cpaClass;
    try {
      cpaClass = Classes.forName(pCpaName, CPA_CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException(
          "Option " + optionName + " is set to unknown CPA " + pCpaName, e);
    }

    if (!ConfigurableProgramAnalysis.class.isAssignableFrom(cpaClass)) {
      throw new InvalidConfigurationException(
        "Option " + optionName + " has to be set to a class implementing the ConfigurableProgramAnalysis interface!");
    }

    Classes.produceClassLoadingWarning(logger, cpaClass, ConfigurableProgramAnalysis.class);

    return cpaClass;
  }

  private CPAFactory getFactoryInstance(String pCpaName, Class<?> cpaClass) throws CPAException {

    // get factory method
    Method factoryMethod;
    try {
      factoryMethod = cpaClass.getMethod("factory", (Class<?>[]) null);
    } catch (NoSuchMethodException e) {
      throw new InvalidComponentException(cpaClass, "CPA", "No public static method \"factory\" with zero parameters.");
    }

    // verify signature
    if (!Modifier.isStatic(factoryMethod.getModifiers())) {
      throw new InvalidComponentException(cpaClass, "CPA", "Factory method is not static.");
    }

    String exception = Classes.verifyDeclaredExceptions(factoryMethod, CPAException.class);
    if (exception != null) {
      throw new InvalidComponentException(cpaClass, "CPA", "Factory method declares the unsupported checked exception " + exception + " .");
    }

    // invoke factory method
    Object factoryObj;
    try {
      factoryObj = factoryMethod.invoke(null, (Object[])null);

    } catch (IllegalAccessException e) {
      throw new InvalidComponentException(cpaClass, "CPA", "Factory method is not public.");

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, CPAException.class);

      throw new UnexpectedCheckedException("instantiation of CPA " + pCpaName, cause);
    }

    if (!(factoryObj instanceof CPAFactory)) {
      throw new InvalidComponentException(cpaClass, "CPA", "Factory method did not return a CPAFactory instance.");
    }

    return (CPAFactory)factoryObj;
  }

  private void createAndSetChildrenCPAs(
      final CPAConfig cpaConfig,
      CPAFactory factory,
      List<ConfigurableProgramAnalysis> cpas,
      final CFA cfa,
      final Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException {

    ImmutableList<CPAConfig> children = cpaConfig.children;
    if (children.isEmpty()
        && cpaConfig.child == null
        && cpaConfig.alias.equals("CompositeCPA")
        && !cpas.isEmpty()) {
      // if a specification was given, but no CPAs, insert a LocationCPA
      children = ImmutableList.of(CPAConfig.forClass(LocationCPA.class));
    }

    if (cpaConfig.child != null) {
      // only one child CPA
      ConfigurableProgramAnalysis child =
          instantiateCPAandChildren(
              cpaConfig.child,
              cpas,
              cfa,
              specification,
              pAggregatedReachedSets);
      try {
        factory.setChild(child);
      } catch (UnsupportedOperationException e) {
        throw new InvalidConfigurationException(
            cpaConfig.name + " is no wrapper CPA, but was configured to have a child CPA!",
            e);
      }

    } else if (!children.isEmpty()) {
      // several children CPAs
      ImmutableList.Builder<ConfigurableProgramAnalysis> childrenCpas = ImmutableList.builder();

      for (CPAConfig currentChildCpaConfig : children) {
        childrenCpas.add(
            instantiateCPAandChildren(
                currentChildCpaConfig, cpas, cfa, specification, pAggregatedReachedSets));
      }
      if (cpas != null) {
        childrenCpas.addAll(cpas);
        cpas.clear();
      }

      try {
        factory.setChildren(childrenCpas.build());
      } catch (UnsupportedOperationException e) {
        throw new InvalidConfigurationException(
            cpaConfig.name
                + " is no wrapper CPA, but was configured to have children CPAs!",
            e);
      }
    }
  }

  /** Represents the configuration for one CPA instance. */
  private static class CPAConfig {
    /**
     * The CPA name as given by the user (possibly abbreviated class name, e.g.,
     * cpa.location.LocationCPA)
     */
    final String name;
    /** The alias for this CPA instance as given by the user or inferred */
    final String alias;
    /** The class of this CPA. */
    final Class<?> cpaClass;
    /** Config for child CPA if the "alias.cpa" option was given. */
    final @Nullable CPAConfig child;
    /** Config for children CPA if the "alias.cpas" option was given. */
    final ImmutableList<CPAConfig> children;

    CPAConfig(
        String pName,
        String pAlias,
        Class<?> pCpaClass,
        @Nullable CPAConfig pChild,
        ImmutableList<CPAConfig> pChildren) {
      name = checkNotNull(pName);
      alias = checkNotNull(pAlias);
      cpaClass = checkNotNull(pCpaClass);
      child = pChild;
      children = checkNotNull(pChildren);
      checkArgument(child == null || pChildren.isEmpty());
    }

    static CPAConfig forClass(Class<? extends ConfigurableProgramAnalysis> cpaClass) {
      return new CPAConfig(
          cpaClass.getCanonicalName(),
          cpaClass.getSimpleName(),
          cpaClass,
          null,
          ImmutableList.of());
    }

    /** Return configured children, no matter if "alias.cpa" or "alias.cpas" was given. */
    ImmutableList<CPAConfig> getAllChildren() {
      return child != null ? ImmutableList.of(child) : children;
    }
  }
}
