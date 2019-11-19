/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util.harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class HeaderFileManager {

  private String headers;
  private String[] headerDirectories =
      {"/usr/lib/gcc/x86_64-linux-gnu/7/include", "/usr/local/include",
          "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed", "/usr/include"};
  private String[] headerFiles =
      {"aio.h", "arpa/inet.h", "assert.h", "complex.h", "cpio.h", "ctype.h", "dirent.h", "dlfcn.h",
          "errno.h", "fcntl.h", "fenv.h", "float.h", "fmtmsg.h", "fnmatch.h", "ftw.h", "glob.h",
          "grp.h", "iconv.h", "inttypes.h", "iso646.h", "langinfo.h", "libgen.h", "limits.h",
          "locale.h", "math.h", "monetary.h", "mqueue.h", "ndbm.h", "netdb.h", "net/if.h",
          "netinet/in.h", "netinet/tcp.h", "nl_types.h", "poll.h", "pthread.h", "pwd.h", "regex.h",
          "sched.h", "search.h", "semaphore.h", "setjmp.h", "signal.h", "spawn.h", "stdalign.h",
          "stdarg.h", "stdatomic.h", "stdbool.h", "stddef.h", "stdint.h", "stdio.h", "stdlib.h",
          "stdnoreturn.h", "string.h", "strings.h", "stropts.h", "sys/ipc.h", "syslog.h",
          "sys/mman.h", "sys/msg.h", "sys/resource.h", "sys/select.h", "sys/sem.h", "sys/shm.h",
          "sys/socket.h", "sys/stat.h", "sys/statvfs.h", "sys/time.h", "sys/times.h", "sys/types.h",
          "sys/uio.h", "sys/un.h", "sys/utsname.h", "sys/wait.h", "tar.h", "termios.h", "tgmath.h",
          "threads.h", "time.h", "trace.h", "uchar.h", "ulimit.h", "unistd.h", "utime.h", "utmpx.h",
          "wchar.h", "wctype.h", "wordexp.h"};

  public HeaderFileManager() {
    loadHeaders();
  }

  public boolean headersContainName(String pName) {
    return headers.contains(" " + pName + " ") || headers.contains("*" + pName + " ");
  }

  private void loadHeaders() {
    List<String> readFiles = new ArrayList<>();
    for(String directory : headerDirectories) {
      for(String headerFile : headerFiles) {
        try {
          if (readFiles.contains(headerFile)) {
            continue;
          }
          String file = readFile(directory + '/' + headerFile);
          headers += file;
          readFiles.add(headerFile);
        } catch (IOException e) {
          continue;
        }
      }
    }
  }

  private String readFile(String path) throws IOException {
    String functionDeclarationLines = "";
    try (Stream<String> lines = Files.lines(Paths.get(path))) {
      functionDeclarationLines =
          lines.filter(line -> line.startsWith("extern") || line.startsWith("__extension__ extern"))
              .reduce("", String::concat);
    }
    return functionDeclarationLines;
  }
}
