#!/bin/bash
cp='lib/mariadb-java-client-2.5.1.jar:src'
script=`basename $0 .sh`.groovy
groovy -cp $cp src/$script "$@"

