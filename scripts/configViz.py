#!/usr/bin/env python3

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2017  Dirk Beyer
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


CPAchecker web page:
  http://cpachecker.sosy-lab.org
"""

import argparse
import os
import re
import sys


# abort when reaching deeply nested configurations
MAX_NESTING = 30

class Node:
  nodes = dict()
  def __init__(self,name):
    assert not name in Node.nodes
    self.children = []
    self.parents = []
    self.name = name
    self.path = os.path.split(name)[0]
    self.recursionwarnings = 0
    Node.nodes[name] = self
    self.addChildren()

  def contains(self, name, i = 0):
    if i > MAX_NESTING:
      raise Exception("RECURSION")
    if self.name == name:
      return True
    for child in self.children:
      try:
        if child != self and child.contains(name, i + 1):
          return True
      except Exception as e:
        if i==0:
          if self.recursionwarnings == 0:
            print("WARNING: Recursion in %s trying to find %s" % (self.name, name), file=sys.stderr)
          self.recursionwarnings += 1
        else:
          raise e
    return False

  @staticmethod
  def filter(filename):
    return "README" not in filename

  def addChildren(self):
    if self.name ==".":
      return
    for line in open(self.name,"r"):
      fname = None
      if line[:8]=="#include":
        fname = line.split()[1]
      else:
        m = re.search("^[a-zA-Z\.]*\.config(?:Files|)\s*=\s*(.*)\s*",line)
        if m != None:
          fname = m.group(1)
        else:
          m = re.search("^specification\s*=\s*(.*)\s*",line)
          if m != None:
            fname = m.group(1)
      if fname == None:
        continue
      if fname.rstrip() != fname:
        print("WARNING: trailing whitespace in config reference '%s' in %s" % (fname, self.name), file=sys.stderr)
        fname = fname.rstrip()
      fnames = fname.split(",")
      for name in fnames:
        name = name.strip().split("::")[0]
        if name != "":
          name = os.path.join(self.path,name)
        name = os.path.normpath(name)
        if self.filter(name):
          if name not in Node.nodes:
            if not os.path.exists(name):
              print("WARNING: file %s referenced in %s does not exists" % (name, self.name), file=sys.stderr)
              continue
            else:
              child = Node(name)
          else:
            child = Node.nodes[name]
          self.children.append(child)
          child.parents.append(self)


def listfiles(path):
  '''recursively traverse the given path and collect all files'''
  for root, subFolders, files in os.walk(path):
    for item in files:
      yield os.path.normpath(os.path.join(root,item))


def parseArgs():
    parser = argparse.ArgumentParser(formatter_class=argparse.RawTextHelpFormatter,description=
"""Visualize config file dependencies. This will output a graphviz graph on stdout.
The graphviz file contains tooltips displaying the content of each config file!
(but xdot fails to show them, try converting it to svg and open it in a browser)
Examples
    # graph with all files in the configuration folder:
    python3 scripts/configViz.py ./config > graph.dot && xdot graph.dot
    # graph just showing everything on which svcomp18.properties depends:
    python3 scripts/configViz.py ./config config/svcomp18.properties --ranksep 1 >graph.dot && xdot graph.dot""")
    parser.add_argument("configdir", help = "directory where the configuration files reside")
    parser.add_argument("rootfile", nargs = '?', help = "optional configuration file for which a graph should be generated. When specified, only files included (directly or indirectely) from this file are shown")
    parser.add_argument("--filter" ,metavar="FILTER", help = "String to filter nodes. When specified, only matching nodes or nodes connected to matching nodes are shown")
    parser.add_argument("--ranksep", help = "ranksep to use in the graphviz output file", default = 8)
    return parser.parse_args()

if __name__ == "__main__":
  args = parseArgs()
  if len(sys.argv) <2:
    print("specify path!")
    exit()

  # collect all files
  trees = []
  for f in listfiles(args.configdir):
    if Node.filter(f) and f not in Node.nodes:
      trees.append(Node(f))
  for t in trees[::]:
    for ot in trees:
      if ot!=t and ot.contains(t.name):
        trees.remove(t)
        break

  graph = sys.stdout #open("configViz.dot","w")
  graph.write("digraph configs {\n")
  graph.write('graph [ranksep="%s" rankdir="LR"];\n' % (args.ranksep))
  graph.write('node [shape=box]\n')
  for (k,v) in Node.nodes.items():
    parentname = k
    relparentname = os.path.relpath(parentname,args.configdir)
    if args.filter != None:
      if not args.filter in parentname and not any([args.filter in child.name for child in v.children]):
        continue
    if args.rootfile != None:
      if not Node.nodes[args.rootfile].contains(parentname):
        continue
    if os.path.isfile(parentname):
      parentcontent = re.sub("\n", "&#10;", open(parentname,"r").read())
      parentcontent = re.sub('"','\\"',parentcontent)
    if parentname.split(".")[-1] != "properties":
      graph.write('"%s"[style=filled fillcolor="grey" color="dodgerblue"];\n' % relparentname)
    else:
      graph.write('"%s"[tooltip = "%s"];\n' % (relparentname, parentcontent))
    for child in v.children:
      childname = child.name
      relchildname = os.path.relpath(childname,args.configdir)
      graph.write('"%s" -> "%s";\n' % (relparentname, relchildname))
  graph.write("}\n")
