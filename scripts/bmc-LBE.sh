./scripts/cpa.sh -heap 2000M -timelimit 10s -preprocess -bmc-LBE -setprop cpa.loopbound.maxLoopIterationsUpperBound=10 -stats -spec sv-comp-reachability $1
firefox output/Report.html &
