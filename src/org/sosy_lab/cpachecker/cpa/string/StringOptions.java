// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;


@Options(prefix = "cpa.string")
public class StringOptions {

  @Option(secure = true, name = "prefixlength", description = "Which prefixlength shall be tracked")
  private int prefixLength = 3;

  @Option(secure = true, name = "suffixlength", description = "Which suffixlength shall be tracked")
  private int suffixLength = 3;

  @Option(description = "Look if these Strings are Part of a String", name = "containset")
  private List<String> containset = new ArrayList<>();

  @Option(
    secure = true,
    name = "stringset",
    description = "Compare these Strings for StringSetDomain")
  private ImmutableList<String> stringSet = ImmutableList.of();

  @Option(
    secure = true,
    name = "domains",
    description = "which domains to use in StringCPA")
  private ImmutableList<String> domainList =
      ImmutableList
          .copyOf(
              Arrays.asList(
                  "PrefixDomain",
                  "SuffixDomain",
                  "LengthDomain",
                  "CharSetDomain",
                  "ContainsDomain",
                  "StringSetDomain"));

  private ImmutableList<AbstractStringDomain<?>> domains;
  private LogManager logger;

  public StringOptions(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    domains = generateDomains(domainList);
  }

  private ImmutableList<AbstractStringDomain<?>>
      generateDomains(ImmutableList<String> pDomainList) {
    ImmutableList.Builder<AbstractStringDomain<?>> builder = new ImmutableList.Builder<>();

    for (String domainName : pDomainList) {
      try {
        Class<?> clazz = Class.forName(addPath(domainName));
        Constructor<?> constructor =
            clazz.getConstructor(StringOptions.class);
        AbstractStringDomain<?> instance = (AbstractStringDomain<?>) constructor.newInstance(this);
        builder.add(instance);
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException | SecurityException
          | ClassNotFoundException e) {
        logger.log(
            Level.FINE,
            domainName
                + " is not an existing Domain or was  implemented incorrectly! Please refer to package-info in folder domain for more information.");
      }
    }
    return builder.build();
  }

  private String addPath(String domainName) {
    return "org.sosy_lab.cpachecker.cpa.string.domains." + domainName;
  }

  /*
   * Add the string Literals in a program to the List of Strings in StringSetDomain
   */
  public void addStringToGivenSet(String str) {
    ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
    builder.addAll(stringSet).add(str);
    stringSet = builder.build();
  }
  public int getPrefixLength() {
    return prefixLength;
  }

  public int getSuffixLength() {
    return suffixLength;
  }

  public ImmutableList<String> getStringSet() {
    return stringSet;
  }

  public List<String> getContainset() {
    return containset;
  }

  public ImmutableList<String> getDomainList() {
    return domainList;
  }

  public boolean containsDomain(DomainType type) {
    for (AbstractStringDomain<?> domain : domains) {
      if (domain.getType().equals(type)) {
        return true;
      }
    }
    return false;
  }

  public AbstractStringDomain<?> getDomain(DomainType type) {
    for (AbstractStringDomain<?> domain : domains) {
      if (domain.getType().equals(type)) {
        return domain;
      }
    }
    return null;
  }
  public ImmutableList<AbstractStringDomain<?>> getDomains() {
    return domains;
  }

}
