#!/bin/bash
set -e # exit if return != 0
if [ "$FOLDER" = "lab6" ]; then
    if [ "$OSTYPE" = "darwin17" ]; then   # OS = osx
        wget https://github.com/forax/java-next/releases/download/untagged-bf24edb7ff6b12ce0d49/jdk-15-vector-osx.tar.gz
        tar xvf jdk-15-vector-osx.tar.gz
    else
        # download jdk-15
        wget https://github.com/forax/java-next/releases/download/untagged-bf24edb7ff6b12ce0d49/jdk-15-vector-linux.tar.gz
        # extract
        tar xvf jdk-15-vector-linux.tar.gz
    fi
    # export new environment variable JAVA_HOME
    export JAVA_HOME=$(pwd)/jdk-15-vector/
fi

cd $FOLDER
echo $JAVA_HOME
mvn package

# lauchn benchmark :
$JAVA_HOME/bin/java --add-modules jdk.incubator.vector -jar target/benchmarks.jar 