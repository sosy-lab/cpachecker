// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;


@Options(prefix = "cpa.string")
public class StringOptions {

  @IntegerOption(min = 0)
  @Option(secure = true, name = "prefixlength", description = "Which prefixlength shall be tracked")
  private int prefixLength = 3;

  @IntegerOption(min = 0)
  @Option(secure = true, name = "suffixlength", description = "Which suffixlength shall be tracked")
  private int suffixLength = 3;

  @Option(description = "Look if these Strings are Part of a String", name = "containset")
  private ImmutableSet<String> containset = ImmutableSet.of();

  @Option(
    secure = true,
    name = "stringset",
    description = "Compare these Strings for StringSetDomain")
  private ImmutableSet<String> stringSet = ImmutableSet.of();

  @Option(
    secure = true,
    name = "domains",
    description = "which domains to use in StringCPA")
  private ImmutableList<String> domainList =
      ImmutableList
          .of(
                  "PrefixDomain",
                  "SuffixDomain",
                  "LengthDomain",
                  "CharSetDomain",
              "StringSetDomain");

  private ImmutableList<AbstractStringDomain<?>> domains;

  public StringOptions(Configuration config, ImmutableSet<String> pStringSet)
      throws InvalidConfigurationException {
    config.inject(this);
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
    builder.addAll(stringSet).addAll(pStringSet);
    stringSet = builder.build();
    domains = generateDomains(domainList);

  }

  private ImmutableList<AbstractStringDomain<?>>
      generateDomains(ImmutableList<String> pDomainList) throws InvalidConfigurationException {
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
        throw new InvalidConfigurationException(
            domainName
                + " is not an existing Domain or was  implemented incorrectly! Please refer to package-info in folder domain for more information.");
      }
    }
    return builder.build();
  }

  private String addPath(String domainName) {
    return "org.sosy_lab.cpachecker.cpa.string.domains." + domainName;
  }

  public int getPrefixLength() {
    return prefixLength;
  }

  public int getSuffixLength() {
    return suffixLength;
  }

  public ImmutableSet<String> getStringSet() {
    return stringSet;
  }

  public ImmutableSet<String> getContainset() {
    return containset;
  }

  public ImmutableList<String> getDomainList() {
    return domainList;
  }

  public boolean hasDomain(DomainType type) {
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
