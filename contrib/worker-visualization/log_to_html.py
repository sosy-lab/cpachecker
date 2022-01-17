# This file is part of BenchExec, a framework for reliable benchmarking:
# https://github.com/sosy-lab/benchexec
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import argparse
import json
import sys
import webbrowser
import networkx as nx
import pydot

from airium import Airium
from pathlib import Path


def relative_path(name):
    return str(Path(__file__).parent / name)


def create_arg_parser():
    parser = argparse.ArgumentParser(description="Transforms Worker logs to HTML.")
    parser.add_argument("--file", type=str)
    return parser


def parse_jsons(file):
    with open(file, "r") as f:
        return json.loads(f.read())


def html_for_message(message, block_log):

    div = Airium()

    if not message:
        with div.div():
            div("")
        return str(div), ""

    infos = block_log[message["from"]]

    predecessors = ["none"] if "predecessors" not in infos else infos["predecessors"]
    successors = ["none"] if "successors" not in infos else infos["successors"]
    result = message["payload"] if message["payload"] else "no contents available"
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
                div(f"React to message from <strong>{sender}</strong>:")
        with div.p():
            receiver = ", ".join(receivers)
            div(f"Calculated new {direction} message for <strong>{receiver}</strong>")
        div.textarea(_t=result)

    return str(div)


def html_dict_to_html_table(all_messages, block_logs: dict):
    first_timestamp = int(all_messages[0]["timestamp"])
    timestamp_to_message = {}
    sorted_keys = list(sorted(block_logs.keys(), key=lambda x: int(x[1::])))
    index_dict = {}
    for index in enumerate(sorted_keys):
        index_dict[index[1]] = index[0]
    for message in all_messages:
        timestamp_to_message.setdefault(message["timestamp"] - first_timestamp, [""] * len(block_logs))[index_dict[message["from"]]] = message
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
                        klass = type_to_klass[msg["type"]] if msg["type"] in type_to_klass else "normal"
                        table.td(klass=klass, _t=html_for_message(msg, block_logs))

    return str(table)


def visualize(block_logs):
    g = nx.DiGraph()
    for key in block_logs:
        code = "\n".join(c for c in block_logs[key]["code"] if c)
        label = key + ":\n" + code if code else key
        g.add_node(key, shape="box", label=label)
    for key in block_logs:
        if "successors" in block_logs[key]:
            for successor in block_logs[key]["successors"]:
                g.add_edge(key, successor)
    nx.drawing.nx_pydot.write_dot(g, "graph.dot")
    (graph,) = pydot.graph_from_dot_file('graph.dot')
    graph.write_png('graph.png')


def main(argv=None):
    parser = create_arg_parser()
    args = parser.parse_args(argv)
    block_logs = parse_jsons(args.file)
    all_messages = []
    for key in block_logs:
        if "messages" in block_logs[key]:
            all_messages += block_logs[key]["messages"]
    if not all_messages:
        return
    all_messages = list(sorted(all_messages, key=lambda entry: (entry["timestamp"], entry["from"][1::])))
    with open(relative_path("table.html")) as html:
        text = html.read().replace(
            "<!--<<<TABLE>>><!-->", html_dict_to_html_table(all_messages, block_logs)
        )
        with open(relative_path("report.html"), "w+") as new_html:
            new_html.write(text)
    visualize(block_logs)
    webbrowser.open(relative_path("report.html"))


if __name__ == "__main__":
    sys.exit(main())
