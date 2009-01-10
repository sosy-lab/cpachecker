#!/usr/bin/env python

"""
Script to benchmark various configurations of CPAChecker
"""

from __future__ import with_statement
import os, sys
import glob
import subprocess
from string import Template
import optparse

# memory limit in bytes (can be overriden on the command line)
MEMORY_LIMIT = 2000000

# time limit in seconds (can be overriden on the command line)
TIME_LIMIT = 1800


CPACHECKER_DIR = os.path.dirname(sys.argv[0])

CSISAT_REPLACEMENT_DIR = os.path.abspath(os.path.join(
    os.path.dirname(sys.argv[0]), 'support_bin'))


def configname(config):
    return os.path.basename(config).split('.')[0]

def run_single(benchmark, config, time_limit, mem_limit):
    """\
    Runs a single benchmark instance with the given configuration, time limit
    and memory limit. The output is saved in a file $benchmark.$config.log.
    Returns a pair (time, outcome)
    """
    cn = configname(config)
    cmdline = Template('ulimit -t $time_limit -v $mem_limit; '
                       '(PATH=../../nativeLibs/Simplify/:$$PATH '
                       './cpa_query-driven-program-testing.sh -config $config -nolog $benchmark > '
                       '$benchmark.$cn.log 2>&1)').substitute(locals())
    p = subprocess.Popen(['/bin/bash', '-c', cmdline], shell=False,
                         cwd=CPACHECKER_DIR)
    retval = p.wait()
    if retval != 0:
        outcome = 'ERROR'
        tot_time = -1
    else:
        tot_time, outcome = None, None
        with open('%s.%s.log' % (benchmark, configname(config))) as f:
            for line in f:
                if tot_time is None and line.startswith(
                    'Total Time Elapsed including CFA construction:'):
                    tot_time = line[46:].strip()[:-1]
                if outcome is None and line.startswith(
                    '#Test cases computed:'):
                    outcome = 'OK'
        if tot_time is None:
            tot_time = -1
        if outcome is None:
            outcome = 'UNKNOWN'
    return tot_time, outcome
    

def main(which, benchmarks, configs, time_limit, mem_limit, outfile,
         order='config', verbose=True):
    """\
    Runs a collection of benchmarks for each of the given configs, and with
    the given time and memory limits. Collects results in one log file for each
    configuration. These log files have the following format:
    BENCHMARK TIME OUTCOME
    If verbose is true, prints on stdout progress messages.    
    order can be used to decide in which order to run benchmarks: if it is
    'config', all benchmarks will be run with one configuration, before moving
    to the next config. Otherwise, each benchmark is run with all the
    configurations before moving to the next one
    """
    run = run_single
        
    results = {}
    for config in configs:
        results[configname(config)] = {}

    def go(b, c, t, m):
        if verbose:
            sys.stdout.write('Running: %s with config: %s...' %
                             (b, configname(c)))
            sys.stdout.flush()
        results[configname(c)][b] = run(b, c, t, m)
        if results[configname(c)][b][1] == 'ERROR':
            sys.stderr.write('ERROR\n')
        elif verbose:
            sys.stdout.write('DONE\n')

    try:
        if order == 'config':
            for config in configs:
                for benchmark in benchmarks:
                    go(benchmark, config, time_limit, mem_limit)
        else:
            for benchmark in benchmarks:
                for config in configs:
                    go(benchmark, config, time_limit, mem_limit)
    finally:
        # write out the results
        for config in map(configname, configs):
            with open(outfile + '.' + config + '.log', 'w') as out:
                d = results[config]
                maxlen = max(map(len, d.keys()))
                maxlentime = len(str(time_limit)) + 4
                out.write(Template('$i $t $o\n').substitute(
                    i='INSTANCE'.ljust(maxlen), t='TIME'.rjust(maxlentime),
                    o='OUTCOME'))
                out.write('-' * (maxlen + maxlentime + 7 + 2) + '\n')
                for name in sorted(d):
                    line = Template('$fname $ftime $foutcome\n')
                    fname = name.ljust(maxlen)
                    ftime = str(d[name][0]).rjust(maxlentime)
                    foutcome = d[name][1].rjust(7)
                    out.write(line.substitute(locals()))


if __name__ == '__main__':
    p = optparse.OptionParser()
    p.add_option('-c', '--config', action='append')
    p.add_option('-t', '--timeout', type='int', default=TIME_LIMIT)
    p.add_option('-m', '--memlimit', type='int', default=MEMORY_LIMIT)
    p.add_option('-o', '--output', default='results')
    p.add_option('--cpachecker_dir')
    p.add_option('--csisat_replacement_dir')
    
    opts, args = p.parse_args()

    if opts.cpachecker_dir:
        CPACHECKER_DIR = opts.cpachecker_dir
    if opts.csisat_replacement_dir:
        CSISAT_REPLACEMENT_DIR = opts.csisat_replacement_dir
    
    if not opts.config:
        sys.stdout.write('ERROR, at least one --config required\n')
    elif not args:
        sys.stdout.write('ERROR, at least one benchmark required\n')
    else:
        main('cpa',
             map(os.path.abspath, args),
             opts.config, opts.timeout, opts.memlimit, opts.output)
