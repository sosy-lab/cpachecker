// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*
 * Domains for Stringcpa
 * A domain represents aspects that all share the
 */
/*
 * Instructions on how to add new domains:
 * For each Domain do the following:
 * 1. The domain needs to implement @AbstractStringDomain
 * 2. The domain needs to have a constructor, that takes StringOptions as input
 * 3. If the domain needs additional Input, write that in StringOptions
 * 4. Add the Name of your Domain to @DomainType
 * 5. Implement the methods from @AbstractStringDomain
 */
package org.sosy_lab.cpachecker.cpa.string.domains;
