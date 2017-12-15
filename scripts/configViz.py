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

# global flag for return code
errorFound = False

def log(msg, level=1):
    if level <= logLevel:
      global errorFound
      errorFound = True
      print("WARNING: " + msg, file=sys.stderr)


class Node:
  def __init__(self, name):
    self.name = name
    self.children = collectChildren(name) # collect names of children
    self.parents = [] # filled later


def getTransitiveChildren(start, nodes):
  '''return all children including start node'''
  transitiveChildren = set()
  waitlist = [start]
  while waitlist:
    node = nodes[waitlist.pop()]
    if node.name not in transitiveChildren:
      transitiveChildren.add(node.name)
      waitlist.extend(node.children)
  return transitiveChildren


def getTransitiveParents(start, nodes):
  '''return all parents including start node'''
  transitiveParents= set()
  waitlist = [start]
  while waitlist:
    node = nodes[waitlist.pop()]
    if node.name not in transitiveParents:
      transitiveParents.add(node.name)
      waitlist.extend(node.parents)
  return transitiveParents


def getFilenamesFromLine(line):
  '''extract all filenames from a line'''
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
  if not fname:
    return []
  fnames = [name.strip().split("::")[0] for name in fname.split(",")]
  assert all(fnames), line
  return fnames


def collectChildren(filename):
  children = set()
  try:
    for line in open(filename,"r"):
      # TODO multiline statements?
      if not line.startswith(('#','//')) and line.rstrip() != line and line.rstrip() != line[:-1]:
        log("trailing whitespace in config '%s' in line '%s'" % (filename, line.strip()), level=2)
      for child in getFilenamesFromLine(line):
        child = os.path.normpath(os.path.join(os.path.dirname(filename), child))
        if os.path.exists(child):
          children.add(child)
        else:
          log("file '%s' referenced in '%s' does not exists" % (child, filename))
  except UnicodeDecodeError:
    log("Cannot read file '%s'" % filename, level=3)
  return children


def listFiles(path):
  '''recursively traverse the given path and collect all files'''
  for root, subFolders, files in os.walk(path):
    for item in files:
      if not "README" in item: # filter unwanted files
        yield os.path.normpath(os.path.join(root,item))


def writeDot(nodes, out):
  '''print dot file for limited set of nodes'''

  out.write('digraph configs {\n')
  out.write('graph [ranksep="%s" rankdir="LR"];\n' % (args.ranksep))
  out.write('node [shape=box]\n')
  for filename,v in sorted(nodes.items()):
    out.write(determineNode(v))

    for child in v.children:
      if child in nodes:
        out.write('"%s" -> "%s";\n' % (normPath(filename), normPath(nodes[child].name)))

  out.write("}\n")

def writeRSF(nodes,out):
  '''print graph in Relational Standard Format(RSF)'''
  for filename,v in sorted(nodes.items()):
    for child in v.children:
      if child in nodes:
        out.write('GRAPH %s %s 1\n' % (normPath(filename), normPath(nodes[child].name)))

def normPath(f):
    return os.path.relpath(f, args.dir)

def determineNode(node):
  filename = node.name
  content = None
  if os.path.isfile(filename):
    content = ""
    try:
      content = re.sub("\n", "&#10;", open(filename,"r").read())
    except UnicodeDecodeError:
      log("Cannot read file '%s'" % filename, level=3)
    content = re.sub('"','\\"', content)
  else:
    log("File does not exist: '%s'" % (filename))
  color = determineColor(node)

  tooltip = ""
  if content != None:
    tooltip = 'tooltip = "%s"' % content

  style = ""
  if color != None:
    style = 'style=filled fillcolor="%s"' % color

  options =  "%s %s" % (tooltip, style)
  result = '"%s"[%s]\n' % (normPath(node.name), options)
  return result

def determineColor(node):
  color = None
  if os.path.splitext(node.name)[1] != ".properties":
    color = "gold"
  elif len(node.parents) == 0 or (len(node.parents) == 1 and node.parents[0]==node.name):
    color = "forestgreen"
  elif not ("/" in normPath(node.name)):
    color = "darkolivegreen1"
  elif "components/" in node.name:
    color = "grey"
  elif "includes/" in node.name:
    color = "aquamarine"
  elif "cex-checks/" in node.name:
    color = "coral2"
  return color


def parseArgs():
    parser = argparse.ArgumentParser(formatter_class=argparse.RawTextHelpFormatter,
        description="""
Visualize config file dependencies. This will output a graphviz graph on stdout.
The graphviz file contains tooltips displaying the content of each config file!
(but xdot fails to show them, try converting it to svg and open it in a browser)

If multiple otions are used, they are applied in the following way:
The union of ROOT-files and DEPEND-files is FILTERED.

Examples:
    # graph with all files in the configuration folder:
    python3 scripts/configViz.py > graph.dot && xdot graph.dot

    # graph showing everything included in 'svcomp18.properties':
    python3 scripts/configViz.py --root config/svcomp18.properties > graph.dot && xdot graph.dot

    # graph showing everything depending on 'specification/default.spc':
    python3 scripts/configViz.py --depend config/specification/default.spc > graph.dot && xdot graph.dot

    # graph showing everything included in or depending on 'predicateAnalysis.properties':
    python3 scripts/configViz.py --root config/predicateAnalysis.properties --depend config/predicateAnalysis.properties > graph.dot
    """)
    parser.add_argument("--dir", metavar="DIRECTORY", default='config/',
        help="directory where the configuration files reside")
    parser.add_argument("--root", metavar="ROOT", default=None,
        help="configuration file for which a graph should be generated. " +
            "When specified, only files included (directly or indirectely) from this file are shown")
    parser.add_argument("--depend", metavar="DEPEND", default=None,
        help="configuration file for which a graph should be generated. " +
            "When specified, only files depending (directly or indirectely) on this file are shown")
    parser.add_argument("--filter", metavar="FILTER", default=None,
        help="String to filter nodes. " +
            "When specified, only matching nodes are shown")
    parser.add_argument("--ranksep", metavar="NUM", default=3,
        help="ranksep to use in the graphviz output file")
    parser.add_argument("--logLevel", metavar="LEVEL", default=1,
        help="a higher value enables more warnings, 0 is OFF")
    parser.add_argument("--rsf", action="store_true",
        help="output in Relational Standard Format (RSF) instead of graphviz")
    return parser.parse_args()


def getNodes(configDirectory):
  '''collect all files and build a graph'''
  nodes = {}

  # collect nodes and their children
  waitlist = list(listFiles(configDirectory))
  while waitlist:
    name = waitlist.pop()
    if name not in nodes:
      node = Node(name)
      nodes[name] = node
      waitlist.extend(node.children)

  # insert parents
  for name,node in nodes.items():
    for child in node.children:
      nodes[child].parents.append(name)

  return nodes

def componentsSanityCheck(nodes):
  '''Check for configuration files in the components folder that are never used.'''
  for name,node in nodes.items():
    if "components/" in name and len(node.parents)==0:
      log("Component file %s is unused!" % name)


if __name__ == "__main__":
  args = parseArgs()

  global logLevel
  logLevel = int(args.logLevel)

  nodes = getNodes(args.dir)

  componentsSanityCheck(nodes)

  nodesFromRoot = {}
  if args.root != None:
    if args.root not in nodes:
      log("Root file '%s' not found." % args.root)
    else:
      children = getTransitiveChildren(args.root, nodes)
      nodesFromRoot = dict((k,v) for k,v in nodes.items() if k in children)

  nodesFromDepend = {}
  if args.depend != None:
    if args.depend not in nodes:
      log("Depend file '%s' not found." % args.depend)
    else:
      parents = getTransitiveParents(args.depend, nodes)
      nodesFromDepend = dict((k,v) for k,v in nodes.items() if k in parents)

  # if any option is set, use its output
  if nodesFromRoot:
    nodes = nodesFromRoot
    if nodesFromDepend:
      nodes.update(nodesFromDepend) # union of maps
  elif nodesFromDepend:
    nodes = nodesFromDepend

  if args.filter != None:
    nodes = dict((k,v) for k,v in nodes.items()
        if args.filter in k or any(args.filter in child for child in v.children))

  # write dot-output
  out = sys.stdout #open("configViz.dot","w")
  if not args.rsf:
    writeDot(nodes, out)
  else:
    writeRSF(nodes, out)

  exit(errorFound)
