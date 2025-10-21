#!/bin/bash

bin/cpachecker \
  --svcomp26 \
  --spec config/properties/unreach-call.prp \
  doc/examples/example-safe.c