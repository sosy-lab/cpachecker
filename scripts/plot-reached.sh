#!/bin/bash

tail -f ./output/iteration-stats.csv | \
    feedgnuplot \
    --domain \
    --points \
    --legend 0 "reached" \
    --legend 1 "wait" \
    --y2 1 \
    --stream

