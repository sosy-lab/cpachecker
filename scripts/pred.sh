./scripts/cpa.sh -timelimit 60s -preprocess -predicateAnalysis -spec sv-comp-reachability example/hand-crafted/even2.c
firefox output/Report.html &
