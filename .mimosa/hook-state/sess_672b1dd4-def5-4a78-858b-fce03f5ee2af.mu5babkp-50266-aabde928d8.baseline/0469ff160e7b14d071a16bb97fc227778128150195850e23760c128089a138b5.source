#!/usr/bin/env python3
"""校验单个 SDK 的分支测试和串行并发压测结果。"""

import argparse
import csv
from pathlib import Path


CONCURRENCY_LEVELS = {"100", "300", "500", "800", "1000"}
WORKLOADS = {"http", "sse"}
BRANCHES = {"feature/1.0.x", "feature/2.0.x", "feature/3.0.x"}


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sdk", required=True)
    parser.add_argument("--results", required=True)
    parser.add_argument("--branches", required=True)
    return parser.parse_args()


def read_rows(path, delimiter=","):
    target = Path(path)
    if not target.is_file():
        raise SystemExit(f"FAIL missing result file: {target}")
    with target.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle, delimiter=delimiter))


def verify_branches(sdk, path):
    rows = read_rows(path, "\t")
    expected = {(sdk, branch) for branch in BRANCHES}
    actual = {
        (row.get("sdk"), row.get("branch"))
        for row in rows
        if row.get("status") == "PASS"
    }
    if len(rows) != 3 or actual != expected:
        raise SystemExit("FAIL branch verification must contain exactly 3 passing rows")


def verify_benchmarks(sdk, path):
    rows = read_rows(path)
    expected = {
        (sdk, workload, concurrency)
        for workload in WORKLOADS
        for concurrency in CONCURRENCY_LEVELS
    }
    actual = {
        (row.get("sdk"), row.get("workload"), row.get("concurrency"))
        for row in rows
    }
    if len(rows) != 10 or actual != expected:
        raise SystemExit("FAIL benchmark results must contain exactly 10 unique runs")

    intervals = []
    for row in rows:
        if row.get("status") != "PASS" or int(row.get("errors", "-1")) != 0:
            raise SystemExit(f"FAIL benchmark errors: {row}")
        if float(row.get("avg_cpu_pct", "-1")) < 0:
            raise SystemExit(f"FAIL invalid average CPU: {row}")
        if float(row.get("peak_cpu_pct", "0")) <= 0:
            raise SystemExit(f"FAIL invalid peak CPU: {row}")
        if float(row.get("peak_rss_mb", "0")) <= 0:
            raise SystemExit(f"FAIL invalid peak RSS: {row}")
        if float(row.get("throughput_per_sec", "0")) <= 0:
            raise SystemExit(f"FAIL invalid throughput: {row}")
        start = int(row["started_epoch_ms"])
        end = int(row["ended_epoch_ms"])
        if end <= start:
            raise SystemExit(f"FAIL invalid interval: {row}")
        intervals.append((start, end, row))

    intervals.sort(key=lambda value: value[0])
    for previous, current in zip(intervals, intervals[1:]):
        if current[0] < previous[1]:
            raise SystemExit(
                f"FAIL overlapping benchmark runs: {previous[2]} and {current[2]}"
            )


def main():
    args = parse_args()
    verify_branches(args.sdk, args.branches)
    verify_benchmarks(args.sdk, args.results)
    print(f"{args.sdk.upper()}_BENCHMARK_RESULTS_PASS")


if __name__ == "__main__":
    main()
