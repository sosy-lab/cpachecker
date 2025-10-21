#!/bin/bash

bin/cpachecker \
  --svcomp26 \
  --preprocess \
  --spec config/properties/unreach-call.prp \
  doc/examples/example-safe.c

bin/cpachecker \
  --svcomp26 \
  --preprocess \
  --spec config/properties/unreach-call.prp \
  doc/examples/example-unsafe.c

bin/cpachecker \
  --svcomp26 \
  --preprocess \
  --spec config/properties/valid-memsafety.prp \
  doc/examples/example-unsafe-memsafety.c