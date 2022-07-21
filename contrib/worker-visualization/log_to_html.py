#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import argparse
import json
import sys
from pathlib import Path
from datetime import datetime

from typing import Dict

from airium import Airium

import webbrowser
import networkx as nx
import pydot

block_analysis_file_name = "block_analysis.json"
summary_file_name = "blocks.json"


def create_arg_parser():
    parser = argparse.ArgumentParser(description="Transforms Worker logs to HTML.")
    parser.add_argument("-d", "--directory",
                        help="set the path to the logs of worker (adjustable block "
                             "analysis) usually found here: output/block_analysis",
                        default="output/block_analysis")
    return parser


def parse_jsons(json_file: Path):
    with open(json_file) as inp:
        return json.load(inp)


def html_for_message(message, block_log: Dict[str, str]):
    div = Airium()

    if not message:
        with div.div():
            div("")
        return str(div), ""

    infos = block_log[message["from"]]

    predecessors = infos.get("predecessors", [])
    successors = infos.get("successors", [])
    result = message.get("payload", "no contents available")
    direction = message["type"]
    arrow = "-"
    senders = ["all"]
    receivers = ["all"]
    if direction == "BLOCK_POSTCONDITION":
        receivers = successors
        senders = predecessors
        arrow = "&darr;"
    elif direction == "ERROR_CONDITION":
        receivers = predecessors
        senders = successors
        arrow = "&uarr;"
    elif direction == "ERROR_CONDITION_UNREACHABLE":
        receivers = ["all"]
        senders = successors
        arrow = "&uarr;"
    elif direction == "FOUND_RESULT":
        senders = [message["from"]]

    code = "\n".join([x for x in infos["code"] if x])

    with div.div(title=code):
        with div.p():
            with div.span():
                div(arrow)
            with div.span():
                sender = "self"
                if senders:
                    sender = ", ".join(senders)
                else:
                    sender = "None"
                div(f"React to message from <strong>{sender}</strong>:")
        with div.p():
            if receivers:
                receiver = ", ".join(receivers)
            else:
                receiver = "None"
            div(f"Calculated new {direction} message for <strong>{receiver}</strong>")
        div.textarea(_t=result)

    return str(div)


def html_dict_to_html_table(all_messages, block_logs: Dict[str, str]):
    first_timestamp = int(all_messages[0]["timestamp"])
    timestamp_to_message = {}
    sorted_keys = sorted(block_logs.keys(), key=lambda x: int(x[1::]))
    index_dict = {}
    for index in enumerate(sorted_keys):
        index_dict[index[1]] = index[0]
    for message in all_messages:
        timestamp_to_message.setdefault(message["timestamp"] - first_timestamp, [""] * len(block_logs))[
            index_dict[message["from"]]] = message
    headers = ["time"] + sorted_keys
    table = Airium()
    with table.table(klass="worker"):
        # header
        with table.tr(klass='header_row'):
            for key in headers:
                table.th(_t=f'{key}')

        # row values
        type_to_klass = {
            "BLOCK_POSTCONDITION": "precondition",
            "ERROR_CONDITION": "postcondition",
            "ERROR_CONDITION_UNREACHABLE": "postcondition"
        }
        for timestamp in timestamp_to_message:
            with table.tr():
                table.td(_t=str(timestamp))
                messages = timestamp_to_message[timestamp]
                for msg in messages:
                    if not msg:
                        table.td()
                    else:
                        klass = type_to_klass.get(msg["type"], "normal")
                        table.td(klass=klass, _t=html_for_message(msg, block_logs))

    return str(table)


def visualize(output_path: Path):
    g = nx.DiGraph()
    block_logs = parse_jsons(output_path / summary_file_name)
    for key in block_logs:
        code = "\n".join(c for c in block_logs[key]["code"] if c)
        label = key + ":\n" + code if code else key
        g.add_node(key, shape="box", label=label)
    for key in block_logs:
        if "successors" in block_logs[key]:
            for successor in block_logs[key]["successors"]:
                g.add_edge(key, successor)

    graph_dot = output_path / "graph.dot"
    nx.drawing.nx_pydot.write_dot(g, str(graph_dot))
    (graph,) = pydot.graph_from_dot_file(str(graph_dot))
    graph.write_png(str(output_path / "graph.png"))


def main(argv=None):
    parser = create_arg_parser()
    args = parser.parse_args(argv)
    output_path = Path(args.directory)
    if not output_path.exists():
        print(f"Path {output_path} does not exist.", file=sys.stderr)
        return 1
    block_logs = parse_jsons(output_path / block_analysis_file_name)
    all_messages = []
    for key in block_logs:
        if "messages" in block_logs[key]:
            all_messages += block_logs[key]["messages"]
    if not all_messages:
        return
    for message in all_messages:
        # 2022 - 03 - 10 14: 44:07.031875
        message["timestamp"] = int(datetime.strptime(message["timestamp"], '%Y-%m-%d %H:%M:%S.%f').timestamp())
    all_messages = sorted(all_messages, key=lambda entry: (entry["timestamp"], entry["from"][1::]))
    with open("table.html") as html:
        with open("table.css") as css:
            text = html.read().replace(
                "<!--<<<TABLE>>><!-->", html_dict_to_html_table(all_messages, block_logs)
            ).replace("/*CSS*/", css.read())
            with open(output_path / "report.html", "w+") as new_html:
                new_html.write(text)
    visualize(output_path)
    webbrowser.open(str(output_path / "report.html"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
