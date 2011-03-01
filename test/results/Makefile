export PATH=../../scripts/results:$(shell echo $$PATH)
COLUMNS=2

table.tex : table.csv ;
	cat "$<" | round.sh | csv_to_latex.sh > "$@"

table.csv : $(addsuffix .csv,$(basename $(wildcard *.log))) ;
	column_select.sh 1,$(addprefix ${COLUMNS} ,$+) > "$@"

csv : $(addsuffix .csv,$(basename $(wildcard *.log)))

.PHONY : clean ;

test_%.csv : test_%.log ;
	cat "$<" | log_to_csv.sh > "$@"

clean : ;
	-rm *.csv table.tex table.log 2> /dev/null
