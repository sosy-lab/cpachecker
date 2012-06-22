/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cmdline;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;

public class CPASelfCheck {

  private static LogManager logManager;
  private static Configuration config;

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    config = Configuration.defaultConfiguration();
    logManager = new LogManager(config);

    CFA cfa = createCFA();
    FunctionEntryNode main = cfa.getMainFunction();

    logManager.log(Level.INFO, "Searching for CPAs");
    List<Class<ConfigurableProgramAnalysis>> cpas = getCPAs();

    for (Class<ConfigurableProgramAnalysis> cpa : cpas) {
      logManager.log(Level.INFO, "Checking " + cpa.getCanonicalName() + " ...");
      ConfigurableProgramAnalysis cpaInst;
      try {
        cpaInst = tryToInstantiate(cpa, cfa);
      } catch (InvocationTargetException e) {
        logManager.logException(Level.WARNING, e,
            "Getting factory instance for " + cpa.getCanonicalName() + " failed!");
        continue;
      } catch (NoSuchMethodException e) {
        logManager.logException(Level.WARNING, e,
            "Getting factory instance for " + cpa.getCanonicalName() +
            " failed: no factory method!");
        continue;
      } catch (Exception e) {
        logManager.logException(Level.WARNING, e, "Could not instantiate " + cpa.getCanonicalName());
        continue;
      } catch (UnsatisfiedLinkError e) {
        logManager.logException(Level.WARNING, e, "Could not instantiate " + cpa.getCanonicalName());
        continue;
      }
      assert cpaInst != null;

      try {
        cpaInst.getInitialState(main);

        boolean ok = true;
        // check domain and lattice
        ok &= checkJoin(cpa, cpaInst, main);
        /// TODO checking the invariantes of the transfer relation is a bit more work ...
        // check merge
        ok &= checkMergeSoundness(cpa, cpaInst, main);
        // check stop
        ok &= checkStopEmptyReached(cpa, cpaInst, main);
        ok &= checkStopReached(cpa, cpaInst, main);
        /// TODO check invariants of precision adjustment
        logManager.log(Level.INFO, ok ? " OK" : " ERROR");
      } catch (Exception e) {
        logManager.logException(Level.WARNING, e, "");
      }
    }
  }

  private static CFA createCFA() throws IOException, ParserException {
    String code = "int main() {\n"
                + "  int a;\n"
                + "  a = 1;\n"
                + "  return (a);\n"
                + "}\n";

    CParser parser = CParser.Factory.getParser(logManager, CParser.Factory.getDefaultOptions());
    ParseResult cfas = parser.parseString(code);
    MutableCFA cfa = new MutableCFA(cfas.getFunctions(), cfas.getCFANodes(), cfas.getFunctions().get("main"));
    return cfa.makeImmutableCFA(Optional.<ImmutableMultimap<String,Loop>>absent());
  }

  private static ConfigurableProgramAnalysis tryToInstantiate(Class<ConfigurableProgramAnalysis> pCpa,
      CFA cfa) throws NoSuchMethodException, InvocationTargetException, InvalidConfigurationException, CPAException, IllegalAccessException {
    Method factoryMethod = pCpa.getMethod("factory", new Class<?>[0]);

    CPAFactory factory = (CPAFactory)factoryMethod.invoke(null, new Object[0]);
    return factory.setLogger(logManager)
                  .setConfiguration(config)
                  .set(cfa, CFA.class)
                  .createInstance();
  }

  private static boolean ensure(boolean pB, String pString) {
    if (!pB) {
      logManager.log(Level.WARNING, pString);
      return false;
    }
    return true;
  }

  private static boolean checkJoin(Class<ConfigurableProgramAnalysis> pCpa,
                                ConfigurableProgramAnalysis pCpaInst, FunctionEntryNode pMain) throws CPAException {
    AbstractDomain d = pCpaInst.getAbstractDomain();
    AbstractState initial = pCpaInst.getInitialState(pMain);

    return ensure(d.isLessOrEqual(initial, d.join(initial,initial)),
        "Join of same elements is unsound!");
  }

  private static boolean checkMergeSoundness(Class<ConfigurableProgramAnalysis> pCpa,
                                 ConfigurableProgramAnalysis pCpaInst, FunctionEntryNode pMain) throws CPAException {
    AbstractDomain d = pCpaInst.getAbstractDomain();
    MergeOperator merge = pCpaInst.getMergeOperator();
    AbstractState initial = pCpaInst.getInitialState(pMain);
    Precision initialPrec = pCpaInst.getInitialPrecision(pMain);

    return ensure(d.isLessOrEqual(initial, merge.merge(initial,initial,initialPrec)),
        "Merging same elements was unsound!");
  }


  private static boolean checkStopEmptyReached(
                                            Class<ConfigurableProgramAnalysis> pCpa,
                                            ConfigurableProgramAnalysis pCpaInst, FunctionEntryNode pMain) throws CPAException {
    StopOperator stop = pCpaInst.getStopOperator();
    HashSet<AbstractState> reached = new HashSet<AbstractState>();
    AbstractState initial = pCpaInst.getInitialState(pMain);
    Precision initialPrec = pCpaInst.getInitialPrecision(pMain);

    return ensure(!stop.stop(initial, reached, initialPrec), "Stopped on empty set!");
  }

  private static boolean checkStopReached(Class<ConfigurableProgramAnalysis> pCpa,
                                       ConfigurableProgramAnalysis pCpaInst, FunctionEntryNode pMain) throws CPAException {
    StopOperator stop = pCpaInst.getStopOperator();
    HashSet<AbstractState> reached = new HashSet<AbstractState>();
    AbstractState initial = pCpaInst.getInitialState(pMain);
    reached.add(initial);
    Precision initialPrec = pCpaInst.getInitialPrecision(pMain);

    return ensure(stop.stop(initial, reached, initialPrec), "Did not stop on same element!");
  }

  private static List<Class<ConfigurableProgramAnalysis>> getCPAs() throws ClassNotFoundException, IOException {
    List<Class<?>> cpaCandidates = getClasses("org.sosy_lab.cpachecker.cpa");
    List<Class<ConfigurableProgramAnalysis>> cpas = new ArrayList<Class<ConfigurableProgramAnalysis>>();

    Class<ConfigurableProgramAnalysis> targetType = null;

    for (Class<?> candidate : cpaCandidates) {
      if (   !Modifier.isAbstract(candidate.getModifiers())
          && !Modifier.isInterface(candidate.getModifiers())
          && ConfigurableProgramAnalysis.class.isAssignableFrom(candidate)) {

        // candidate is non-abstract implementation of CPA interface
        cpas.add(uncheckedGenericCast(candidate, targetType));
      }
    }

    return cpas;
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> uncheckedGenericCast(Class<?> classObj, Class<T> targetType) {
    return (Class<T>)classObj;
  }

  /**
   * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
   *
   * @param packageName The base package
   * @return The classes
   * @throws IOException
   *
   * taken from http://www.sourcesnippets.com/java-get-all-classes-within-a-package.html
   */
  private static List<Class<?>> getClasses(String packageName) throws IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);

    ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      collectClasses(new File(resource.getFile()), packageName, classes);
    }
    return classes;
  }

  /**
   * Recursive method used to find all classes in a given directory and subdirs.
   *
   * @param directory   The base directory
   * @param packageName The package name for classes found inside the base directory
   * @param classes     List where the classes are added.
   *
   * taken from http://www.sourcesnippets.com/java-get-all-classes-within-a-package.html
   */
  private static void collectClasses(File directory, String packageName, List<Class<?>> classes) {
    if (!directory.exists()) {
      return;
    }
    File[] files = directory.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        assert !file.getName().contains(".");
        collectClasses(file, packageName + "." + file.getName(), classes);
      } else if (file.getName().endsWith(".class")) {
        try {
          Class<?> foundClass = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
          classes.add(foundClass);
        } catch (ClassNotFoundException e) {
          /* ignore, there is no class available for this file */
        }
      }
    }
  }

}
