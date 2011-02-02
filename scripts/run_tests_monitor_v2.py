#!/usr/bin/env python

"""
Script to benchmark various configurations of CPAChecker.
"""

from __future__ import with_statement
import os, sys
import glob
import subprocess
from string import Template
import optparse
import re

# memory limit in bytes (can be overriden on the command line)
MEMORY_LIMIT = 3000000

# time limit in seconds (can be overriden on the command line)
TIME_LIMIT = 10000


CPACHECKER_DIR = os.path.dirname(sys.argv[0])

BLAST_DIR = os.path.abspath(os.path.join(os.path.dirname(sys.argv[0]),
                                         '../blast-2.5-bin'))
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
                       '(scripts/cpa.sh -setprop output.disable=true -config $config $benchmark > '
                       '$benchmark.$cn.log 2>&1)').substitute(locals())
    p = subprocess.Popen(['/bin/bash', '-c', cmdline], shell=False)
    retval = p.wait()
    tot_time, outcome, reached, refinements, abstractions, blocksize, maxtime, avtime, maxtimeonpath, maxsizeofpath, maxbranches, totalreftime, maxreftime = None, None, None, None, None, None, None, None, None, None, None, None, None

    if retval == 0:
        outcome = None
    elif retval == 134:
        outcome = 'ABORTED (probably by Mathsat)'
    elif retval == 137:
        outcome = 'KILLED BY SIGNAL 9 (probably ulimit)'
    elif retval == 143:
        outcome = 'KILLED'
    else:
        outcome = 'ERROR (%d)' % retval

    with open('%s.%s.log' % (benchmark, configname(config))) as f:
        for line in f:
            if tot_time is None and line.startswith(
                'Total Time Elapsed including CFA construction:'):
                tot_time = line[46:].strip()[:-1]
            elif reached is None and line.startswith('Size of reached set:'):
                reached = line[20:].strip()
            elif abstractions is None and line.startswith('Number of abstractions:'):
                abstractions = line[28:].strip()
            elif refinements is None and line.startswith('Number of refinements:'):
                refinements = line[27:].strip()
            elif blocksize is None and line.startswith('Max ABE block size: '):
                blocksize = line[19:].strip()
	    elif maxtime is None and line.startswith('Max. Post Time:'):
                maxtime = line[27:].strip()[:-2]
	    elif avtime is None and line.startswith('Av Post Time:'):
                avtime = line[27:].strip()[:-2]
	    elif maxtimeonpath is None and line.startswith('Max Post time on a path:'):
                maxtimeonpath = line[27:].strip()[:-2]
	    elif maxsizeofpath is None and line.startswith('Max size of a single path:'):
                maxsizeofpath = line[27:].strip()
            elif maxbranches is None and line.startswith('Max number of branches:'):
                maxbranches = line[27:].strip()
            elif totalreftime is None and line.startswith('Time for refinements:'):
                totalreftime = line[35:].strip()[:-1]
            elif maxreftime is None and line.startswith('Max time for refinement:'):
                maxreftime = line[35:].strip()[:-1]
            if (line.find('java.lang.OutOfMemoryError') != -1) or line.startswith('out of memory'):
                outcome = 'OUT OF MEMORY'
            elif line.find('SIGSEGV') != -1:
                outcome = 'SEGMENTATION FAULT'
            elif (outcome is None or outcome == "ERROR (1)") and (line.find('Exception') != -1):
                outcome = 'EXCEPTION'
            elif outcome is None and line.startswith('Given specification violated?'):
                line = line[30:].strip()
                if line.startswith('NO'):
                    outcome = 'SAFE'
                elif line.startswith('YES'):
                    outcome = 'UNSAFE'
                else:
                    outcome = 'UNKNOWN'
            if outcome is None and line.startswith(
                    '#Test cases computed:'):
                outcome = 'OK'
    if tot_time is None:
        tot_time = -1
    if outcome is None:
        outcome = 'UNKNOWN'
    return tot_time, outcome, reached, refinements, abstractions, blocksize, maxtime, avtime, maxtimeonpath, maxsizeofpath, maxbranches, totalreftime, maxreftime
    

def blast_cmdline_for_config(config):
    with open(config) as f:
        return " ".join(s.strip() for s in f.readlines())

def run_single_blast(benchmark, config, time_limit, mem_limit):
    """\
    Runs Blast on a single benchmark instance with the given configuration,
    time limit and memory limit.
    The output is saved in a file $benchmark.$config.log.
    Returns a pair (time, outcome)
    """
    cn = configname(config)
    flags = blast_cmdline_for_config(config)
    logname = '%s.%s.log' % (benchmark, cn)
    csisatdir = CSISAT_REPLACEMENT_DIR
    blastdir = BLAST_DIR
    cmdline = Template('ulimit -t $time_limit -v $mem_limit; '
                       '(PATH=$csisatdir:$blastdir:$$PATH '
                       'time -p pblast.opt $flags $benchmark) > '
                       '$logname 2>&1').substitute(locals())
    p = subprocess.Popen(['/bin/bash','-c',cmdline], shell=False)
    retval = p.wait()
    tot_time, outcome = None, None
    with open(logname) as f:
        for line in f:
            if tot_time is None and line.startswith('real '):
                try:
                    tot_time = float(line[5:].strip())
                except ValueError:
                    pass
            if outcome is None:
                if line.startswith('Error found! The system is unsafe :-('):
                    outcome = 'UNSAFE'
                elif line.startswith('No error found.  The system is safe :-)'):
                    outcome = 'SAFE'
    if tot_time is None:
        tot_time = -1
    if outcome is None:
        outcome = 'UNKNOWN'
    return tot_time, outcome


def main(which, benchmarks, configs, time_limit, mem_limit, outfile,
         order, verbose, live):
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
    if which == 'blast':
        run = run_single_blast
    else:
        run = run_single
        
    results = {}
    for config in configs:
        results[configname(config)] = {}

    def go(b, c, t, m):
        if verbose:
            sys.stdout.write('Running: %s with config: %s... ' %
                             (b, configname(c)))
            sys.stdout.flush()

        subst = re.compile('.*/(benchmarks-[^/]*/)')
        bs = subst.sub(r'\1', b)
        
        write_header = (results[configname(c)] == {})
        
        results[configname(c)][bs] = run(os.path.abspath(b), c, t, m)
        sys.stderr.write('\n')
        if not live:
            return

        if write_header:
            # override the file's content
            mode = 'w'
        else:
            # append to the file
            mode = 'a'

        with open(outfile + '.' + configname(c) + '.log', mode) as out:
                d = results[configname(c)]
                maxlen = max(map(len, d.keys()))
                maxlentime = len(str(time_limit)) + 4
                
                if write_header: 
                    out.write(Template('$i\t$t\t$o\t\tReached\tRefinements\tAbstractions\t\tBlocksize\tMaxTimeForPost\tAvTime\tMaxTimeonPath\tMaxSizeofSinglePath\tMaxbranches\tTotalTimeRefs\tMaxRefTime\n').substitute(i='INSTANCE'.ljust(maxlen), t='TIME'.rjust(maxlentime), o='OUTCOME'))
                    out.write('-' * (maxlen + maxlentime + 86) + '\n')

                line = Template('$fname\t$ftime\t$foutcome\t$freached\t$frefinements\t$fabstractions\t$fblocksize\t$fmaxtime\t$favtime\t$fmaxtimeonpath\t$fmaxsizeofpath\t$fmaxbranches\t$ftotalreftime\t$fmaxreftime')
                for name in sorted(d):
                    fname = name.ljust(maxlen)
                    ftime = str(d[name][0]).rjust(maxlentime)
                    foutcome = d[name][1].ljust(15)
                    freached = (d[name][2] if d[name][2] != None else "").ljust(7)
                    frefinements = (d[name][3] if d[name][3] != None else "").ljust(15)
                    fabstractions = (d[name][4] if d[name][4] != None else "").ljust(22)
	            fblocksize = (d[name][5] if d[name][5] != None else "").ljust(15)
		    fmaxtime = (d[name][6] if d[name][6] != None else "").ljust(12)
		    favtime = (d[name][7] if d[name][7] != None else "").ljust(2)
		    fmaxtimeonpath = (d[name][8] if d[name][8] != None else "").ljust(13)
                    fmaxsizeofpath = (d[name][9] if d[name][9] != None else "").ljust(8)
                    fmaxbranches = (d[name][10] if d[name][10] != None else "").ljust(8)
		    ftotalreftime = (d[name][11] if d[name][11] != None else "").ljust(8)
		    fmaxreftime = d[name][12] if d[name][12] != None else ""
                    out.write(line.substitute(locals()).strip() + '\n')
        
        
	sys.stderr.write(results[configname(c)][bs][1])
	sys.stderr.write('\n')
#        if results[configname(c)][bs][1] == 'ERROR':
#            sys.stderr.write('ERROR\n')
#        elif verbose:
#            sys.stdout.write('DONE\n')

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
        if live:
            # there's nothing to do here anymore
            return
        
        # if not live write out the results
        for config in map(configname, configs):
            with open(outfile + '.' + config + '.log', 'w') as out:
                d = results[config]
                maxlen = max(map(len, d.keys()))
                maxlentime = len(str(time_limit)) + 4
                out.write(Template('$i\t$t\t$o\t\tReached\tRefinements\tAbstractions\t\tBlocksize\tMaxTimeForPost\tAvTime\tMaxTimeonPath\tMaxSizeofSinglePath\tMaxbranches\tTotalTimeRefs\tMaxRefTime\n').substitute(
                    i='INSTANCE'.ljust(maxlen), t='TIME'.rjust(maxlentime),
                    o='OUTCOME'))
                out.write('-' * (maxlen + maxlentime + 86) + '\n')
                line = Template('$fname\t$ftime\t$foutcome\t$freached\t$frefinements\t$fabstractions\t$fblocksize\t$fmaxtime\t$favtime\t$fmaxtimeonpath\t$fmaxsizeofpath\t$fmaxbranches\t$ftotalreftime\t$fmaxreftime')
                for name in sorted(d):
                    fname = name.ljust(maxlen)
                    ftime = str(d[name][0]).rjust(maxlentime)
                    foutcome = d[name][1].ljust(15)
                    freached = (d[name][2] if d[name][2] != None else "").ljust(7)
                    frefinements = (d[name][3] if d[name][3] != None else "").ljust(15)
                    fabstractions = (d[name][4] if d[name][4] != None else "").ljust(22)
	            fblocksize = (d[name][5] if d[name][5] != None else "").ljust(15)
		    fmaxtime = (d[name][6] if d[name][6] != None else "").ljust(12)
		    favtime = (d[name][7] if d[name][7] != None else "").ljust(2)
		    fmaxtimeonpath = (d[name][8] if d[name][8] != None else "").ljust(13)
                    fmaxsizeofpath = (d[name][9] if d[name][9] != None else "").ljust(8)
                    fmaxbranches = (d[name][10] if d[name][10] != None else "").ljust(8)
		    ftotalreftime = (d[name][11] if d[name][11] != None else "").ljust(8)
		    fmaxreftime = d[name][12] if d[name][12] != None else ""
                    out.write(line.substitute(locals()).strip() + '\n')


if __name__ == '__main__':
    p = optparse.OptionParser()
    p.add_option('-c', '--config', action='append')
    p.add_option('-t', '--timeout', type='int', default=TIME_LIMIT)
    p.add_option('-m', '--memlimit', type='int', default=MEMORY_LIMIT)
    p.add_option('-o', '--output', default='results')
    p.add_option('--blast', action='store_true')
    p.add_option('--blast_dir')
    p.add_option('--cpachecker_dir')
    p.add_option('--csisat_replacement_dir')
    p.add_option('--live', action='store_true')

    opts, args = p.parse_args()

    if opts.blast_dir:
        BLAST_DIR = opts.blast_dir
    if opts.cpachecker_dir:
        CPACHECKER_DIR = opts.cpachecker_dir
    if opts.csisat_replacement_dir:
        CSISAT_REPLACEMENT_DIR = opts.csisat_replacement_dir

    if not opts.config:
        sys.stdout.write('ERROR, at least one --config required\n')
    elif not args:
        sys.stdout.write('ERROR, at least one benchmark required\n')
    else:
        main('blast' if opts.blast else 'cpa',
             args,
             opts.config,
             opts.timeout,
             opts.memlimit,
             opts.output,
             'config',
             True, # be verbose
             opts.live)
