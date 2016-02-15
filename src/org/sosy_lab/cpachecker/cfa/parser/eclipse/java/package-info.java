/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
/**
 * A {@link org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser} implementation
 * based on the Eclipse JDT project.
 *
 * Important: this package may NOT contain public types,
 * and may not be accessed directly,
 * because we load it in a separate class loader.
 * Use {@link org.sosy_lab.cpachecker.cfa.parser.eclipse.EclipseParsers}
 * to get an instance.
 */
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;