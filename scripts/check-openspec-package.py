#!/usr/bin/env python3
"""Limited offline documentation check; NOT the official OpenSpec validator.

Only uses Python's standard library. No network, no SDK execution, no mutation.
Checks this package's known change set, Markdown shape, IDs and traceability.
It does not parse the full OpenSpec schema or simulate archive merges.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from urllib.parse import unquote

REQ = re.compile(r"^### Requirement: ([A-Z]+-\d{3}) (.+)$", re.MULTILINE)
SCENARIO = re.compile(r"^#### Scenario: ([A-Z]+-\d{3}-S\d+) (.+)$", re.MULTILINE)
TASK = re.compile(r"^- \[([^\]]*)\] (\d+\.\d+) (.+)$")

def without_fences(text: str) -> str:
    result, in_fence = [], False
    for line in text.splitlines():
        if line.lstrip().startswith(("```", "~~~")):
            in_fence = not in_fence
            result.append("")
        else:
            result.append("" if in_fence else line)
    return "\n".join(result)

def check(root: Path, allow_progress: bool = False) -> dict:
    errors: list[dict] = []
    counts = {"changes": 0, "capabilities": 0, "requirements": 0, "scenarios": 0,
              "tasks": 0, "checked_tasks": 0, "legacy_tasks": 0, "legacy_acceptance": 0}
    def error(code: str, path: object, detail: str):
        errors.append({"code": code, "path": str(path), "detail": detail})
    def read(path: str) -> str:
        p = root / path
        try:
            return p.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as ex:
            error("READ_FAILURE", path, str(ex)); return ""
    def load(path: str) -> dict:
        try:
            return json.loads(read(path))
        except json.JSONDecodeError as ex:
            error("JSON_INVALID", path, str(ex)); return {}

    index = load("docs/openspec/spec-index.json")
    trace = load("docs/openspec/traceability.json")
    if not index or not trace:
        return {"scope": "limited-offline-document-check", "passed": False,
                "errors": errors, "counts": counts, "official_cli_executed": False}
    config = read("openspec/config.yaml")
    for pattern in (r"^schema: spec-driven$", r"^context: \|$", r"^rules:$"):
        if not re.search(pattern, config, re.MULTILINE):
            error("CONFIG_SHAPE", "openspec/config.yaml", "Missing expected field: "+pattern)
    # Full YAML/schema semantics are deliberately outside this checker.
    expected_changes = index["changes"]
    found_changes = {p.name for p in (root/"openspec/changes").iterdir()
                     if p.is_dir() and p.name != "archive"}
    if set(expected_changes) != found_changes:
        error("CHANGE_SET", "openspec/changes", repr(found_changes))
    seen_requirements, seen_scenarios, parsed_tasks = {}, {}, {}
    seen_caps = set()
    for ch, definition in expected_changes.items():
        counts["changes"] += 1
        base = f"openspec/changes/{ch}"
        meta = read(base+"/.openspec.yaml")
        if meta.strip() != "schema: spec-driven":
            error("METADATA_SHAPE", base+"/.openspec.yaml", "Expected minimal spec-driven metadata")
        proposal = read(base+"/proposal.md")
        design = read(base+"/design.md")
        task_text = read(base+"/tasks.md")
        catalog = read(base+"/test-catalog.md")
        for heading in ["## Why","## What Changes","## Capabilities","### New Capabilities",
                        "### Modified Capabilities","## Impact"]:
            if heading not in proposal:
                error("PROPOSAL_SHAPE", base+"/proposal.md", "Missing "+heading)
        for heading in ["## Context","## Goals / Non-Goals","## Decisions",
                        "## Risks / Trade-offs","## Migration Plan"]:
            if heading not in design:
                error("DESIGN_SHAPE", base+"/design.md", "Missing "+heading)
        try:
            new_section = proposal.split("### New Capabilities",1)[1].split("### Modified Capabilities",1)[0]
            declared = set(re.findall(r"^- `([a-z0-9-]+)`", new_section, re.MULTILINE))
        except IndexError:
            declared = set()
        actual = {p.parent.name for p in (root/base/"specs").glob("*/spec.md")}
        if declared != set(definition["caps"]) or actual != declared:
            error("CAPABILITY_SET", base, f"Declared={declared}, Actual={actual}")
        for cn in sorted(actual):
            if cn in seen_caps:
                error("CAPABILITY_OWNER", cn, "Same capability declared by multiple changes")
            seen_caps.add(cn); counts["capabilities"] += 1
            path = base+f"/specs/{cn}/spec.md"
            text = without_fences(read(path))
            purpose = re.search(r"^## Purpose\s*\n(.*?)(?=^## |\Z)",text,re.MULTILINE|re.DOTALL)
            if not purpose or len(purpose.group(1).strip()) < 50:
                error("PURPOSE_SHAPE",path,"New capability needs substantive Purpose (50+ characters)")
            if "## ADDED Requirements" not in text:
                error("DELTA_SHAPE",path,"Expected ADDED for this empty baseline")
            if re.search(r"^## (MODIFIED|REMOVED|RENAMED) Requirements",text,re.MULTILINE):
                error("DELTA_BASELINE",path,"This package has no preexisting spec targets")
            matches=list(REQ.finditer(text))
            if not matches: error("NO_REQUIREMENTS",path,"No recognized Requirement")
            for i,m in enumerate(matches):
                rid,_title=m.groups();counts["requirements"]+=1
                if rid in seen_requirements: error("DUPLICATE_REQUIREMENT",path,rid)
                seen_requirements[rid]={"capability": cn, "change": ch, "spec": path}
                block=text[m.end():matches[i+1].start() if i+1<len(matches) else len(text)]
                first=block.split("#### Scenario:",1)[0]
                if not re.search(r"\b(MUST|SHALL)\b",first):
                    error("NORMATIVE_KEYWORD",path,rid+" missing MUST/SHALL")
                if text.index("## ADDED Requirements") > m.start():
                    error("OUTSIDE_DELTA",path,rid)
                sm=list(SCENARIO.finditer(block))
                if not sm: error("NO_SCENARIOS",path,rid)
                if len(re.findall(r"^#### Scenario:",block,re.MULTILINE)) != len(sm):
                    error("SCENARIO_ID",path,rid+" invalid scenario id")
                for j,s in enumerate(sm):
                    sid,_stitle=s.groups();counts["scenarios"]+=1
                    if sid in seen_scenarios: error("DUPLICATE_SCENARIO",path,sid)
                    seen_scenarios[sid]=rid
                    if not sid.startswith(rid+"-S"): error("SCENARIO_OWNER",path,sid)
                    sb=block[s.end():sm[j+1].start() if j+1<len(sm) else len(block)]
                    for kw in ("GIVEN","WHEN","THEN"):
                        if not re.search(r"^- \*\*"+kw+r"\*\* \S",sb,re.MULTILINE):
                            error("SCENARIO_SHAPE",path,sid+" missing "+kw)
                    if sid not in catalog:
                        error("TEST_CATALOG_MISSING",base+"/test-catalog.md",sid)
        current_group=None
        for line in without_fences(task_text).splitlines():
            head=re.match(r"^## (\d+)\.",line)
            if head: current_group=head.group(1)
            match=TASK.match(line)
            if not match:
                if line.startswith("- ["): error("UNTRACKED_TASK",base+"/tasks.md",line)
                continue
            marker,num,body=match.groups();counts["tasks"]+=1
            key=ch+":"+num
            if key in parsed_tasks: error("DUPLICATE_TASK",base+"/tasks.md",num)
            parsed_tasks[key]=body
            if num.split(".")[0] != current_group:
                error("TASK_GROUP",base+"/tasks.md",num)
            if marker.strip().lower() == "x":
                counts["checked_tasks"]+=1
                if not allow_progress: error("PREMATURE_COMPLETION",base+"/tasks.md",num)
            if marker.strip().lower() not in ("","x"):
                error("TASK_MARKER",base+"/tasks.md",marker)
            if not re.search(r"验证|测试|记录|检查|检视|运行|核对|审查|复核",body):
                error("TASK_VERIFICATION",base+"/tasks.md",num+" has no verification cue")

    if set(seen_requirements) != set(index["requirements"]):
        error("INDEX_REQUIREMENTS","docs/openspec/spec-index.json","Requirement ids differ")
    for rid,obj in index["requirements"].items():
        expected={s["id"] for s in obj["scenarios"]}
        actual={sid for sid,owner in seen_scenarios.items() if owner==rid}
        if actual != expected: error("INDEX_SCENARIOS",rid,f"{actual} != {expected}")
    if set(parsed_tasks) != set(trace["tasks"]):
        error("TRACE_TASK_SET","docs/openspec/traceability.json","Parsed task IDs differ from manifest")
    for rid,obj in trace["requirements"].items():
        if rid not in seen_requirements: error("TRACE_REQUIREMENT",rid,"Not found in specs"); continue
        if not obj["tasks"]: error("TRACE_TASK_MISSING",rid,"No owning tasks")
        for tk in obj["tasks"]:
            if tk not in parsed_tasks: error("TRACE_UNKNOWN_TASK",rid,tk)
        for sid in obj["scenarios"]:
            if seen_scenarios.get(sid) != rid: error("TRACE_SCENARIO",rid,sid)
        if obj["spec"] != seen_requirements[rid]["spec"]:
            error("TRACE_SPEC_PATH",rid,obj["spec"])
    if set(trace["requirements"]) != set(seen_requirements):
        error("TRACE_REQUIREMENT_SET","docs/openspec/traceability.json","Requirement coverage differs")
    original=read("docs/design/hermes-java-sdk-optimization-plan-v1.0.md")
    legacy_h=set(re.findall(r"^- \[ \] \*\*(H-\d+)\*\*",original,re.MULTILINE))
    legacy_t=set(re.findall(r"^\|\s*(T-\d+)\s*\|",original,re.MULTILINE))
    counts["legacy_tasks"]=len(trace["legacy_tasks"]);counts["legacy_acceptance"]=len(trace["legacy_acceptance"])
    if legacy_h != set(trace["legacy_tasks"]):
        error("LEGACY_TASK_COVERAGE","docs/openspec/traceability.json","Original H set not preserved")
    if legacy_t != set(trace["legacy_acceptance"]):
        error("LEGACY_TEST_COVERAGE","docs/openspec/traceability.json","Original T set not preserved")
    for hid,targets in trace["legacy_tasks"].items():
        if not targets: error("LEGACY_TASK_EMPTY",hid,"No mapping")
        for tk in targets:
            if tk not in parsed_tasks or f"**{hid}**" not in parsed_tasks[tk]:
                error("LEGACY_TASK_TARGET",hid,tk)
    for tid,sids in trace["legacy_acceptance"].items():
        if not sids: error("LEGACY_TEST_EMPTY",tid,"No mapping")
        for sid in sids:
            if sid not in seen_scenarios: error("LEGACY_TEST_TARGET",tid,sid)
    deps=trace["dependencies"];visiting=set();done=set()
    def visit(node):
        if node in visiting: error("DEPENDENCY_CYCLE",node,"Cycle detected");return
        if node in done:return
        if node not in deps: error("DEPENDENCY_UNKNOWN",node,"Missing node");return
        visiting.add(node)
        for upstream in deps[node]:visit(upstream)
        visiting.remove(node);done.add(node)
    for ch in deps:visit(ch)
    for ch,obj in expected_changes.items():
        if deps.get(ch) != obj["depends"]:
            error("DEPENDENCY_MANIFEST_MISMATCH",ch,"Manifest dependency differs")
    # Paths only, not anchor semantics. Ignore remote URLs and examples inside code fences.
    for path in list((root/"openspec/changes").rglob("*.md"))+list((root/"docs").rglob("*.md")):
        text=without_fences(path.read_text(encoding="utf-8"))
        for dest in re.findall(r"(?<!!)\[[^\]\n]+\]\(([^)\s]+)\)",text):
            if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:",dest) or dest.startswith("#"):continue
            target=(path.parent/unquote(dest.split("#",1)[0])).resolve()
            if not target.is_relative_to(root.resolve()) or not target.exists():
                error("BROKEN_LINK",path.relative_to(root),dest)
    source=load("docs/openspec/source-manifest.json")
    op=source.get("original_plan",{})
    if op and hashlib.sha256((root/op["path"]).read_bytes()).hexdigest()!=op["sha256"]:
        error("ORIGINAL_PLAN_HASH",op["path"],"Source plan bytes changed")
    if not allow_progress and list((root/"openspec/specs").glob("**/spec.md")):
        error("PREMATURE_APPLIED_SPEC","openspec/specs","Planning package must not pretend implementation is archived")
    return {
        "scope": "limited-offline-document-check",
        "passed": not errors,
        "counts": counts,
        "errors": errors,
        "official_cli_executed": False,
        "sdk_tests_executed": False,
        "limitations": [
            "Not the official OpenSpec parser or schema validator",
            "Does not simulate archive merges or verify Markdown anchors",
            "Does not execute SDK, JDK, Hermes or platform tests",
            "No proof of semantic completeness beyond the explicitly checked mappings",
        ],
    }

def main() -> int:
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root",type=Path,default=Path(__file__).resolve().parents[1])
    parser.add_argument("--json",action="store_true",dest="as_json")
    parser.add_argument("--allow-progress",action="store_true",help="Allow checked implementation tasks and applied specs after actual work")
    args=parser.parse_args()
    try:
        result=check(args.root.resolve(),args.allow_progress)
    except (OSError,KeyError,TypeError,ValueError) as ex:
        result = {
            "scope": "limited-offline-document-check",
            "passed": False,
            "errors": [{"code": "CHECK_ABORTED", "path": str(args.root), "detail": str(ex)}],
            "counts": {},
            "official_cli_executed": False,
        }
    if args.as_json:print(json.dumps(result,ensure_ascii=False,indent=2))
    else:
        print("LIMITED OFFLINE CHECK: "+("PASS" if result["passed"] else "FAIL"))
        print(json.dumps(result["counts"],ensure_ascii=False))
        for e in result["errors"]:print(f"{e['code']}: {e['path']} — {e['detail']}")
        print("Official OpenSpec CLI and SDK tests: NOT EXECUTED by this checker.")
    return 0 if result["passed"] else 1

if __name__=="__main__":
    raise SystemExit(main())
