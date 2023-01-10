if test -f ../test/results/microbenchmark-condensed.txt; then
    echo "Deleting old microbenchmark-condensed.txt"
    rm ../test/results/microbenchmark-condensed.txt
fi

if test -f ../test/results/microbenchmark-run-times.txt; then
    echo "Deleting old microbenchmark-run-times.txt"
    rm ../test/results/microbenchmark-run-times.txt
fi

echo "Looking for files"
for d in $(find ../test/results -name "output.txt")
do

    echo "Parsing file " $d
    echo $d >> ../test/results/microbenchmark-condensed.txt
    while read line; do
        if [[ $line =~ ^[0-9] ]]; then
            echo -n $d";" >> ../test/results/microbenchmark-run-times.txt
            echo $line >> ../test/results/microbenchmark-run-times.txt
        fi

        echo $line >> ../test/results/microbenchmark-condensed.txt
    done < $d

    echo -e '\n' >> ../test/results/microbenchmark-condensed.txt
done
