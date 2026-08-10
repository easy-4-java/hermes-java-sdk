#!/bin/zsh
set -euo pipefail

repo_root=${0:a:h:h}
sdk=hermes
workload=${1:-}
concurrency=${2:-}
result_file=${3:-$repo_root/target/benchmark/benchmark-results.csv}
lock_dir=/tmp/easy4j-sdk-benchmark-exclusive.lock

if [[ "$workload" != http && "$workload" != sse ]]; then
  print -u2 'Usage: run-concurrency-benchmark.zsh <http|sse> <concurrency> [result.csv]'
  exit 64
fi
if [[ ! "$concurrency" =~ '^[1-9][0-9]*$' ]]; then
  print -u2 'concurrency must be a positive integer'
  exit 64
fi
if ! mkdir "$lock_dir" 2>/dev/null; then
  print -u2 "another SDK benchmark holds the global lock: $lock_dir"
  exit 75
fi

benchmark_pid=
cleanup() {
  if [[ -n "$benchmark_pid" ]] && kill -0 "$benchmark_pid" 2>/dev/null; then
    kill "$benchmark_pid" 2>/dev/null || true
  fi
  rmdir "$lock_dir" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

mkdir -p "$repo_root/target/benchmark"
classpath_file=$repo_root/target/benchmark/test-classpath.txt
metrics_file=$repo_root/target/benchmark/${sdk}-${workload}-${concurrency}.metrics
samples_file=$repo_root/target/benchmark/${sdk}-${workload}-${concurrency}.samples
log_file=$repo_root/target/benchmark/${sdk}-${workload}-${concurrency}.log
rm -f "$metrics_file" "$samples_file" "$log_file"

JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}
PATH="$JAVA_HOME/bin:$PATH" mvn -q -f "$repo_root/pom.xml" -DskipTests test-compile \
  dependency:build-classpath -Dmdep.outputFile="$classpath_file"
classpath="$repo_root/target/test-classes:$repo_root/target/classes:$(<"$classpath_file")"
started_epoch_ms=$(python3 -c 'import time; print(time.time_ns() // 1000000)')
"$JAVA_HOME/bin/java" -cp "$classpath" io.github.easy4j.hermes.HermesConcurrencyBenchmark \
  "$workload" "$concurrency" "$metrics_file" >"$log_file" 2>&1 &
benchmark_pid=$!

while kill -0 "$benchmark_pid" 2>/dev/null; do
  ps -o %cpu= -o rss= -p "$benchmark_pid" | awk 'NF == 2 { print $1, $2 }' >>"$samples_file"
  sleep 0.2
done
set +e
wait "$benchmark_pid"
benchmark_exit=$?
set -e
benchmark_pid=
ended_epoch_ms=$(python3 -c 'import time; print(time.time_ns() // 1000000)')

if [[ ! -s "$metrics_file" ]]; then
  print -u2 "benchmark did not produce metrics; see $log_file"
  exit ${benchmark_exit:-1}
fi
operations=$(awk -F= '$1 == "operations" { print $2 }' "$metrics_file")
errors=$(awk -F= '$1 == "errors" { print $2 }' "$metrics_file")
duration_seconds=$(awk -F= '$1 == "duration_seconds" { print $2 }' "$metrics_file")
throughput=$(awk -F= '$1 == "throughput_per_sec" { print $2 }' "$metrics_file")
read avg_cpu peak_cpu peak_rss_mb <<<"$(awk '
  BEGIN { sum=0; count=0; peak_cpu=0; peak_rss=0 }
  NF == 2 { sum += $1; count++; if ($1 > peak_cpu) peak_cpu=$1; if ($2 > peak_rss) peak_rss=$2 }
  END { printf "%.3f %.3f %.3f", count ? sum/count : 0, peak_cpu, peak_rss/1024 }
' "$samples_file")"
benchmark_status=PASS
if (( benchmark_exit != 0 || errors != 0 )) || [[ "$peak_cpu" == 0.000 ]]; then
  benchmark_status=FAIL
fi
git_sha=$(git -C "$repo_root" rev-parse HEAD)
if [[ ! -s "$result_file" ]]; then
  print 'sdk,workload,concurrency,started_epoch_ms,ended_epoch_ms,duration_seconds,operations,errors,throughput_per_sec,avg_cpu_pct,peak_cpu_pct,peak_rss_mb,status,git_sha' >"$result_file"
fi
print "$sdk,$workload,$concurrency,$started_epoch_ms,$ended_epoch_ms,$duration_seconds,$operations,$errors,$throughput,$avg_cpu,$peak_cpu,$peak_rss_mb,$benchmark_status,$git_sha" >>"$result_file"
print "$sdk $workload concurrency=$concurrency status=$benchmark_status cpu(avg/peak)=$avg_cpu/$peak_cpu rss_peak_mb=$peak_rss_mb throughput=$throughput"
if [[ "$benchmark_status" != PASS ]]; then
  print -u2 "benchmark failed; see $log_file"
  exit 1
fi
