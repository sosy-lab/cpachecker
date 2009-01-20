gcc -I/localhome/erkan/jdk1.6.0_07/include/ -I/localhome/erkan/jdk1.6.0_07/include/linux/ -I/usr/local/include/oct -g -O2 -g -DOCT_HAS_MPFR -I/usr/lib/ocaml/3.10.0 -DOCT_HAS_MPFR -DOCT_HAS_GMP -DOCT_ENABLE_ASSERT -DOCT_NUM_FLOAT -DOCT_PREFIX=CAT\(octfag_ -c OctWrapper.c
echo "compiled"
ld -G -L/usr/local/lib -loct_fag -lmpfr -lgmp -lm OctWrapper.o -o libJOct.so -lm -lc -lpthread
echo "linked"
cp libJOct.so nativeLibs/
echo "copied"
