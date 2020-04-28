./scripts/cpa.sh -heap 2000M -timelimit 10s -preprocess -bmc-LBE -stats -spec sv-comp-reachability $1
firefox output/Report.html &
