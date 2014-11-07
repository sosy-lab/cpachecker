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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.net.URISyntaxException;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

import com.google.common.base.StandardSystemProperty;

/**
 * Helper class for loading native libraries.
 */
public class NativeLibraries {

  private NativeLibraries() {}

  public static enum OS { LINUX, MACOSX, WINDOWS;

    private static OS CURRENT_OS = null;

    public static OS guessOperatingSystem() throws UnsatisfiedLinkError {
      if (CURRENT_OS != null) {
        return CURRENT_OS;
      }

      String prop = StandardSystemProperty.OS_NAME.value();
      if (isNullOrEmpty(prop)) {
        throw new UnsatisfiedLinkError("No value for os.name, "
            + "please report this together with information about your system (OS, architecture, JVM).");
      }

      prop = prop.toLowerCase().replace(" ", "");

      if (prop.startsWith("linux")) {
        CURRENT_OS = LINUX;
      } else if (prop.startsWith("windows")) {
        CURRENT_OS = WINDOWS;
      } else if (prop.startsWith("macosx")) {
        CURRENT_OS = MACOSX;
      } else {
        throw new UnsatisfiedLinkError("Unknown value for os.name: '" + StandardSystemProperty.OS_NAME.value()
            + "', please report this together with information about your system (OS, architecture, JVM).");
      }

      return CURRENT_OS;
    }
  }

  public static enum Architecture { X86, X86_64;

    private static Architecture CURRENT_ARCH = null;

    public static Architecture guessVmArchitecture() throws UnsatisfiedLinkError {
      if (CURRENT_ARCH != null) {
        return CURRENT_ARCH;
      }

      String prop = System.getProperty("os.arch.data.model");
      if (isNullOrEmpty(prop)) {
        prop = System.getProperty("sun.arch.data.model");
      }

      if (!isNullOrEmpty(prop)) {
        if (prop.equals("32")) {
          CURRENT_ARCH = Architecture.X86;
        } else if (prop.equals("64")) {
          CURRENT_ARCH = Architecture.X86_64;
        } else {
          throw new UnsatisfiedLinkError("Unknown value for os.arch.data.model: '" + prop
              + "', please report this together with information about your system (OS, architecture, JVM).");
        }
      } else {

        prop = StandardSystemProperty.JAVA_VM_NAME.value();
        if (!isNullOrEmpty(prop)) {
          prop = prop.toLowerCase();


          if (   prop.contains("32-bit")
              || prop.contains("32bit")
              || prop.contains("i386")) {

            CURRENT_ARCH = Architecture.X86;
          } else if (
                 prop.contains("64-bit")
              || prop.contains("64bit")
              || prop.contains("x64")
              || prop.contains("x86_64")
              || prop.contains("amd64")) {

            CURRENT_ARCH = Architecture.X86_64;
          } else {
            throw new UnsatisfiedLinkError("Unknown value for java.vm.name: '" + prop
                + "', please report this together with information about your system (OS, architecture, JVM).");
          }
        } else {
          throw new UnsatisfiedLinkError("Could not detect system architecture");
        }
      }

      return CURRENT_ARCH;
    }
  }

  private static Path nativePath = null;

  public static Path getNativeLibraryPath() {
    // We expected the libraries to be in the directory lib/native/<arch>-<os>
    // relative to the parent of the code.
    // When the code resides in a JAR file, the JAR file needs to be in the same
    // directory as the "lib" directory.
    // When the code is in .class files, those .class files need to be in a
    // sub-directory of the one with the "lib" directory (e.g., in a "bin" directory).

    if (nativePath == null) {
      String arch = Architecture.guessVmArchitecture().name().toLowerCase();
      String os = OS.guessOperatingSystem().name().toLowerCase();
      URI pathToJar;
      try {
        pathToJar = NativeLibraries.class.getProtectionDomain().getCodeSource().getLocation().toURI();
      } catch (URISyntaxException e) {
        throw new AssertionError(e);
      }

      nativePath = Paths.get(pathToJar).getParent().resolve(Paths.get("lib", "native", arch + "-" + os));
    }
    return nativePath;
  }

  /**
   * Load a native library.
   * This is similar to {@link System#loadLibrary(String)},
   * but additionally tries more directories for the search path of the library.
   */
  public static void loadLibrary(String name) {
    // We first try to load the library via the normal VM way.
    // This way one can use the java.library.path property to point the VM
    // to another file.
    // Only if this fails (which is expected if the user did not specify the library path)
    // we try to load the file from the architecture-specific directory under "lib/native/".
    try {
      System.loadLibrary(name);
    } catch (UnsatisfiedLinkError firstEx) {
      try {
        Path file = getNativeLibraryPath().resolve(System.mapLibraryName(name)).toAbsolutePath();
        System.load(file.toString());
      } catch (Throwable t) {
        t.addSuppressed(firstEx);
        throw t;
      }
    }
  }
}
