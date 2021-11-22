import argparse
import json
import os
import glob
import sys
import webbrowser

from pathlib import Path


def relative_path(name):
    return str(Path(__file__).parent / name)


def create_arg_parser():
    parser = argparse.ArgumentParser(description="Transforms Worker logs to HTML.")

    parser.add_argument("--dir", type=str)

    return parser


def parse_jsons(path):
    jsons = list()
    for filename in glob.glob(os.path.join(path, "*.json")):
        with open(os.path.join(os.getcwd(), filename), "r") as f:
            jsons.append(json.loads(f.read()))
    return jsons


def collect_and_adapt_time_stamps(jsons):
    time_stamps = set()
    for worker in jsons:
        for message in worker:
            time_stamps.add(int(message["time"]))
    sorted_list = list(sorted(time_stamps))
    time_to_message_dict = {time - sorted_list[0]: list() for time in sorted_list}
    for worker in jsons:
        for message in worker:
            message["time"] -= sorted_list[0]
            time_to_message_dict[message["time"]].append(message)
    return time_to_message_dict


def find_all_worker_ids(jsons):
    ids = set()
    for worker in jsons:
        for message in worker:
            ids.add(message["id"])
    return list(sorted(ids))


def html_for_message(message):
    def wrap(tag, content, options=None):
        option_str = " " + (" ".join(options) if options else "")
        return f"<{tag}{option_str}>{content}</{tag}>"

    def from_post(blocks):
        return wrap("p", wrap("span", "&uarr;") + wrap("span", f"{blocks}"))

    def from_pre(blocks):
        return wrap("p", wrap("span", "&darr;") + wrap("span", f"{blocks}"))

    def from_block(direction, successor, predecessor, introduction="Received from "):
        if direction == "FORWARD":
            return from_pre(
                introduction + wrap("b", ", ".join(sorted(predecessor))) + ":"
            )
        else:
            return from_post(introduction + wrap("b", ", ".join(sorted(successor))) + ":")

    if not message:
        return "<div></div>"
    # msg_id = message["id"]
    predecessors = message["predecessors"]
    successors = message["successors"]
    condition = message["currentMap"]
    result = message["result"]
    code = message["code"].replace("]", "]\n").replace(";", ";\n")
    action = message["action"]

    from_part = from_block(action, successors, predecessors)

    condition_part = wrap("p", wrap("textarea", f"{condition}", options="readonly"))

    condition_name = "pre" if action == "FORWARD" else "post"

    to_blocks = from_block(
        action,
        predecessors,
        successors,
        introduction=f"Calculating new {condition_name}condition for ",
    )
    result_part = wrap("p", wrap("textarea", f"{result}", options="readonly"))

    return wrap(
        "div",
        f"{from_part}{condition_part}{to_blocks}{result_part}",
        options=[f'title="{code}"', f'class="{condition_name}cond"'],
    )


def html_dict_to_html_table(html_dict):
    table = "<tr>"
    for key in html_dict.keys():
        table += "<th>" + str(key) + "</th>"
    table += "</tr>"
    for values in zip(*html_dict.values()):
        row = "<tr>"
        for value in values:
            row += "<td>" + str(value) + "</td>"
        row += "</tr>"
        table += row
    return table


def main(argv=None):
    parser = create_arg_parser()
    args = parser.parse_args(argv)
    print("Parsing JSON...")
    jsons = parse_jsons(args.dir)
    print("Find all available ids...")
    ids = find_all_worker_ids(jsons)
    print("Found ids:", ids)
    print("Prepare dictionary for rendering the table...")
    render_dict = {worker_id: list() for worker_id in ids}
    print("Extract and normalize timestamps...")
    time_stamps = collect_and_adapt_time_stamps(jsons)
    print("Found timestamps:", list(time_stamps.keys()))
    print("Fill table...")
    for time, messages in time_stamps.items():
        unused_ids = set(ids)
        for message in messages:
            unused_ids.remove(message["id"])
            render_dict[message["id"]].append(message)
        for unused in unused_ids:
            render_dict[unused].append(None)
    print("Create table...")
    html_dict = {
        "time": list(time_stamps.keys()),
        **{
            block_id: list(map(html_for_message, render_dict[block_id]))
            for block_id in render_dict.keys()
        },
    }
    with open(relative_path("table.html")) as html:
        text = html.read().replace(
            "<!--<<<TABLE>>><!-->", html_dict_to_html_table(html_dict)
        )
        with open(relative_path("report.html"), "w+") as new_html:
            new_html.write(text)
    webbrowser.open(relative_path("report.html"))


if __name__ == "__main__":
    sys.exit(main())
