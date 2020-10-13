FROM openjdk:11.0-jdk

WORKDIR /usr/app

RUN apt-get update && apt-get install -y \
  graphviz \
  libgmp10 \
  libgomp1 \
  python3 \
  ant

COPY . /usr/app
RUN ant build