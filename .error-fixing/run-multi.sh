#!/bin/bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

TARGET_DIR="../sv-benchmarks/c/intel-tdx-module"
CPACHECKER_BIN="./bin/cpachecker"
OPTIONS="--svcomp25 --timelimit 120s"

BASE_LOG_DIR="output/.i"
MASTER_SUMMARY="$BASE_LOG_DIR/.abridged-summary.log"

# Defaults
lower_bound=""
upper_bound=""
verbose=false
clear_old=false

# Parse arguments in any order
for arg in "$@"; do
  case "$arg" in
    verbose) verbose=true ;;
    clear) clear_old=true ;;
    all) upper_bound="all" ;;
    last) upper_bound="last" ;;
    ''|*[!0-9]*) ;; # ignore non-numeric unless handled above
    *)
      if [[ -z "$lower_bound" ]]; then
        lower_bound="$arg"
      elif [[ -z "$upper_bound" ]]; then
        upper_bound="$arg"
      fi
      ;;
  esac
done

# Require at least one bound
if [[ -z "$lower_bound" && -z "$upper_bound" ]]; then
  echo "Usage: $0 <number|start end|start last|all> [verbose] [clear]"
  exit 1
fi

# Count total files
total_files=$(ls "$TARGET_DIR"/*.i | wc -l)

# Normalize bounds if two numbers are given
if [[ -n "$upper_bound" && -n "$lower_bound" && "$upper_bound" != "all" && "$upper_bound" != "last" ]]; then
  if (( lower_bound > upper_bound )); then
    tmp=$lower_bound
    lower_bound=$upper_bound
    upper_bound=$tmp
  fi
fi

# Handle clearing
if $clear_old; then
  rm -rf "$BASE_LOG_DIR"
fi

# Create timestamped run directory
timestamp=$(date +"%Y-%m-%d_%H-%M-%S")
LOG_DIR="$BASE_LOG_DIR/run_$timestamp"
FULL_SUMMARY="$LOG_DIR/.full-summary.log"
mkdir -p "$LOG_DIR"

> "$LOG_DIR/passed.log"
> "$LOG_DIR/several-exceptions.log"
> "$LOG_DIR/single-parsing-failed.log"
> "$LOG_DIR/single-error.log"
> "$LOG_DIR/single-exception.log"

passed_count=0
several_count=0
parsing_failed_count=0
error_count=0
exception_count=0

trap 'echo; echo "Interrupted! Summary so far:";
      echo "Passed: $passed_count (see $LOG_DIR/passed.log)";
      echo "Several exceptions: $several_count (see $LOG_DIR/several-exceptions.log)";
      echo "Parsing failed: $parsing_failed_count (see $LOG_DIR/single-parsing-failed.log)";
      echo "Errors: $error_count (see $LOG_DIR/single-error.log)";
      echo "Exceptions: $exception_count (see $LOG_DIR/single-exception.log)";
      exit 1' INT

# Select files
if [[ "$upper_bound" == "all" ]]; then
  files=$(ls "$TARGET_DIR"/*.i)
  lower_bound=1
elif [[ "$upper_bound" == "last" ]]; then
  upper_bound=$total_files
  files=$(ls "$TARGET_DIR"/*.i | sed -n "${lower_bound},${upper_bound}p")
elif [[ -n "$upper_bound" && -n "$lower_bound" ]]; then
  # Range mode: run files from lower_bound to upper_bound inclusive
  files=$(ls "$TARGET_DIR"/*.i | sed -n "${lower_bound},${upper_bound}p")
else
  # Single number mode: run first N files (upper bound only, lower bound = 1 implicit)
  upper_bound="$lower_bound"
  lower_bound=1
  files=$(ls "$TARGET_DIR"/*.i | sed -n "${lower_bound},${upper_bound}p")
fi

index=$lower_bound

# Timing stats
total_time=0
shortest_time=999999
longest_time=0
shortest_file=""
longest_file=""
declare -A file_times
declare -A file_names

for file in $files; do
  if [[ -f "$file" ]]; then
    echo "[$index] Running CPAchecker on: $file"

    start_time=$(date +%s)

    if $verbose; then
      "$CPACHECKER_BIN" "$file" $OPTIONS 2>&1 | tee "$LOG_DIR/file_${index}.out"
      output=$(cat "$LOG_DIR/file_${index}.out")
    else
      output=$("$CPACHECKER_BIN" "$file" $OPTIONS 2>&1)
      echo "$output" > "$LOG_DIR/file_${index}.out"
    fi

    end_time=$(date +%s)
    duration=$((end_time - start_time))
    total_time=$((total_time + duration))
    file_times[$index]=$duration
    file_names[$index]=$(basename "$file")

    if (( duration < shortest_time )); then
      shortest_time=$duration
      shortest_file=$index
    fi
    if (( duration > longest_time )); then
      longest_time=$duration
      longest_file=$index
    fi

    # Categorization
    if echo "$output" | grep -q "Several exceptions occurred during the analysis"; then
      echo "$index $file" >> "$LOG_DIR/several-exceptions.log"
      several_count=$((several_count + 1))
      echo "Several exceptions [$index]: $file (time=${duration}s)"
    elif echo "$output" | grep -q "Error: Parsing failed"; then
      echo "$index $file" >> "$LOG_DIR/single-parsing-failed.log"
      parsing_failed_count=$((parsing_failed_count + 1))
      echo "Parsing failed [$index]: $file (time=${duration}s)"
    elif echo "$output" | grep -q "Error:"; then
      echo "$index $file" >> "$LOG_DIR/single-error.log"
      error_count=$((error_count + 1))
      echo "Error [$index]: $file (time=${duration}s)"
    elif echo "$output" | grep -q "Exception"; then
      echo "$index $file" >> "$LOG_DIR/single-exception.log"
      exception_count=$((exception_count + 1))
      echo "Exception [$index]: $file (time=${duration}s)"
    else
      echo "$index $file" >> "$LOG_DIR/passed.log"
      passed_count=$((passed_count + 1))
      echo "Passed [$index]: $file (time=${duration}s)"
    fi

    echo "-----------------------------------"
    index=$((index + 1))
  fi
done

# Final summary description
if [[ "$upper_bound" == "all" ]]; then
  run_desc="Processed all files."
elif [[ "$upper_bound" == "$total_files" && passed"$lower_bound" != "1" ]]; then
  run_desc="Processed files $lower_bound through $upper_bound (last)."
elif [[ -n "$upper_bound" && -n "$lower_bound" && "$lower_bound" != "1" ]]; then
  run_desc="Processed files $lower_bound through $upper_bound."
else
  run_desc="Processed files 1 through $upper_bound."
fi

echo "$run_desc"
echo "Passed: $passed_count"
# (see $LOG_DIR/passed.log)
echo "Several exceptions: $several_count"
# (see $LOG_DIR/several-exceptions.log)
echo "Parsing failed: $parsing_failed_count"
# (see $LOG_DIR/single-parsing-failed.log)
echo "Single Errors: $error_count"
# (see $LOG_DIR/single-error.log)
echo "Exceptions: $exception_count"
# (see $LOG_DIR/single-exception.log)

# Compute averages
file_count=$((passed_count + several_count + parsing_failed_count + error_count + exception_count))
if (( file_count > 0 )); then
  avg_time=$(echo "scale=2; $total_time / $file_count" | bc)
else
  avg_time=0
fi

# Write per-run summary file
{
  echo "[$timestamp] $run_desc"
  echo "Passed=$passed_count Several=$several_count ParsingFailed=$parsing_failed_count Error=$error_count Exception=$exception_count"
  echo "Total time=${total_time}s"
  echo "Average time per file=${avg_time}s"
  echo "Shortest time=${shortest_time}s (file $shortest_file: ${file_names[$shortest_file]})"
  echo "Longest time=${longest_time}s (file $longest_file: ${file_names[$longest_file]})"
  echo "Per-file times:"
  for i in "${!file_times[@]}"; do
    echo "File $i (${file_names[$i]}): ${file_times[$i]}s"
  done | sort -n -k2
} > "$LOG_DIR/summary.log"

# Append to master summary
echo "[$timestamp] $run_desc | Passed=$passed_count Several=$several_count ParsingFailed=$parsing_failed_count Error=$error_count Exception=$exception_count" >> "$MASTER_SUMMARY"
echo "  Total=${total_time}s Avg=${avg_time}s Shortest=${shortest_time}s(file $shortest_file:${file_names[$shortest_file]}) Longest=${longest_time}s(file $longest_file:${file_names[$longest_file]})" >> "$MASTER_SUMMARY"

# Build full summary file by concatenating all category logs
{
  echo "[$timestamp] Full summary of run"
  echo "Run description: $run_desc"
  echo "Passed=$passed_count Several=$several_count ParsingFailed=$parsing_failed_count Error=$error_count Exception=$exception_count"
  echo
  echo "=== Passed files ==="
  cat "$LOG_DIR/passed.log"
  echo
  echo "=== Single Errors ==="
  cat "$LOG_DIR/single-error.log"
  echo
  echo "=== Several Exceptions ==="
  cat "$LOG_DIR/several-exceptions.log"
  echo
  echo "=== Parsing Failed ==="
  cat "$LOG_DIR/single-parsing-failed.log"
  echo
  echo "=== Single Exceptions ==="
  cat "$LOG_DIR/single-exception.log"
} > "$FULL_SUMMARY"
