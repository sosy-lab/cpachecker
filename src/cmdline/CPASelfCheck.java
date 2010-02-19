/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007,2008,2009  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cmdline;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.IASTServiceProvider.UnsupportedDialectException;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.resources.IFile;

import common.configuration.Configuration;

import cfa.CFABuilder;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain.InvalidCmdlineArgumentException;
import cmdline.stubs.StubFile;
import cpa.common.CPAchecker;
import cpa.common.LogManager;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;
import exceptions.InvalidConfigurationException;

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
    try {
      logManager = new LogManager(cpaConfig);
    } catch (InvalidConfigurationException e) {
      System.err.println("Invalid configuration: " + e.getMessage());
      System.exit(1);
    }
    
    CPAchecker cpachecker = null;
    try {
      cpachecker = new CPAchecker(cpaConfig, logManager);
    } catch (InvalidConfigurationException e) {
      logManager.log(Level.SEVERE, "Invalid configuration:", e.getMessage());
    }
    
    try {
      logManager.log(Level.INFO, "Searching for CPAs");
      LinkedList<Class<ConfigurableProgramAnalysis>> cpas = getCPAs();
      
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
        
        CFAFunctionDefinitionNode main = createCFA(cpachecker);
       
        try {
          cpaInst.getInitialElement(main);
        } catch (Exception e) {
          logManager.logException(Level.WARNING, e, 
              "Getting initial element failed!");
          continue;
        }

        boolean ok = true;
        // check domain and lattice
        ok &= checkSingletonBottomTop(cpa, cpaInst, main);
        ok &= checkBottomLessThanTop(cpa, cpaInst, main);
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
  
  private static CFAFunctionDefinitionNode createCFA(CPAchecker cpachecker) throws IOException, UnsupportedDialectException {
    File lFile = File.createTempFile("dummy", ".c");
    lFile.deleteOnExit();
       
    PrintWriter lWriter = new PrintWriter(lFile);

    lWriter.print(
"int main() {\n" +
"  int a;\n" +
"  a = 1;" +
"  return (a);\n" +
"}\n"
    		);

    lWriter.close();

    IFile currentFile = new StubFile(lFile.getCanonicalPath());

    // Get Eclipse to parse the C in the current file
    IASTTranslationUnit ast = cpachecker.parse(currentFile);

    // TODO use the methods from CPAMain for this?
    CFABuilder builder = new CFABuilder();
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
  
  private static boolean checkSingletonBottomTop(Class<ConfigurableProgramAnalysis> pCpa, ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Constructor<ConfigurableProgramAnalysis> ct = pCpa.getConstructor(String.class, String.class);
    Object argumentlist[] = {"sep", "sep"};
    ConfigurableProgramAnalysis inst2 = ct.newInstance(argumentlist);
    
    // maybe use equals, but elements should really be static
    boolean ok = true;
    ok &= ensure(pCpaInst.getAbstractDomain().getBottomElement() == inst2.getAbstractDomain().getBottomElement(),
        "Bottom elements are not the same!");
    ok &= ensure(pCpaInst.getAbstractDomain().getTopElement() == inst2.getAbstractDomain().getTopElement(),
        "Top elements are not the same!");
    return ok;
  }

  private static boolean checkBottomLessThanTop(Class<ConfigurableProgramAnalysis> pCpa, ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws CPAException {
    PartialOrder le = pCpaInst.getAbstractDomain().getPartialOrder();
    AbstractElement bottom = pCpaInst.getAbstractDomain().getBottomElement();
    AbstractElement top = pCpaInst.getAbstractDomain().getBottomElement();
    
    boolean ok = true;
    ok &= ensure(le.satisfiesPartialOrder(bottom, top), "Bottom is not less or equal to top!");
    ok &= ensure(!le.satisfiesPartialOrder(top, bottom), "Top is less or equal to bottom!");
    return ok;
  }
  

  private static boolean checkInitialElementInLattice(
                                                   Class<ConfigurableProgramAnalysis> pCpa,
                                                   ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws CPAException {
    PartialOrder le = pCpaInst.getAbstractDomain().getPartialOrder();
    AbstractElement bottom = pCpaInst.getAbstractDomain().getBottomElement();
    AbstractElement top = pCpaInst.getAbstractDomain().getBottomElement();
    AbstractElement initial = pCpaInst.getInitialElement(pMain);
    
    boolean ok = true;
    ok &= ensure(le.satisfiesPartialOrder(bottom, initial), "Initial element is not greater or equal bottom!");
    ok &= ensure(le.satisfiesPartialOrder(initial, top), "Initial element is not less or equal top!");
    return ok;
  }

  private static boolean checkJoin(Class<ConfigurableProgramAnalysis> pCpa,
                                ConfigurableProgramAnalysis pCpaInst, CFAFunctionDefinitionNode pMain) throws CPAException {
    PartialOrder le = pCpaInst.getAbstractDomain().getPartialOrder();
    JoinOperator join = pCpaInst.getAbstractDomain().getJoinOperator();
    AbstractElement top = pCpaInst.getAbstractDomain().getBottomElement();
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

  @SuppressWarnings("unchecked")
  private static LinkedList<Class<ConfigurableProgramAnalysis>> getCPAs() throws ClassNotFoundException, IOException {
    Class[] cpaCandidates = getClasses("cpa");
    
    HashMap<Class,HashSet<String>> transitiveInterfaces = new HashMap<Class,HashSet<String>>();
    LinkedList<Class> waitlist = new LinkedList<Class>();
    
    for (Class cl : cpaCandidates) {
      waitlist.add(cl);
    }
    
    while (!waitlist.isEmpty()) {
      Class cl = waitlist.poll();
      boolean allParentInterfacesKnown = true;
      HashSet<String> allInterfaces = new HashSet<String>();
      
      for (Class p : cl.getInterfaces()) {
        if (!p.getCanonicalName().startsWith("cpa.")) continue;
        allInterfaces.add(p.getCanonicalName());
        HashSet<String> ifs = transitiveInterfaces.get(p);
        if (null == ifs) {
          allParentInterfacesKnown = false;
          break;
        } else {
          allInterfaces.addAll(ifs);
        }
      }
      if (allParentInterfacesKnown && null != cl.getSuperclass() &&
          cl.getSuperclass().getCanonicalName().startsWith("cpa.")) {
        HashSet<String> ifs = transitiveInterfaces.get(cl.getSuperclass());
        if (null == ifs) {
          allParentInterfacesKnown = false;
        } else {
          allInterfaces.addAll(ifs);
        }
      }
      if (!allParentInterfacesKnown) {
        waitlist.add(cl);
        continue;
      } else {
        transitiveInterfaces.put(cl, allInterfaces);
      }
    }
    
    LinkedList<Class<ConfigurableProgramAnalysis>> cpas = new LinkedList<Class<ConfigurableProgramAnalysis>>();
    
    for(Map.Entry<Class, HashSet<String>> clEntry : transitiveInterfaces.entrySet()) {
      if (Modifier.isAbstract(clEntry.getKey().getModifiers()) ||
          Modifier.isInterface(clEntry.getKey().getModifiers())) continue;
      
      if (clEntry.getValue().contains("cpa.common.interfaces.ConfigurableProgramAnalysis")) {
        cpas.add(clEntry.getKey());
      } /*else {
        System.out.println("Skipping " + clEntry.getKey().getCanonicalName());
        System.out.println("Parents: " + clEntry.getValue());
      }*/
    }
    
    return cpas;
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
  @SuppressWarnings("unchecked")
  private static Class[] getClasses(String packageName)
  throws ClassNotFoundException, IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<File>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      dirs.add(new File(resource.getFile()));
    }
    ArrayList<Class> classes = new ArrayList<Class>();
    for (File directory : dirs) {
      classes.addAll(findClasses(directory, packageName));
    }
    return classes.toArray(new Class[classes.size()]);
  }

  /**
   * Recursive method used to find all classes in a given directory and subdirs.
   *
   * @param directory   The base directory
   * @param packageName The package name for classes found inside the base directory
   * @return The classes
   * @throws ClassNotFoundException
   * 
   * taken from http://www.sourcesnippets.com/java-get-all-classes-within-a-package.html
   */
  @SuppressWarnings("unchecked")
  private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
    List<Class> classes = new ArrayList<Class>();
    if (!directory.exists()) {
      return classes;
    }
    File[] files = directory.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        assert !file.getName().contains(".");
        classes.addAll(findClasses(file, packageName + "." + file.getName()));
      } else if (file.getName().endsWith(".class")) {
        // System.err.println("Processing " + file.getName());
        classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
      }
    }
    return classes;
  }

}
