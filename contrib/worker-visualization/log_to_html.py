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
import webbrowser
from pathlib import Path
from datetime import datetime

from typing import Dict

from airium import Airium

import networkx as nx
import pydot

ENCODING = "UTF-8"


def create_arg_parser():
    parser = argparse.ArgumentParser(description="Transforms Worker logs to HTML.")
    parser.add_argument(
        "--messages-json",
        help="Path to JSON file that contains messages sent during distributed block analysis.",
        default="output/block_analysis/block_analysis.json",
    )
    parser.add_argument(
        "--block-structure-json",
        help="Path to JSON file that contains the block structure used for"
        " distributed block analysis",
        default="output/block_analysis/blocks.json",
    )
    parser.add_argument(
        "-o",
        "--output",
        help="Output path for generated files",
        default="output/block_analysis",
    )
    return parser


def parse_args(argv):
    parser = create_arg_parser()
    args = parser.parse_args(argv)

    args.block_structure_json = Path(args.block_structure_json)
    if not args.block_structure_json.exists():
        raise ValueError(f"Path {args.block_structure_json} does not exist.")

    args.messages_json = Path(args.messages_json)
    if not args.messages_json.exists():
        raise ValueError(f"Path {args.messages_json} does not exist.")

    args.output = Path(args.output)
    return args


def parse_jsons(json_file: Path):
    with open(json_file, encoding=ENCODING) as inp:
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
        timestamp_to_message.setdefault(
            message["timestamp"] - first_timestamp, [""] * len(block_logs)
        )[index_dict[message["from"]]] = message
    headers = ["time"] + sorted_keys
    table = Airium()
    with table.table(klass="worker"):
        # header
        with table.tr(klass="header_row"):
            for key in headers:
                table.th(_t=f"{key}")

        # row values
        type_to_klass = {
            "BLOCK_POSTCONDITION": "precondition",
            "ERROR_CONDITION": "postcondition",
            "ERROR_CONDITION_UNREACHABLE": "postcondition",
        }
        for timestamp, messages in timestamp_to_message.items():
            with table.tr():
                table.td(_t=str(timestamp))
                for msg in messages:
                    if not msg:
                        table.td()
                    else:
                        klass = type_to_klass.get(msg["type"], "normal")
                        table.td(klass=klass, _t=html_for_message(msg, block_logs))

    return str(table)


def visualize_blocks(
    block_structure_file: Path,
    output_path: Path,
    output_dot_name="graph.dot",
    output_png_name="graph.png",
):
    g = nx.DiGraph()
    block_logs = parse_jsons(block_structure_file)
    for key in block_logs:
        code = "\n".join(c for c in block_logs[key]["code"] if c)
        label = key + ":\n" + code if code else key
        g.add_node(key, shape="box", label=f'"{label}"')
    for key in block_logs:
        if "successors" in block_logs[key]:
            for successor in block_logs[key]["successors"]:
                g.add_edge(key, successor)

    output_path.mkdir(parents=True, exist_ok=True)
    graph_dot = output_path / output_dot_name
    nx.drawing.nx_pydot.write_dot(g, str(graph_dot))
    (graph,) = pydot.graph_from_dot_file(str(graph_dot))
    graph.write_png(str(output_path / output_png_name))


def export_messages_table(
    *,
    all_messages,
    block_logs,
    output_path,
    report_filename="report.html",
    message_table_html_file=None,
    message_table_css_file=None,
):
    if message_table_html_file is None:
        message_table_html_file = Path(__file__).parent / "table.html"
    if message_table_css_file is None:
        message_table_css_file = Path(__file__).parent / "table.css"

    for message in all_messages:
        # 2022-03-10 14:44:07.0318755
        message["timestamp"] = int(
            datetime.strptime(message["timestamp"], "%Y-%m-%d %H:%M:%S.%f").timestamp()
        )
    all_messages = sorted(
        all_messages, key=lambda entry: (entry["timestamp"], entry["from"][1::])
    )

    output_path.mkdir(parents=True, exist_ok=True)
    with open(message_table_html_file, encoding=ENCODING) as html:
        with open(message_table_css_file, encoding=ENCODING) as css:
            text = (
                html.read()
                .replace(
                    "<!--<<<TABLE>>><!-->",
                    html_dict_to_html_table(all_messages, block_logs),
                )
                .replace("/*CSS*/", css.read())
            )
            output_file = output_path / report_filename
            with open(output_file, "w+", encoding=ENCODING) as new_html:
                new_html.write(text)
    return output_file


def visualize_messages(messages_file: Path, output_path: Path):
    block_logs = parse_jsons(messages_file)
    all_messages = []
    for key in block_logs:
        if "messages" in block_logs[key]:
            all_messages += block_logs[key]["messages"]
    if not all_messages:
        return

    export_filename = export_messages_table(
        all_messages=all_messages, block_logs=block_logs, output_path=output_path
    )
    webbrowser.open(str(export_filename))


def main(argv=None):
    if argv is None:
        argv = sys.argv[1:]
    args = parse_args(argv)
    output_path: Path = args.output

    visualize_blocks(
        block_structure_file=args.block_structure_json, output_path=output_path
    )

    visualize_messages(messages_file=args.messages_json, output_path=output_path)


if __name__ == "__main__":
    sys.exit(main())
