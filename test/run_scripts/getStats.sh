#!/bin/bash

echo 'Time for Analysis:'
cat $@ | grep 'Time for Analysis:' | sort |  awk '{ sum+=$4 } END {print sum / 2}'

echo 'Total time on ART computation:'
cat $@ | grep 'Total time on ART computation:' | sort |  awk '{ sum+=$6 } END {print sum / 2}'

echo 'Total time on ref.:' 
cat $@ | grep 'Total time on ref.:' | sort |  awk '{ sum+=$5 } END {print sum / 2}'

echo 'Time for env. precision adjustment:'
cat $@ | grep 'Time for env. precision adjustment:' | sort |  awk '{ sum+=$6 } END {print sum / 2}'

echo 'No of all successors:'
cat $@ | grep 'No of all successors:' | sort |  awk '{ sum+=$5 } END {print sum /2}'

echo 'No of environmental successors:'
cat $@ | grep 'No of environmental successors:' | sort |  awk '{ sum+=$5 } END {print sum /2}'

echo 'Max. predicates per location'
cat $@ | grep 'Max. predicates per location' | awk '$5 > max {max=$5} END{ print max}'

echo 'All valid transitions by thread:'
cat $@ | grep 'All valid transitions by thread:' | awk '$6 > max {max=$6} END{ print max}'


echo 'Number of refinements:'

cat $@ | grep 'Number of refinements:'