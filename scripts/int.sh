./scripts/cpa.sh -timelimit 60s -preprocess -bmc-interpolation -setprop cpa.loopbound.maxLoopIterationsUpperBound=2 example/hand-crafted/even2.c
firefox output/Report.html &
