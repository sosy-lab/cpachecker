#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import argparse
import os
import re
import sys
from collections import defaultdict
from enum import Enum


class EdgeType(Enum):
    NORMAL = 0  # dependencies caused by "#include ..."
    SPECIAL = 1  # things like "parallelAlgorithm.configFiles=..."
    SPECIFICATION = 2  # dependencies caused by "specification = ..."


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
        self.childrenToType = collectChildren(name)  # collect types of children
        self.children = set(self.childrenToType.keys())
        for c in self.children:
            assert c in self.childrenToType
        self.parents = []  # filled later
        self.childNodes = []  # filled later
        self.parentNodes = []  # filled later


def getTransitiveChildren(start, nodes):
    """return all children including start node"""
    transitiveChildren = set()
    waitlist = [start]
    while waitlist:
        node = nodes[waitlist.pop()]
        if node.name not in transitiveChildren:
            transitiveChildren.add(node.name)
            waitlist.extend(node.children)
    return transitiveChildren


def getTransitiveParents(start, nodes):
    """return all parents including start node"""
    transitiveParents = set()
    waitlist = [start]
    while waitlist:
        node = nodes[waitlist.pop()]
        if node.name not in transitiveParents:
            transitiveParents.add(node.name)
            waitlist.extend(node.parents)
    return transitiveParents


def getFilenamesFromLine(line):
    """extract all filenames from a line"""
    fname = None
    typ = EdgeType.NORMAL
    if line[:8] == "#include":
        fname = line.split()[1]
    else:
        m = re.search(
            r"^[a-zA-Z\.]*(config|Config|terminatingStatements)(?:Files|)\s*=\s*(.*)\s*",
            line,
        )
        if m is not None:
            fname = m.group(2)
            typ = EdgeType.SPECIAL
            if fname in ["true", "false"]:
                fname = None  # ignore some options with unusual name
        else:
            m = re.search(r"^specification\s*=\s*(.*)\s*", line)
            if m is not None:
                fname = m.group(1)
                typ = EdgeType.SPECIFICATION
    if not fname:
        return ([], typ)
    fnames = [name.strip().split("::")[0] for name in fname.split(",")]
    assert all(fnames), line
    return (fnames, typ)


def collectChildren(filename):
    children = {}
    try:
        multilineBuffer = ""
        for line in open(filename, "r"):
            if (
                not line.startswith(("#", "//"))
                and line.rstrip() != line
                and line.rstrip() != line[:-1]
            ):
                log(
                    "trailing whitespace in config '%s' in line '%s'"
                    % (filename, line.strip()),
                    level=2,
                )

            if line.strip().endswith("\\"):
                # multiline statement
                multilineBuffer += line.strip()[:-1]  # remove ending
                continue
            else:
                # single line or end of multiline statement
                multilineBuffer += line
                line = multilineBuffer
                multilineBuffer = ""  # reset

            (filenames, typ) = getFilenamesFromLine(line)
            for child in filenames:
                child = os.path.normpath(os.path.join(os.path.dirname(filename), child))
                if os.path.exists(child):
                    children[child] = typ
                else:
                    log(
                        "file '%s' referenced in '%s' does not exists"
                        % (child, filename)
                    )
    except UnicodeDecodeError:
        log("Cannot read file '%s'" % filename, level=3)
    return children


def listFiles(paths):
    """recursively traverse the given path and collect all files"""
    for path in paths:
        for root, _subFolders, files in os.walk(path):
            for item in files:
                if "README" not in item:  # filter unwanted files
                    yield os.path.normpath(os.path.join(root, item))


def writeDot(
    nodes,
    out,
    showChildDependencies=True,
    showParentDependencies=True,
    markDependencies=True,
    clusterNodes=False,
    clusterKeywords=False,
):
    """print dot file for limited set of nodes"""

    out.write("digraph configs {\n")
    out.write('graph [ranksep="%s" rankdir="LR"];\n' % (args.ranksep))
    out.write("node [shape=box]\n")

    allNodesDict, childDependencyNodes, parentDependencyNodes = determineDependencies(
        nodes, showChildDependencies, showParentDependencies
    )

    if clusterNodes or clusterKeywords:
        categoryMapping = defaultdict(dict)
        unclustered = []
        for name, node in sorted(allNodesDict.items()):
            if clusterNodes:
                categoryMapping[normPath(os.path.dirname(node.name))][name] = node
            else:
                matches = [
                    keyword
                    for keyword in clusterKeywords
                    if keyword.lower() in name.lower()
                ]
                if len(matches) == 1:
                    categoryMapping[matches[0]][name] = node
                else:
                    unclustered.append(node)
        for index, (category, categoryNodesDict) in enumerate(
            sorted(categoryMapping.items())
        ):
            out.write("subgraph cluster_%s {" % index)
            out.write('label = "%s";' % category)
            for name, node in sorted(categoryNodesDict.items()):
                isDependency = (
                    name in childDependencyNodes or name in parentDependencyNodes
                )
                out.write(
                    determineNode(
                        node, dependencyNode=isDependency and markDependencies
                    )
                )
            out.write("}\n")
        for node in unclustered:
            out.write(determineNode(node))
    else:
        for name, node in sorted(allNodesDict.items()):
            isDependency = name in childDependencyNodes or name in parentDependencyNodes
            out.write(
                determineNode(node, dependencyNode=isDependency and markDependencies)
            )

    if not markDependencies:
        nodes = allNodesDict

    for filename, v in sorted(nodes.items()):
        for child in sorted(v.children):
            if child in nodes:
                out.write(
                    '"%s" -> "%s";\n'
                    % (normPath(filename), normPath(nodes[child].name))
                )
            elif child in childDependencyNodes:
                out.write(
                    '"%s" -> "%s" [style="dashed" color="grey"];\n'
                    % (normPath(filename), normPath(childDependencyNodes[child].name))
                )
        for parent in v.parents:
            if parent not in nodes and parent in parentDependencyNodes:
                out.write(
                    '"%s" -> "%s" [style="dashed" color="grey"];\n'
                    % (normPath(parentDependencyNodes[parent].name), normPath(filename))
                )

    out.write("}\n")


def writeRSF(nodes, out, showChildDependencies=True, showParentDependencies=True):
    """print graph in Relational Standard Format(RSF)"""
    nodes = determineDependencies(nodes, showChildDependencies, showParentDependencies)[
        0
    ]
    for filename, v in sorted(nodes.items()):
        for child in v.children:
            if child in nodes:
                out.write(
                    "GRAPH %s %s 1\n"
                    % (normPath(filename), normPath(nodes[child].name))
                )


def normPath(f):
    return os.path.relpath(f, args.dir[0])


def determineDependencies(nodes, showChildDependencies, showParentDependencies):
    childDependencyNodes = {}
    if showChildDependencies:
        childDependencyNodes = {
            childNode.name: childNode
            for k, v in nodes.items()
            for childNode in v.childNodes
            if childNode.name not in nodes
        }
    parentDependencyNodes = {}
    if showParentDependencies:
        parentDependencyNodes = {
            parentNode.name: parentNode
            for k, v in nodes.items()
            for parentNode in v.parentNodes
            if parentNode.name not in nodes
        }

    allNodesDict = {}
    allNodesDict.update(nodes)
    allNodesDict.update(childDependencyNodes)
    allNodesDict.update(parentDependencyNodes)
    return (allNodesDict, childDependencyNodes, parentDependencyNodes)


def determineNode(node, dependencyNode=False):
    filename = node.name
    content = None
    if os.path.isfile(filename):
        content = ""
        try:
            content = re.sub("\n", "&#10;", open(filename, "r").read())
        except UnicodeDecodeError:
            log("Cannot read file '%s'" % filename, level=3)
        content = content.replace("\\", "\\\\").replace('"', '\\"')
    else:
        log("File does not exist: '%s'" % (filename))
    color = determineColor(node)

    tooltip = ""
    if content is not None:
        tooltip = 'tooltip = "%s"' % content

    style = ""
    if color is not None:
        style = 'style=filled fillcolor="%s"' % color
    if dependencyNode:
        style = 'style="setlinewidth(3),rounded" color="%s"' % color

    options = "%s %s" % (tooltip, style)
    result = '"%s"[%s]\n' % (normPath(node.name), options)
    return result


def determineColor(node):
    color = None
    if "unmaintained/" in node.name:
        color = "magenta"
    elif os.path.splitext(node.name)[1] != ".properties":
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
    parser = argparse.ArgumentParser(
        formatter_class=argparse.RawTextHelpFormatter,
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
    """,
    )
    parser.add_argument(
        "--dir",
        metavar="DIRECTORY",
        default=["config/", "test/config"],
        nargs="+",
        help="directory where the configuration files reside",
    )
    parser.add_argument(
        "--root",
        metavar="ROOT",
        default=None,
        nargs="+",
        help="configuration file for which a graph should be generated. "
        + "When specified, only files included (directly or indirectely) from this file are shown",
    )
    parser.add_argument(
        "--depend",
        metavar="DEPEND",
        default=None,
        help="configuration file for which a graph should be generated. "
        + "When specified, only files depending (directly or indirectely) on this file are shown",
    )
    parser.add_argument(
        "--filter",
        metavar="FILTER",
        default=None,
        nargs="+",
        help="String to filter nodes. "
        + "When specified, only matching nodes are shown. "
        + "When specified multiple times, nodes matching any filter will be shown.",
    )
    parser.add_argument(
        "--exclude",
        metavar="EXCLUDE",
        default=None,
        nargs="+",
        help="String to filter nodes. "
        + "When specified, only non-matching nodes are shown. "
        + "When specified multiple times, nodes matching any filter will be removed.",
    )
    parser.add_argument(
        "--ranksep",
        metavar="NUM",
        default=3,
        help="ranksep to use in the graphviz output file",
    )
    parser.add_argument(
        "--logLevel",
        metavar="LEVEL",
        default=1,
        help="a higher value enables more warnings, 0 is OFF",
    )
    parser.add_argument(
        "--rsf",
        action="store_true",
        help="output in Relational Standard Format (RSF) instead of graphviz",
    )
    parser.add_argument(
        "--showChildren", action="store_true", help="Show children of selected nodes"
    )
    parser.add_argument(
        "--showParents", action="store_true", help="Show parents of selected nodes"
    )
    parser.add_argument(
        "--samedep",
        action="store_true",
        help="Make dependency nodes look like regular nodes",
    )
    parser.add_argument(
        "--clusterNodes", action="store_true", help="Cluster nodes by their directory"
    )
    parser.add_argument(
        "--clusterKeywords",
        default=None,
        nargs="+",
        help="Cluster nodes based on the given keyword. If a config would belong to two given keywords, "
        + "it is in neither of the clusters, because the graphviz engine cannot make overlapping clusters.",
    )
    return parser.parse_args()


def getNodes(configDirectories):
    """collect all files and build a graph"""
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
    for name, node in nodes.items():
        for child in node.children:
            nodes[child].parents.append(name)
            nodes[child].parentNodes.append(node)
            node.childNodes.append(nodes[child])

    return nodes


def componentsSanityCheck(nodes):
    """Check for configuration files in the components folder that are never used."""
    for name, node in nodes.items():
        if not node.parents:
            if "components/" in name:
                log("Component file %s is unused!" % name, 2)
            if "includes/" in name:
                log("Include file %s is unused!" % name)
        elif "unmaintained/" not in name and all(
            "unmaintained/" in parent for parent in node.parents
        ):
            if "components/" in name:
                log("Component file %s is unmaintained!" % name, 2)
            if "includes/" in name:
                log("Include file %s is unmaintained!" % name, 2)


def transitiveReductionCheck(nodes):
    wlist = [node for name, node in nodes.items()]  # type is list of node names

    def filterChildren(node):  # returns generator over Node objects
        return (
            c
            for c in node.childNodes
            if c != node
            and node.childrenToType[c.name] in (EdgeType.NORMAL, EdgeType.SPECIFICATION)
        )

    def specificationOptionEqual(first, second):
        firstspecs = {
            c.name
            for c in first.childNodes
            if first.childrenToType[c.name] == EdgeType.SPECIFICATION
        }
        secondspecs = {
            c.name
            for c in second.childNodes
            if second.childrenToType[c.name] == EdgeType.SPECIFICATION
        }
        return firstspecs == secondspecs

    while wlist:
        current = wlist.pop()  # type is node name
        reach = {}  # key is reached element, value is by whom it was reached
        for c in filterChildren(current):
            reach[c] = current
        wlist2 = list(filterChildren(current))  # type is list of Node objects
        while wlist2:
            current2 = wlist2.pop()
            for c in filterChildren(current2):
                if c in reach.keys():
                    if reach[c] != current and current2 != current:
                        continue  # only consider cases where one of the includes is the common ancestor
                    if current2.childrenToType[
                        c.name
                    ] == EdgeType.SPECIFICATION and not specificationOptionEqual(
                        current2, reach[c]
                    ):
                        continue  # it is ok if the set of specs in "specification = " is overriden with a different set
                    log(
                        "included twice:%s\nFirst include by:\t%s\nSecond include by:\t%s\nCommon ancestor:\t%s\n"
                        % (c.name, reach[c].name, current2.name, current.name)
                    )
                else:
                    reach[c] = current2
                    if not c.name.endswith(".spc"):
                        # do not explore children of spc files
                        wlist2.append(c)


if __name__ == "__main__":
    args = parseArgs()

    global logLevel
    logLevel = int(args.logLevel)

    nodes = getNodes(args.dir)

    componentsSanityCheck(nodes)
    transitiveReductionCheck(nodes)

    children = []
    if args.root is not None:
        for root in args.root:
            if root not in nodes:
                log("Root file '%s' not found." % root)
            else:
                children.extend(getTransitiveChildren(root, nodes))
    nodesFromRoot = {k: v for k, v in nodes.items() if k in children}

    nodesFromDepend = {}
    if args.depend is not None:
        if args.depend not in nodes:
            log("Depend file '%s' not found." % args.depend)
        else:
            parents = getTransitiveParents(args.depend, nodes)
            nodesFromDepend = {k: v for k, v in nodes.items() if k in parents}

    # if any option is set, use its output
    if nodesFromRoot:
        nodes = nodesFromRoot
        if nodesFromDepend:
            nodes.update(nodesFromDepend)  # union of maps
    elif nodesFromDepend:
        nodes = nodesFromDepend

    if args.filter is not None:
        nodes = {
            k: v
            for k, v in nodes.items()
            if any(f.lower() in k.lower() for f in args.filter)
        }

    if args.exclude is not None:
        nodes = {
            k: v for k, v in nodes.items() if not any(f in k for f in args.exclude)
        }

    # write dot-output
    out = sys.stdout  # open("configViz.dot","w")
    if not args.rsf:
        writeDot(
            nodes,
            out,
            args.showChildren,
            args.showParents,
            not args.samedep,
            args.clusterNodes,
            args.clusterKeywords,
        )
    else:
        writeRSF(nodes, out, args.showChildren, args.showParents)

    exit(errorFound)
