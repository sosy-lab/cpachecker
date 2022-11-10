#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

from argparse import ArgumentParser, Namespace
from pathlib import Path
import shutil
from typing import Dict, Sequence
import os
from logging import log
import re
import sys
from enum import Enum
from typing_extensions import LiteralString


class EdgeType(Enum):
    NORMAL = 0  # dependencies caused by "#include ..."
    SPECIAL = 1  # things like "parallelAlgorithm.configFiles=..."
    SPECIFICATION = 2  # dependencies caused by "specification = ..."


def get_parser() -> ArgumentParser:

    parser = ArgumentParser("configcp.py")
    parser.add_argument("--root", help="The file to start copying from.", required=True)
    parser.add_argument(
        "-f",
        "--filter",
        action="store",
        help="Regex which files should be considered while copying.",
    )
    parser.add_argument(
        "--dir",
        metavar="DIRECTORY",
        default="config/",
        nargs=1,
        help="directory where the configuration files reside",
    )
    parser.add_argument(
        "--dest",
        action="store",
        required=True,
        type=str,
        help="The destination path template for the files."
        "Use {name} to reference the filename of the found file."
        "Use {path} to reference the path part to the filename relative to DIRECTORY.",
    )
    parser.add_argument(
        "--update",
        action="store_true",
        help="Update dependencies inside the files. DOES NOT keep multiline statements. "
        "Currently, the relative paths of the files are kept from the originals."
        "If you want to have a working configuration tree execute with: "
        "--dest path/to/some/dir/{path}/{name}",
    )
    parser.add_argument(
        "--replace",
        action="store",
        nargs=2,
        help="Replace <1> with <2> in filenames. <1> is treated as regex.",
    )
    cmd = parser.add_mutually_exclusive_group(required=True)
    cmd.add_argument("-cp", "--copy", action="store_true")
    cmd.add_argument("-mv", "--move", action="store_true")
    cmd.add_argument("-p", "--print", action="store_true")

    return parser


def exec_parser_cmd(args: Namespace, src: os.PathLike[str], dest: os.PathLike[str]):
    if args.copy:
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        shutil.copy(src, dest)
    if args.move:
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        shutil.move(src, dest)
    if args.print:
        print(src, " -> ", dest)


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
                    2,
                    "trailing whitespace in config '%s' in line '%s'"
                    % (filename, line.strip()),
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
                        1,
                        "file '%s' referenced in '%s' does not exists"
                        % (child, filename),
                    )
    except UnicodeDecodeError:
        log(3, "Cannot read file '%s'" % filename)
    return children


def updateChildren(filename, updates: Dict[str, str]):
    try:
        multilineBuffer = ""
        with open(filename, "r+") as fd:
            lines = fd.readlines()
            print(filename)
            newlines = []
            while lines:
                line = lines.pop(0)

                if line.strip().endswith("\\"):
                    # multiline statement
                    multilineBuffer += line.strip()[:-1]  # remove ending
                    continue
                else:
                    # single line or end of multiline statement
                    multilineBuffer += line
                    line = multilineBuffer
                    multilineBuffer = ""  # reset

                (filenames, _) = getFilenamesFromLine(line)

                newline = line
                for fname in filenames:
                    print("Replacing: ", fname)
                    try:
                        name = Path(fname).name
                        replacement_path = updates[name]
                        replacement = Path(replacement_path).name
                        newline = re.sub(name, replacement, newline)
                        print(newline)
                    except KeyError as e:
                        print(e)
                        pass

                newlines.append(newline)
            fd.seek(0)
            fd.writelines(newlines)
            fd.flush()

    except UnicodeDecodeError:
        log(3, "Cannot read file '%s'" % filename)


def listFiles(paths):
    """recursively traverse the given path and collect all files"""
    for path in paths:
        for root, _subFolders, files in os.walk(path):
            for item in files:
                if "README" not in item:  # filter unwanted files
                    yield os.path.normpath(os.path.join(root, item))


def getNodes(configDirectories) -> Dict[LiteralString, Node]:
    """collect all files and build a graph"""
    nodes: Dict[LiteralString, Node] = {}

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


def matchesFilter(filter_exp: str, arg: str) -> bool:
    path = Path(arg)
    return re.match(filter_exp, path.name) is not None


def main(argv: Sequence[str]):

    parser = get_parser()
    args = parser.parse_args(argv)

    nodes = getNodes(args.dir)

    dirpath = Path(args.dir[0])

    children = []
    if args.root not in nodes:
        log(40, "Root file '%s' not found." % args.root)
    else:
        children.extend(getTransitiveChildren(args.root, nodes))

    nodesFromRoot = {k: v for k, v in nodes.items() if k in children}

    if args.filter:
        nodesFromRoot = {
            k: v for k, v in nodesFromRoot.items() if matchesFilter(args.filter, k)
        }

    updates: Dict[str, str] = {}
    for _name in nodesFromRoot.keys():
        path = Path(_name)
        relative = path.relative_to(dirpath)
        name = path.name
        if args.replace:
            name = re.sub(args.replace[0], args.replace[1], name)
        dest = args.dest.format_map({"path": str(relative.parent), "name": name})

        updates[path.name] = dest
        exec_parser_cmd(args, path, dest)

    if args.update:
        print(updates)
        for fname in updates.values():
            updateChildren(fname, updates)


if __name__ == "__main__":
    main(sys.argv[1:])
