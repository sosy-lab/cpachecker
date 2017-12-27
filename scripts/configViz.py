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
    self.childNodes = [] # filled later
    self.parentNodes = [] # filled later


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
    m = re.search("^[a-zA-Z\.]*\.(config|terminatingStatements|checkerConfig)(?:Files|)\s*=\s*(.*)\s*",line)
    if m != None:
      fname = m.group(2)
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


def listFiles(paths):
  '''recursively traverse the given path and collect all files'''
  for path in paths:
    for root, subFolders, files in os.walk(path):
      for item in files:
        if not "README" in item: # filter unwanted files
          yield os.path.normpath(os.path.join(root,item))


def writeDot(nodes, out, showChildDependencies = True, showParentDependencies = True, markDependencies = True):
  '''print dot file for limited set of nodes'''

  out.write('digraph configs {\n')
  out.write('graph [ranksep="%s" rankdir="LR"];\n' % (args.ranksep))
  out.write('node [shape=box]\n')

  allNodesDict,childDependencyNodes,parentDependencyNodes = determineDependencies(nodes, showChildDependencies, showParentDependencies)

  for name,node in sorted(allNodesDict.items()):
    isDependency = name in childDependencyNodes or name in parentDependencyNodes
    out.write(determineNode(node,dependencyNode = isDependency and markDependencies))

  if not markDependencies:
    nodes = allNodesDict

  for filename,v in sorted(nodes.items()):
    for child in v.children:
      if child in nodes:
        out.write('"%s" -> "%s";\n' % (normPath(filename), normPath(nodes[child].name)))
      elif child in childDependencyNodes:
        out.write('"%s" -> "%s" [style="dashed" color="grey"];\n'
                  % (normPath(filename), normPath(childDependencyNodes[child].name)))
    for parent in v.parents:
      if not parent in nodes and parent in parentDependencyNodes:
        out.write('"%s" -> "%s" [style="dashed" color="grey"];\n'
                  % (normPath(parentDependencyNodes[parent].name), normPath(filename)))

  out.write("}\n")

def writeRSF(nodes, out, showChildDependencies = True, showParentDependencies = True):
  '''print graph in Relational Standard Format(RSF)'''
  nodes = determineDependencies(nodes, showChildDependencies, showParentDependencies)[0]
  for filename,v in sorted(nodes.items()):
    for child in v.children:
      if child in nodes:
        out.write('GRAPH %s %s 1\n' % (normPath(filename), normPath(nodes[child].name)))

def normPath(f):
    return os.path.relpath(f, args.dir[0])

def determineDependencies(nodes, showChildDependencies, showParentDependencies):
  childDependencyNodes = dict()
  if showChildDependencies:
    childDependencyNodes = dict((childNode.name,childNode)
                                for k,v in nodes.items()
                                for childNode in v.childNodes
                                if childNode.name not in nodes)
  parentDependencyNodes = dict()
  if showParentDependencies:
    parentDependencyNodes = dict((parentNode.name,parentNode)
                                 for k,v in nodes.items()
                                 for parentNode in v.parentNodes
                                 if parentNode.name not in nodes)

  allNodesDict = dict()
  allNodesDict.update(nodes)
  allNodesDict.update(childDependencyNodes)
  allNodesDict.update(parentDependencyNodes)
  return (allNodesDict, childDependencyNodes, parentDependencyNodes)

def determineNode(node, dependencyNode = False):
  filename = node.name
  content = None
  if os.path.isfile(filename):
    content = ""
    try:
      content = re.sub("\n", "&#10;", open(filename,"r").read())
    except UnicodeDecodeError:
      log("Cannot read file '%s'" % filename, level=3)
    content = content.replace('\\','\\\\').replace('"','\\"')
  else:
    log("File does not exist: '%s'" % (filename))
  color = determineColor(node)

  tooltip = ""
  if content != None:
    tooltip = 'tooltip = "%s"' % content

  style = ""
  if color != None:
    style = 'style=filled fillcolor="%s"' % color
  if dependencyNode:
    style = 'style="setlinewidth(3),rounded" color="%s"' % color

  options =  "%s %s" % (tooltip, style)
  result = '"%s"[%s]\n' % (normPath(node.name), options)
  return result

def determineColor(node):
  color = None
  if os.path.splitext(node.name)[1] != ".properties":
    color = "gold"
  elif not node.parents or (len(node.parents) == 1 and node.parents[0] == node.name):
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
    parser.add_argument("--dir", metavar="DIRECTORY", default=['config/', 'test/config'], action="append",
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
    parser.add_argument("--showChildren", action="store_true",
        help="Show children of selected nodes")
    parser.add_argument("--showParents", action="store_true",
        help="Show parents of selected nodes")
    parser.add_argument("--samedep", action="store_true",
        help="Make dependency nodes look like regular nodes")
    return parser.parse_args()


def getNodes(configDirectories):
  '''collect all files and build a graph'''
  nodes = {}

  # collect nodes and their children
  waitlist = list(listFiles(configDirectories))
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
      nodes[child].parentNodes.append(node)
      node.childNodes.append(nodes[child])

  return nodes

def componentsSanityCheck(nodes):
  '''Check for configuration files in the components folder that are never used.'''
  for name,node in nodes.items():
    if not node.parents:
      if "components/" in name:
        log("Component file %s is unused!" % name, 2)
      if "includes/" in name:
        log("Include file %s is unused!" % name)


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
        if args.filter in k)

  # write dot-output
  out = sys.stdout #open("configViz.dot","w")
  if not args.rsf:
    writeDot(nodes, out, args.showChildren, args.showParents, not args.samedep)
  else:
    writeRSF(nodes, out, args.showChildren, args.showParents)

  exit(errorFound)
