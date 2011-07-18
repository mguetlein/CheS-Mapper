#!/bin/bash

if [ -z "$1" ]
then
	echo "Pls give keystore password as first param"
	exit 1
else
	password=$1
fi

if [ -z "$2" ]
then
	echo "Pls give keystore name as second param"
	exit 1
else
	keystore=$2
fi

if [ -z "$3" ]
then
	echo "Pls give keystore alias as third param"
	exit 1
else
	alias=$3
fi

echo "sign deploy/ches-mapper.jar"
jarsigner -storepass $password -keystore $keystore deploy/ches-mapper.jar $alias

for s in `find deploy/ches-mapper_lib/ -name \*jar`; do
  echo "sign $s"
  jarsigner -storepass $password -keystore $keystore $s $alias
done
