# This file is part of BenchExec, a framework for reliable benchmarking:
# https://github.com/sosy-lab/benchexec
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import argparse
import json
import os
import glob
import sys
import webbrowser
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
    result = message["payload"]
    direction = message["type"]

    is_forward = direction == "BLOCK_POSTCONDITION"
    senders = predecessors if is_forward else successors
    receivers = successors if is_forward else predecessors
    condition_name = "precondition" if is_forward else "postcondition"
    arrow = "<big>" + ("&darr;" if is_forward else "&uarr;") + "</big>"
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
            div(f"Calculated new {condition_name} for <strong>{receiver}</strong>")
        div.textarea(_t=result)

    return str(div)


def html_dict_to_html_table(all_messages, block_logs: dict):
    first_timestamp = int(all_messages[0]["timestamp"])
    timestamp_to_message = {}
    for message in all_messages:
        timestamp_to_message.setdefault(message["timestamp"] - first_timestamp, [""] * len(block_logs))[int(message["from"][1::])] = message
    headers = ["time"] + list(sorted(block_logs.keys(), key=lambda x: int(x[1::])))
    table = Airium()
    with table.table(klass="worker"):
        # header
        with table.tr(klass='header_row'):
            for key in headers:
                table.th(_t=f'{key}')

        # row values
        for timestamp in timestamp_to_message:
            with table.tr():
                table.td(_t=str(timestamp))
                messages = timestamp_to_message[timestamp]
                for msg in messages:
                    if not msg:
                        table.td()
                    else:
                        klass = "postcondition" if msg["type"] == "ERROR_CONDITION" else "precondition"
                        table.td(klass=klass, _t=html_for_message(msg, block_logs))

    return str(table)


def main(argv=None):
    parser = create_arg_parser()
    args = parser.parse_args(argv)
    block_logs = parse_jsons(args.file)
    all_messages = []
    for key in block_logs:
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
    webbrowser.open(relative_path("report.html"))


if __name__ == "__main__":
    sys.exit(main())
