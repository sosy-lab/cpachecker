/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.test;

import com.google.common.collect.ImmutableList;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This is a class loader that keeps a reference to
 * all classes that have been loaded through it.
 */
@SuppressWarnings("deprecation") // TODO: Do this without ChildFirstPatternClassLoader?
public class LoggingClassLoader extends org.sosy_lab.common.ChildFirstPatternClassLoader {

  private final List<Class<?>> loadedClasses = new ArrayList<>();

  /**
   * Create a new class loader.
   * @param pChildFirstPattern The pattern telling which classes should never be loaded by the parent.
   * @param pUrls The sources where this class loader should load classes from.
   * @param pParent The parent class loader.
   */
  public LoggingClassLoader(
      Pattern pChildFirstPattern, URL[] pUrls, ClassLoader pParent) {
    super(pChildFirstPattern, pUrls, pParent);
  }

  @Override
  public Class<?> loadClass(String pName) throws ClassNotFoundException {
    Class<?> cls = super.loadClass(pName);
    loadedClasses.add(cls);
    return cls;
  }

  public ImmutableList<Class<?>> getLoadedClasses() {
    return ImmutableList.copyOf(loadedClasses);
  }
}
