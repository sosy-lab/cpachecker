// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*
 * Domains for Stringcpa
 * A domain represents aspects of strings. It is used to create an aspect from a string.
 */
/**
 * Instructions on how to add new domains: For each new Domain do the following steps: 1. The domain
 * needs to implement the class
 * {@link org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain} 2. The domain needs to
 * have a constructor, that takes just StringOptions as input. StringOptions needs to be given as
 * input, even it is not used! 3. If the domain needs additional Input, write that in StringOptions
 * 4. Add the Name of your Domain to {@link org.sosy_lab.cpachecker.cpa.string.domains.DomainType}
 * 5. Implement the methods from AbstractStringDomain
 */
package org.sosy_lab.cpachecker.cpa.string.domains;
