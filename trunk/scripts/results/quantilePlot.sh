#!/bin/bash
#

# Input:
# This script assumes as first parameter a directory that contains
# several other directories with benchmark results, formatted as tab-separated values (.csv),
# where the first column is the program name, the second column is the result, and the third column is the run time.
# The script extracts the successfully solved instances, by accepting only rows that contain SAFE or UNSAFE,
# and if the program name contains BUG or unsafe, the expected result is declared to be UNSAFE.
#
# Output:
# The scipt writes a Gnuplot file 'quantilePlotShow.gp', which can be turned into a PDF file 'quantilePlot.pdf' using:
# ./quantilePlot.sh tables_per_tool_and_test && gnuplot quantilePlotShow.gp && evince quantilePlot.pdf
#
# Example:
#   tables/cpachecker1/systemc.csv
#   tables/cpachecker-abe/cpachecker-abe.control.table.csv
#   tables/cpachecker-abe/cpachecker-abe.systemc.table.csv
#   tables/cpachecker-abm/cpachecker-abm.control.table.csv
#   tables/cpachecker-abm/cpachecker-abm.systemc.table.csv

DIR="$1";

COLORS=(green red blue black navy magenta dark-cyan brown dark-violet sea-green);

echo "
set terminal pdfcairo font \",9\" size 20cm,14cm
set bmargin 0
set lmargin 9
set rmargin 3
set yrange [1:1000]
unset xlabel
unset xtics
set ylabel 'Time in s'
set key left top
set logscale y 10
set pointsize 1.0
set output 'quantilePlot.pdf'
set multiplot layout 2,1
set size 1,0.86
set origin 0,0.14
" > quantilePlotShow.gp;

CMD="plot ";
k=0;
XRANGE=0;
for i in `ls $DIR`; do
  echo "$k: $i";

  if [ -f "tmp.data" ]
  then
      rm tmp.data
  fi

  for j in `ls $DIR/$i/*.csv`; do

    echo "  $j";

    cat $j \
      | while read FILENAME RESULT RUNTIME REST; do
          EXPECTEDRESULT="SAFE";
          echo $FILENAME | egrep "(BUG|unsafe)" > /dev/null && EXPECTEDRESULT="UNSAFE";
          if [ "$RESULT" = "SAFE"   -a "$EXPECTEDRESULT" = "SAFE"   ]; then
            echo "$RUNTIME" >> tmp.data;
          fi
          if [ "$RESULT" = "UNSAFE" -a "$EXPECTEDRESULT" = "UNSAFE" ]; then
            echo "$RUNTIME" >> tmp.data;
          fi
        done;
  done;
  sort -g tmp.data | grep -n "" | sed "s/:/\t/" > tab_$i.data;
  NRVALUES=`cat tab_$i.data | wc -l`;
  if [ "$NRVALUES" -ge "$XRANGE" ]; then
    XRANGE=$NRVALUES;
  fi
  POINTINTERVAL=$(($XRANGE / 15));
  CMD="$CMD 'tab_$i.data' using 1:2 with linespoints linecolor rgb \"${COLORS[$k]}\" pointinterval $POINTINTERVAL linewidth 3.5 title '$i',";
  k=$(($k + 1));
done;

echo "set xrange [0:$XRANGE]
" >> quantilePlotShow.gp;
echo "$CMD%" | sed "s/,%/;/" >> quantilePlotShow.gp;

echo "
set yrange [0.01:1]
unset key
unset bmargin
set tmargin 0
set xtics nomirror
unset ytics
unset ylabel
unset logscale
set size 1,0.14
set origin 0,0
set xlabel 'n-th fastest result'
" >> quantilePlotShow.gp;

echo "$CMD%" | sed "s/,%/;/" >> quantilePlotShow.gp;


