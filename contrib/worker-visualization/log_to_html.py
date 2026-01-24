#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

"""
Worker Log Visualization Tool

This module transforms distributed block analysis worker logs into an interactive
HTML visualization with filtering capabilities and a clean, professional interface.
"""

import argparse
import json
import sys
import webbrowser
from pathlib import Path
from typing import Dict, List, Any, Optional
from dataclasses import dataclass
from enum import Enum

import networkx as nx
import pydot

ENCODING = "UTF-8"


class MessageType(Enum):
    """Enumeration of message types in the distributed block analysis."""
    POST_CONDITION = "POST_CONDITION"
    VIOLATION_CONDITION = "VIOLATION_CONDITION"
    FOUND_RESULT = "FOUND_RESULT"


@dataclass
class Message:
    """Data class representing a worker message."""
    timestamp: int
    sender_id: str
    message_type: MessageType
    content: Dict[str, Any]
    filename: str
    predecessors: List[str]
    successors: List[str]
    code: str

    @classmethod
    def from_json(cls, json_data: Dict[str, Any], block_logs: Dict[str, Any]) -> 'Message':
        """Create a Message instance from JSON data."""
        header = json_data["header"]
        sender_id = header["senderId"]
        block_info = block_logs.get(sender_id, {})

        return cls(
            timestamp=int(header["timestamp"]),
            sender_id=sender_id,
            message_type=MessageType(header["messageType"]),
            content=json_data.get("content", {}),
            filename=json_data.get("filename", ""),
            predecessors=block_info.get("predecessors", []),
            successors=block_info.get("successors", []),
            code="\n".join(x for x in block_info.get("code", []) if x)
        )


def create_arg_parser() -> argparse.ArgumentParser:
    """Create and configure the command-line argument parser."""
    parser = argparse.ArgumentParser(
        description="Transform Worker logs from distributed block analysis to interactive HTML visualization.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s
  %(prog)s --messages-json output/messages --block-structure-json output/blocks.json
  %(prog)s -o visualization --export-keys state predicates
        """
    )
    parser.add_argument(
        "--messages-json",
        help="Path to directory containing JSON files sent during distributed block analysis.",
        default="output/block_analysis/messages",
    )
    parser.add_argument(
        "--block-structure-json",
        help="Path to JSON file that contains the block structure used for distributed block analysis.",
        default="output/block_analysis/blocks.json",
    )
    parser.add_argument(
        "-o",
        "--output",
        help="Output path for generated files.",
        default="output/block_analysis",
    )
    parser.add_argument(
        "--export-keys",
        help="Space separated list of keys to export from the messages. If not set, all keys are exported.",
        nargs="+",
        action="extend",
        dest="export_keys",
    )
    return parser


def parse_args(argv: List[str]) -> argparse.Namespace:
    """Parse and validate command-line arguments."""
    parser = create_arg_parser()
    args = parser.parse_args(argv)

    args.block_structure_json = Path(args.block_structure_json)
    if not args.block_structure_json.exists():
        raise ValueError(f"Block structure file does not exist: {args.block_structure_json}")

    args.messages_json = Path(args.messages_json)
    if not args.messages_json.is_dir():
        raise ValueError(f"Messages directory does not exist: {args.messages_json}")

    args.output = Path(args.output)
    args.export_keys = args.export_keys or []
    return args


def load_json_file(json_file: Path) -> Dict[str, Any]:
    """Load and parse a JSON file with error handling."""
    try:
        with open(json_file, encoding=ENCODING) as file:
            return json.load(file)
    except json.JSONDecodeError as e:
        print(f"WARNING: JSON decoding error in {json_file}: {e}", file=sys.stderr)
        return {}
    except Exception as e:
        print(f"WARNING: Error reading {json_file}: {e}", file=sys.stderr)
        return {}


def filter_content_by_keys(content: Dict[str, Any], export_keys: List[str]) -> Dict[str, Any]:
    """Filter message content based on export keys."""
    if not export_keys:
        return content

    filtered = {}
    for key, value in content.items():
        for export_key in export_keys:
            if export_key in key:
                filtered[key.replace(export_key, "")] = value
                break
    return filtered


def generate_message_html(message: Optional[Dict[str, Any]], block_logs: Dict[str, Any],
                          export_keys: List[str]) -> str:
    """Generate HTML representation of a single message."""
    if not message:
        return "<div></div>"

    header = message["header"]
    sender_id = header["senderId"]
    message_type = header["messageType"]
    msg_id = message["filename"]

    block_info = block_logs.get(sender_id, {})
    predecessors = block_info.get("predecessors", [])
    successors = block_info.get("successors", [])
    code = "\n".join(x for x in block_info.get("code", []) if x)
    content = message.get("content", {})

    # Determine message direction and participants
    arrow, senders, receivers = "-", ["all"], ["all"]
    if message_type == "POST_CONDITION":
        arrow, senders, receivers = "&darr;", predecessors or ["Self"], successors
    elif message_type == "VIOLATION_CONDITION":
        arrow, senders, receivers = "&uarr;", successors or ["Self"], predecessors
    elif message_type == "FOUND_RESULT":
        senders = [message.get("from", "Self")]

    # Filter content based on export keys
    filtered_content = filter_content_by_keys(content, export_keys)
    content_json = json.dumps(filtered_content, indent=2)

    # Build HTML
    html = f"""
    <div class="message-card" title="{sender_id}:\n{code}" data-sender="{sender_id}" data-type="{message_type}">
        <div class="message-header">
            <span class="message-arrow">{arrow}</span>
            <span class="message-info">
                From <strong>{', '.join(senders)}</strong>
                <span class="message-id">(ID: {msg_id[1:-5]})</span>
            </span>
        </div>
        <div class="message-direction">
            To <strong>{', '.join(receivers) if receivers else 'None'}</strong>
            <span class="message-type-badge">{message_type.replace('_', ' ')}</span>
        </div>
        <details class="message-content">
            <summary>View Content</summary>
            <pre class="content-json">{content_json}</pre>
        </details>
    </div>
    """
    return html


def generate_html_table(messages: List[Dict[str, Any]], block_logs: Dict[str, Any],
                        export_keys: List[str]) -> str:
    """Generate HTML table from messages."""
    if not messages:
        return "<p>No messages to display.</p>"

    # Calculate relative timestamps
    first_timestamp = int(messages[0]["header"]["timestamp"])

    # Organize messages by timestamp and sender
    sorted_block_ids = sorted(block_logs.keys())
    block_id_to_index = {block_id: i for i, block_id in enumerate(sorted_block_ids)}

    timestamp_to_messages = {}
    for message in messages:
        sender_id = message["header"]["senderId"]
        if sender_id not in block_id_to_index:
            continue

        relative_time = message["header"]["timestamp"] - first_timestamp
        if relative_time not in timestamp_to_messages:
            timestamp_to_messages[relative_time] = [None] * len(sorted_block_ids)

        timestamp_to_messages[relative_time][block_id_to_index[sender_id]] = message

    # Message type to CSS class mapping
    type_to_class = {
        "POST_CONDITION": "post-condition",
        "VIOLATION_CONDITION": "violation-condition",
        "FOUND_RESULT": "found-result"
    }

    # Build table HTML
    html_parts = ['<table class="worker-table" id="messageTable">']

    # Table header
    html_parts.append('<thead><tr class="header-row">')
    html_parts.append('<th class="time-column">Time</th>')
    for block_id in sorted_block_ids:
        html_parts.append(f'<th class="block-column" data-block="{block_id}">{block_id}</th>')
    html_parts.append('</tr></thead>')

    # Table body
    html_parts.append('<tbody>')
    for timestamp in sorted(timestamp_to_messages.keys()):
        row_messages = timestamp_to_messages[timestamp]
        html_parts.append(f'<tr data-timestamp="{timestamp}">')
        html_parts.append(f'<td class="time-cell">{timestamp}</td>')

        for msg in row_messages:
            if msg is None:
                html_parts.append('<td class="empty-cell"></td>')
            else:
                msg_type = msg["header"]["messageType"]
                css_class = type_to_class.get(msg_type, "normal")
                cell_html = generate_message_html(msg, block_logs, export_keys)
                html_parts.append(
                    f'<td class="message-cell {css_class}" data-type="{msg_type}">{cell_html}</td>')

        html_parts.append('</tr>')
    html_parts.append('</tbody>')
    html_parts.append('</table>')

    return '\n'.join(html_parts)


def visualize_block_graph(
        block_structure_file: Path,
        output_path: Path,
        output_dot_name: str = "graph.dot",
        output_png_name: str = "graph.png",
) -> None:
    """Generate block structure graph visualization."""
    block_logs = load_json_file(block_structure_file)
    if not block_logs:
        print("WARNING: No block structure data found", file=sys.stderr)
        return

    graph = nx.DiGraph()

    # Add nodes with code labels
    for block_id, block_info in block_logs.items():
        code_parts = [c.replace('"', "'") for c in block_info.get("code", [])]
        code = "\n".join(c for c in code_parts if c)

        # Truncate long code snippets
        if len(code) > 1000:
            code = code[:1000] + "..."

        label = f"{block_id}:\n{code}" if code else block_id
        graph.add_node(block_id, shape="box", label=f'"{label}"')

    # Add edges
    for block_id, block_info in block_logs.items():
        for successor in block_info.get("successors", []):
            graph.add_edge(block_id, successor)

    # Create output directory
    output_path.mkdir(parents=True, exist_ok=True)

    # Write DOT and PNG files
    graph_dot_path = output_path / output_dot_name
    nx.drawing.nx_pydot.write_dot(graph, str(graph_dot_path))

    try:
        (pydot_graph,) = pydot.graph_from_dot_file(str(graph_dot_path))
        pydot_graph.write_png(str(output_path / output_png_name))
        print(f"Block graph visualization saved to {output_path / output_png_name}")
    except Exception as e:
        print(f"WARNING: Failed to generate PNG from DOT file: {e}", file=sys.stderr)


def generate_html_report(
        messages: List[Dict[str, Any]],
        block_logs: Dict[str, Any],
        output_path: Path,
        export_keys: Optional[List[str]] = None,
        report_filename: str = "report.html",
) -> Path:
    """Generate the complete HTML report with embedded styles and scripts."""
    export_keys = export_keys or []

    # Sort messages by timestamp and sender
    for message in messages:
        message["header"]["timestamp"] = int(message["header"]["timestamp"])

    messages.sort(key=lambda m: (m["header"]["timestamp"], m["header"]["senderId"][1:]))

    # Generate table HTML
    table_html = generate_html_table(messages, block_logs, export_keys)

    # Get unique block IDs and message types for filters
    block_ids = sorted(block_logs.keys())
    message_types = ["POST_CONDITION", "VIOLATION_CONDITION", "FOUND_RESULT"]

    # Build complete HTML document
    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Worker Events - Block Analysis Visualization</title>
    <style>
        {get_embedded_css()}
    </style>
</head>
<body>
    <div class="container">
        <header class="header">
            <h1>Worker Events Visualization</h1>
            <p class="subtitle">Distributed Block Analysis Messages</p>
        </header>
        
        <div class="controls">
            <div class="control-group">
                <button id="resetFiltersBtn" class="btn btn-secondary">
                    <span>🔄</span> Reset Filters
                </button>
            </div>
            
            <div class="filter-panel">
                <div class="filter-section">
                    <label>Filter by Message Type:</label>
                    <div class="filter-buttons">
                        {' '.join(f'<button class="filter-btn active" data-filter-type="{mt}">{mt.replace("_", " ")}</button>' for mt in message_types)}
                    </div>
                </div>
                
                <div class="filter-section">
                    <label>Filter by Block ID:</label>
                    <div class="filter-input-group">
                        <input type="text" id="blockFilter" placeholder="Enter block ID..." />
                        <button id="clearBlockFilter" class="btn-clear">✕</button>
                    </div>
                </div>
                
                <div class="filter-section">
                    <label>Search Message Content:</label>
                    <div class="filter-input-group">
                        <input type="text" id="contentSearch" placeholder="Search in message content..." />
                        <button id="clearContentSearch" class="btn-clear">✕</button>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="stats-panel">
            <div class="stat-card">
                <span class="stat-label">Total Messages:</span>
                <span class="stat-value" id="totalMessages">{len(messages)}</span>
            </div>
            <div class="stat-card">
                <span class="stat-label">Visible Messages:</span>
                <span class="stat-value" id="visibleMessages">{len(messages)}</span>
            </div>
            <div class="stat-card">
                <span class="stat-label">Block Count:</span>
                <span class="stat-value">{len(block_ids)}</span>
            </div>
        </div>
        
        <div class="table-container">
            {table_html}
        </div>
    </div>
    
    <!-- Fixed floating button for block graph -->
    <button class="floating-graph-btn" id="floatingGraphBtn" title="View Block Graph">
        📊
    </button>
    
    <div class="modal" id="graphModal">
        <div class="modal-overlay" id="modalOverlay"></div>
        <div class="modal-content">
            <button class="modal-close" id="modalClose">&times;</button>
            <h2>Block Structure Graph</h2>
            <div class="modal-body">
                <img src="graph.png" alt="Block Structure Graph" id="graphImage" />
            </div>
        </div>
    </div>
    
    <script>
        {get_embedded_javascript()}
    </script>
</body>
</html>
"""

    # Write output file
    output_path.mkdir(parents=True, exist_ok=True)
    output_file = output_path / report_filename
    with open(output_file, "w", encoding=ENCODING) as f:
        f.write(html_content)

    print(f"HTML report generated: {output_file}")
    return output_file


def load_and_process_messages(
        message_dir: Path,
        block_structure_json: Path,
        output_path: Path,
        export_keys: Optional[List[str]] = None,
) -> Optional[Path]:
    """Load messages from directory and generate visualization."""
    all_messages = []
    hash_code = None

    # Get sorted list of JSON message files
    try:
        json_files = sorted(
            [f.name for f in message_dir.iterdir() if f.is_file() and f.suffix == ".json"],
            key=lambda name: name[1:-5] if name[0].isdigit() else name
        )
    except Exception as e:
        print(f"ERROR: Failed to read message directory: {e}", file=sys.stderr)
        return None

    # Load block structure
    block_logs = load_json_file(block_structure_json)
    if not block_logs:
        print("ERROR: Failed to load block structure", file=sys.stderr)
        return None

    # Load all message files
    for json_filename in json_files:
        message_data = load_json_file(message_dir / json_filename)
        if not message_data:
            continue

        header = message_data.get("header", {})

        # Filter by identifier hash
        current_hash = header.get("identifier", "UNKNOWN")
        if hash_code is None:
            hash_code = current_hash

        if current_hash == hash_code:
            message_data["filename"] = json_filename
            all_messages.append(message_data)

        if "identifier" not in header:
            print(f"WARNING: Missing identifier in {json_filename}", file=sys.stderr)

    if not all_messages:
        print("WARNING: No messages found to visualize", file=sys.stderr)
        return None

    print(f"Loaded {len(all_messages)} messages from {len(json_files)} files")

    # Generate HTML report
    return generate_html_report(
        messages=all_messages,
        block_logs=block_logs,
        output_path=output_path,
        export_keys=export_keys,
    )


def get_embedded_css() -> str:
    """Return the embedded CSS for the HTML report."""
    return """
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: #f5f5f5;
            min-height: 100vh;
            padding: 20px;
            color: #333;
        }
        
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        }
        
        .header {
            background: #2c3e50;
            color: white;
            padding: 30px 40px;
            text-align: center;
            border-bottom: 3px solid #34495e;
        }
        
        .header h1 {
            font-size: 2em;
            margin-bottom: 8px;
            font-weight: 600;
        }
        
        .subtitle {
            font-size: 1em;
            opacity: 0.9;
            font-weight: 400;
        }
        
        .controls {
            padding: 24px 40px;
            background: #fafafa;
            border-bottom: 1px solid #e0e0e0;
        }
        
        .control-group {
            display: flex;
            gap: 12px;
            margin-bottom: 20px;
        }
        
        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            font-size: 0.95em;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s ease;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }
        
        .btn-primary {
            background: #3498db;
            color: white;
        }
        
        .btn-primary:hover {
            background: #2980b9;
        }
        
        .btn-secondary {
            background: #95a5a6;
            color: white;
        }
        
        .btn-secondary:hover {
            background: #7f8c8d;
        }
        
        .filter-panel {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }
        
        .filter-section {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        
        .filter-section label {
            font-weight: 500;
            color: #555;
            font-size: 0.9em;
        }
        
        .filter-buttons {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }
        
        .filter-btn {
            padding: 6px 14px;
            border: 1px solid #d0d0d0;
            background: white;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.2s ease;
            font-size: 0.85em;
            font-weight: 500;
        }
        
        .filter-btn.active {
            background: #3498db;
            color: white;
            border-color: #3498db;
        }
        
        .filter-btn:hover {
            border-color: #3498db;
        }
        
        .filter-input-group {
            display: flex;
            gap: 8px;
            align-items: center;
        }
        
        .filter-input-group input {
            flex: 1;
            padding: 8px 12px;
            border: 1px solid #d0d0d0;
            border-radius: 4px;
            font-size: 0.9em;
            transition: all 0.2s ease;
        }
        
        .filter-input-group input:focus {
            outline: none;
            border-color: #3498db;
            box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.1);
        }
        
        .btn-clear {
            padding: 8px 12px;
            background: #e74c3c;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 0.9em;
            transition: all 0.2s ease;
        }
        
        .btn-clear:hover {
            background: #c0392b;
        }
        
        .stats-panel {
            display: flex;
            gap: 16px;
            padding: 20px 40px;
            background: #fafafa;
            border-bottom: 1px solid #e0e0e0;
        }
        
        .stat-card {
            flex: 1;
            padding: 14px 18px;
            background: white;
            border-radius: 4px;
            border: 1px solid #e0e0e0;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }
        
        .stat-label {
            font-size: 0.8em;
            color: #777;
            font-weight: 500;
        }
        
        .stat-value {
            font-size: 1.6em;
            font-weight: 600;
            color: #2c3e50;
        }
        
        .table-container {
            padding: 20px;
            overflow-x: auto;
        }
        
        .worker-table {
            width: 100%;
            border-collapse: separate;
            border-spacing: 0;
            font-size: 0.85em;
        }
        
        .worker-table thead {
            position: sticky;
            top: 0;
            z-index: 10;
        }
        
        .worker-table th {
            background: #34495e;
            color: white;
            padding: 12px 10px;
            text-align: left;
            font-weight: 500;
            border: 1px solid #2c3e50;
        }
        
        .worker-table th:first-child {
            border-top-left-radius: 4px;
        }
        
        .worker-table th:last-child {
            border-top-right-radius: 4px;
        }
        
        .worker-table td {
            padding: 10px;
            border: 1px solid #e0e0e0;
            vertical-align: top;
            max-width: 300px;
        }
        
        .worker-table tr:hover td {
            background: #fafafa;
        }
        
        .time-cell {
            font-weight: 500;
            color: #555;
            white-space: nowrap;
        }
        
        .empty-cell {
            background: #f9f9f9;
        }
        
        .message-cell {
            min-width: 200px;
            max-width: 300px;
        }
        
        .message-cell.post-condition {
            background: #fffbf0;
        }
        
        .message-cell.violation-condition {
            background: #fff5f5;
        }
        
        .message-cell.found-result {
            background: #f0fff4;
        }
        
        .message-card {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }
        
        .message-header {
            display: flex;
            gap: 6px;
            align-items: center;
        }
        
        .message-arrow {
            font-size: 1.2em;
            font-weight: bold;
            color: #555;
        }
        
        .message-info {
            flex: 1;
            font-size: 0.85em;
            word-wrap: break-word;
            overflow-wrap: break-word;
        }
        
        .message-id {
            color: #888;
            font-size: 0.8em;
            font-weight: normal;
            display: block;
            margin-top: 2px;
        }
        
        .message-direction {
            font-size: 0.85em;
            color: #555;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 4px;
        }
        
        .message-type-badge {
            padding: 2px 6px;
            background: #34495e;
            color: white;
            border-radius: 3px;
            font-size: 0.7em;
            font-weight: 500;
            white-space: nowrap;
        }
        
        .message-content {
            margin-top: 4px;
        }
        
        .message-content summary {
            cursor: pointer;
            font-weight: 500;
            color: #3498db;
            font-size: 0.8em;
            padding: 4px 0;
            user-select: none;
        }
        
        .message-content summary:hover {
            color: #2980b9;
        }
        
        .content-json {
            margin-top: 6px;
            padding: 8px;
            background: #f5f5f5;
            border-radius: 3px;
            font-size: 0.75em;
            font-family: 'Consolas', 'Monaco', monospace;
            overflow-x: auto;
            max-height: 200px;
            overflow-y: auto;
            border: 1px solid #e0e0e0;
            white-space: pre-wrap;
            word-wrap: break-word;
            word-break: break-all;
        }
        
        /* Fixed floating button for block graph */
        .floating-graph-btn {
            position: fixed;
            bottom: 30px;
            right: 30px;
            width: 60px;
            height: 60px;
            border-radius: 50%;
            background: #3498db;
            color: white;
            border: none;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.5em;
            z-index: 100;
            transition: all 0.3s ease;
        }
        
        .floating-graph-btn:hover {
            background: #2980b9;
            transform: scale(1.1);
            box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
        }
        
        .modal {
            display: none;
            position: fixed;
            inset: 0;
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }
        
        .modal.active {
            display: flex;
        }
        
        .modal-overlay {
            position: absolute;
            inset: 0;
            background: rgba(0, 0, 0, 0.7);
            cursor: pointer;
        }
        
        .modal-content {
            position: relative;
            background: white;
            border-radius: 6px;
            max-width: 90vw;
            max-height: 90vh;
            overflow: hidden;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
            z-index: 1001;
        }
        
        .modal-content h2 {
            padding: 16px 24px;
            background: #2c3e50;
            color: white;
            margin: 0;
            font-size: 1.3em;
            font-weight: 500;
        }
        
        .modal-close {
            position: absolute;
            top: 16px;
            right: 24px;
            font-size: 1.8em;
            background: none;
            border: none;
            color: white;
            cursor: pointer;
            z-index: 1002;
            line-height: 1;
            padding: 0;
            width: 28px;
            height: 28px;
        }
        
        .modal-close:hover {
            opacity: 0.8;
        }
        
        .modal-body {
            padding: 20px;
            overflow: auto;
            max-height: calc(90vh - 64px);
            background: #fafafa;
        }
        
        .modal-body img {
            max-width: 100%;
            height: auto;
            display: block;
            background: white;
            border: 1px solid #e0e0e0;
            border-radius: 4px;
        }
        
        .hidden {
            display: none !important;
        }
        
        @media (max-width: 768px) {
            body {
                padding: 0;
            }
            
            .container {
                border-radius: 0;
            }
            
            .header h1 {
                font-size: 1.5em;
            }
            
            .controls {
                padding: 16px 20px;
            }
            
            .control-group {
                flex-direction: column;
            }
            
            .stats-panel {
                flex-direction: column;
                padding: 16px 20px;
            }
            
            .table-container {
                padding: 10px;
            }
            
            .worker-table td {
                max-width: 250px;
            }
            
            .floating-graph-btn {
                bottom: 20px;
                right: 20px;
                width: 50px;
                height: 50px;
                font-size: 1.3em;
            }
        }
    """


def get_embedded_javascript() -> str:
    """Return the embedded JavaScript for the HTML report."""
    return """
        // Filter state
        const filterState = {
            messageTypes: new Set(['POST_CONDITION', 'VIOLATION_CONDITION', 'FOUND_RESULT']),
            blockId: '',
            contentSearch: ''
        };
        
        // Initialize
        document.addEventListener('DOMContentLoaded', function() {
            initializeFilters();
            initializeModal();
            updateVisibleMessages();
        });
        
        function initializeFilters() {
            // Message type filters
            document.querySelectorAll('.filter-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const type = this.dataset.filterType;
                    this.classList.toggle('active');
                    
                    if (this.classList.contains('active')) {
                        filterState.messageTypes.add(type);
                    } else {
                        filterState.messageTypes.delete(type);
                    }
                    
                    applyFilters();
                });
            });
            
            // Block ID filter
            const blockFilter = document.getElementById('blockFilter');
            blockFilter.addEventListener('input', function() {
                filterState.blockId = this.value.toLowerCase();
                applyFilters();
            });
            
            document.getElementById('clearBlockFilter').addEventListener('click', function() {
                blockFilter.value = '';
                filterState.blockId = '';
                applyFilters();
            });
            
            // Content search
            const contentSearch = document.getElementById('contentSearch');
            contentSearch.addEventListener('input', function() {
                filterState.contentSearch = this.value.toLowerCase();
                applyFilters();
            });
            
            document.getElementById('clearContentSearch').addEventListener('click', function() {
                contentSearch.value = '';
                filterState.contentSearch = '';
                applyFilters();
            });
            
            // Reset button
            document.getElementById('resetFiltersBtn').addEventListener('click', function() {
                filterState.messageTypes = new Set(['POST_CONDITION', 'VIOLATION_CONDITION', 'FOUND_RESULT']);
                filterState.blockId = '';
                filterState.contentSearch = '';
                
                document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.add('active'));
                blockFilter.value = '';
                contentSearch.value = '';
                
                applyFilters();
            });
        }
        
        function applyFilters() {
            const rows = document.querySelectorAll('#messageTable tbody tr');
            
            rows.forEach(row => {
                const cells = row.querySelectorAll('.message-cell');
                let showRow = false;
                
                cells.forEach(cell => {
                    const messageType = cell.dataset.type;
                    const senderId = cell.querySelector('[data-sender]')?.dataset.sender || '';
                    const content = cell.textContent.toLowerCase();
                    
                    // Check filters
                    const typeMatch = !messageType || filterState.messageTypes.has(messageType);
                    const blockMatch = !filterState.blockId || senderId.toLowerCase().includes(filterState.blockId);
                    const contentMatch = !filterState.contentSearch || content.includes(filterState.contentSearch);
                    
                    if (messageType && typeMatch && blockMatch && contentMatch) {
                        cell.style.display = '';
                        showRow = true;
                    } else if (messageType) {
                        cell.style.display = 'none';
                    }
                });
                
                // Show/hide row
                row.style.display = showRow ? '' : 'none';
            });
            
            updateVisibleMessages();
        }
        
        function updateVisibleMessages() {
            const visibleRows = document.querySelectorAll('#messageTable tbody tr:not([style*="display: none"])');
            let visibleCount = 0;
            
            visibleRows.forEach(row => {
                const visibleCells = row.querySelectorAll('.message-cell:not([style*="display: none"])');
                visibleCount += visibleCells.length;
            });
            
            document.getElementById('visibleMessages').textContent = visibleCount;
        }
        
        function initializeModal() {
            const modal = document.getElementById('graphModal');
            const floatingBtn = document.getElementById('floatingGraphBtn');
            const closeBtn = document.getElementById('modalClose');
            const overlay = document.getElementById('modalOverlay');
            
            floatingBtn.addEventListener('click', () => {
                modal.classList.add('active');
            });
            
            closeBtn.addEventListener('click', () => {
                modal.classList.remove('active');
            });
            
            overlay.addEventListener('click', () => {
                modal.classList.remove('active');
            });
            
            // Close on Escape key
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && modal.classList.contains('active')) {
                    modal.classList.remove('active');
                }
            });
        }
    """


def main(argv=None):
    """Main entry point for the visualization tool."""
    if argv is None:
        argv = sys.argv[1:]

    try:
        args = parse_args(argv)
    except ValueError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    output_path: Path = args.output

    # Generate block graph visualization
    print("Generating block structure visualization...")
    visualize_block_graph(
        block_structure_file=args.block_structure_json,
        output_path=output_path
    )

    # Generate message visualization
    print("Processing messages...")
    output_file = load_and_process_messages(
        message_dir=args.messages_json,
        block_structure_json=args.block_structure_json,
        output_path=output_path,
        export_keys=args.export_keys,
    )

    if output_file:
        print(f"\n✓ Visualization complete!")
        print(f"Opening {output_file} in browser...")
        webbrowser.open(str(output_file))
        return 0
    else:
        print("ERROR: Failed to generate visualization", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
