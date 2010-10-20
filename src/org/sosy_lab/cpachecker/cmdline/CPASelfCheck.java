/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

import org.sosy_lab.cpachecker.cfa.CFABuilder;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cmdline.CPAMain.InvalidCmdlineArgumentException;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CParser;
import org.sosy_lab.cpachecker.util.CParser.Dialect;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CPASelfCheck {

  private static LogManager logManager;

  /**
   * @param args
   */
  public static void main(String[] args) {
    CPAchecker cpachecker = null;
    try {
      Configuration cpaConfig = null;
      try {
        cpaConfig = CPAMain.createConfiguration(args);
      } catch (InvalidCmdlineArgumentException e) {
        System.err.println("Could not parse command line arguments: " + e.getMessage());
        System.exit(1);
      } catch (IOException e) {
        System.err.println("Could not read config file " + e.getMessage());
        System.exit(1);
      }

      logManager = new LogManager(cpaConfig);
      cpachecker = new CPAchecker(cpaConfig, logManager);
    } catch (InvalidConfigurationException e) {
      System.err.println("Invalid configuration: " + e.getMessage());
      System.exit(1);
    }

    try {
      logManager.log(Level.INFO, "Searching for CPAs");
      List<Class<ConfigurableProgramAnalysis>> cpas = getCPAs();

      for (Class<ConfigurableProgramAnalysis> cpa : cpas) {
        logManager.log(Level.INFO, "Checking " + cpa.getCanonicalName() + " ...");
        ConfigurableProgramAnalysis cpaInst = null;
        try {
          cpaInst = tryToInstantiate(cpa);
        } catch (InvocationTargetException e) {
          logManager.logException(Level.WARNING, e,
              "Instantiating " + cpa.getCanonicalName() + " failed!");
          continue;
        } catch (NoSuchMethodException e) {
          logManager.logException(Level.WARNING, e,
              "Instantiating " + cpa.getCanonicalName() +
              " failed: no (String, String) constructor!");
          continue;
        }
        assert(cpaInst != null);

        CFAFunctionDefinitionNode main = createCFA(cpachecker, logManager);

        try {
          cpaInst.getInitialElement(main);
        } catch (Exception e) {
          logManager.logException(Level.WARNING, e,
              "Getting initial element failed!");
          continue;
        }

        boolean ok = true;
        // check domain and lattice
        ok &= checkSingletonTop(cpa, cpaInst, main);
        ok &= checkInitialElementInLattice(cpa, cpaInst, main);
        ok &= checkJoin(cpa, cpaInst, main);
        /// TODO checking the invariantes of the transfer relation is a bit more work ...
        // check merge
        ok &= checkMergeSoundness(cpa, cpaInst, main);
        // check stop
        ok &= checkStopEmptyReached(cpa, cpaInst, main);
        ok &= checkStopReached(cpa, cpaInst, main);
        /// TODO check invariants of precision adjustment
        System.out.println(ok ? " OK" : " ERROR");
      }
    } catch (Exception e) {
      logManager.logException(Level.WARNING, e, "");
    }
  }

  private static CFAFunctionDefinitionNode createCFA(CPAchecker cpachecker, LogManager logManager) throws IOException, CoreException {
    String code =
"int main() {\n" +
"  int a;\n" +
"  a = 1;" +
"  return (a);\n" +
"}\n"
    		;

    // Get Eclipse to parse the C in the current file
    IASTTranslationUnit ast = CParser.parseString(code, Dialect.C99);

    // TODO use the methods from CPAMain for this?
    CFABuilder builder = new CFABuilder(logManager);
    ast.accept(builder);
    Map<String, CFAFunctionDefinitionNode> cfas = builder.getCFAs();
    return cfas.get("main");
  }

  private static ConfigurableProgramAnalysis tryToInstantiate(Class<ConfigurableProgramAnalysis> pCpa) throws NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Constructor<ConfigurableProgramAnalysis> ct = pCpa.getConstructor(String.class, String.class);
    Object argumentlist[] = {"sep", "sep"};

    return ct.newInstance(argumentlist);
  }

  private static boolean ensure(boolean pB, String pString) {
    if (!pB) {
      logManager.log(Level.WARNING, pString);
      // assert(false);
      return false;
    }
    return true;
  }

  private static boolean checkSingletonTop(Class<ConfigurableProgramAnalysis> pCpa, ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Constructor<ConfigurableProgramAnalysis> ct = pCpa.getConstructor(String.class, String.class);
    Object argumentlist[] = {"sep", "sep"};
    ConfigurableProgramAnalysis inst2 = ct.newInstance(argumentlist);

    // maybe use equals, but elements should really be static
    boolean ok = true;
    ok &= ensure(pCpaInst.getAbstractDomain().getTopElement() == inst2.getAbstractDomain().getTopElement(),
        "Top elements are not the same!");
    return ok;
  }


  private static boolean checkInitialElementInLattice(
                                                   Class<ConfigurableProgramAnalysis> pCpa,
                                                   ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws CPAException {
    PartialOrder le = pCpaInst.getAbstractDomain().getPartialOrder();
    AbstractElement top = pCpaInst.getAbstractDomain().getTopElement();
    AbstractElement initial = pCpaInst.getInitialElement(pMain);

    boolean ok = true;
    ok &= ensure(le.satisfiesPartialOrder(initial, top), "Initial element is not less or equal top!");
    return ok;
  }

  private static boolean checkJoin(Class<ConfigurableProgramAnalysis> pCpa,
                                ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws CPAException {
    PartialOrder le = pCpaInst.getAbstractDomain().getPartialOrder();
    JoinOperator join = pCpaInst.getAbstractDomain().getJoinOperator();
    AbstractElement top = pCpaInst.getAbstractDomain().getTopElement();
    AbstractElement initial = pCpaInst.getInitialElement(pMain);

    boolean ok = true;
    ok &= ensure(le.satisfiesPartialOrder(initial, join.join(initial,initial)),
        "Join of same elements is unsound!");
    ok &= ensure(le.satisfiesPartialOrder(top, join.join(initial, top)), "Join with top is unsound!");
    return ok;
  }

  private static boolean checkMergeSoundness(Class<ConfigurableProgramAnalysis> pCpa,
                                 ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws CPAException {
    PartialOrder le = pCpaInst.getAbstractDomain().getPartialOrder();
    MergeOperator merge = pCpaInst.getMergeOperator();
    AbstractElement initial = pCpaInst.getInitialElement(pMain);
    Precision initialPrec = pCpaInst.getInitialPrecision(pMain);

    boolean ok = true;
    ok &= ensure(merge != null, "Merge-hack: mergeOperator is null!");
    if (!ok) return false;
    ok &= ensure(le.satisfiesPartialOrder(initial, merge.merge(initial,initial,initialPrec)),
        "Merging same elements was unsound!");
    return ok;
  }


  private static boolean checkStopEmptyReached(
                                            Class<ConfigurableProgramAnalysis> pCpa,
                                            ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws CPAException {
    StopOperator stop = pCpaInst.getStopOperator();
    HashSet<AbstractElement> reached = new HashSet<AbstractElement>();
    AbstractElement initial = pCpaInst.getInitialElement(pMain);
    Precision initialPrec = pCpaInst.getInitialPrecision(pMain);

    boolean ok = true;
    ok &= ensure(!stop.stop(initial, reached, initialPrec), "Stopped on empty set!");
    return ok;
  }

  private static boolean checkStopReached(Class<ConfigurableProgramAnalysis> pCpa,
                                       ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws CPAException {
    StopOperator stop = pCpaInst.getStopOperator();
    HashSet<AbstractElement> reached = new HashSet<AbstractElement>();
    AbstractElement initial = pCpaInst.getInitialElement(pMain);
    reached.add(initial);
    Precision initialPrec = pCpaInst.getInitialPrecision(pMain);

    boolean ok = true;
    ok &= ensure(stop.stop(initial, reached, initialPrec), "Did not stop on same element!");
    return ok;
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
   * @throws ClassNotFoundException
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
   * @throws ClassNotFoundException
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
