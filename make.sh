#!/bin/bash

FULLPATH="`pwd`/$0"
DIR=`dirname "$FULLPATH"`

src="$DIR"/src
lib="$DIR"/lib
build="$DIR"/_build
archive="$DIR"/archive
doc="$DIR"/doc

#linux / osx different mktemp call
TMPFILE=`mktemp 2>/dev/null || mktemp -t /tmp`

NOW=`date +"%s"`

jsource=1.6
jtarget=1.6

JAVAC="javac -source $jsource -target $jtarget -nowarn"

#========================================================================
checkAvail()
{
	which "$1" >/dev/null 2>&1
	ret=$?
	if [ $ret -ne 0 ]
	then
		echo "tool \"$1\" not found. please install"
		exit 1
	fi
}

#========================================================================
compile_serial_reader()
{
	echo "building serial reader"
	echo "======================"
	$JAVAC -classpath "$build":"$lib"/jssc-2.8.0.jar:"$lib"/JavaOSC_1456673189.jar -sourcepath "$src" -d "$build" "$src"/*.java
}

#========================================================================
create_javadoc()
{
	echo "creating jssc doc"
	echo "================="

	mkdir -p "$build"/jssc
	cp "$archive"/jssc_2.8.0.zip "$build"/jssc
	cd "$build"/jssc
	unzip -q -q jssc_2.8.0.zip
	cd java-simple-serial-connector-2.8.0/src/java/jssc/
	javadoc -quiet -private -linksource -sourcetab 2 -d "$doc" -classpath "$lib"/jssc-2.8.0.jar -sourcepath . *.java
	cd "$DIR"
}

#========================================================================
build_jar()
{
	echo ""
	echo "creating SerialReader jar"
	echo "========================="

	cur="`pwd`"

	echo "preparing dependencies build..."

	mkdir -p "$build"/jarbuild
	rm -rf "$build"/jarbuild/*
	cp "$build"/*.class "$build"/jarbuild

	cat - > "$build"/jarbuild/Manifest.txt << _EOF_
Manifest-Version: 1.0
Main-Class: SerialReader
_EOF_

	cp "$archive"/jssc_2.8.0.zip "$build"/jarbuild
	cp "$archive"/JavaOSC-master.zip "$build"/jarbuild

	cd "$build"/jarbuild
	unzip -q jssc_2.8.0.zip
	rm -f jssc_2.8.0.zip
	cp -r java-simple-serial-connector-2.8.0/src/java/libs .
	cp -r java-simple-serial-connector-2.8.0/src/java/jssc .
	rm -rf java-simple-serial-connector-2.8.0
	unzip -q JavaOSC-master.zip
	rm JavaOSC-master.zip
	cp -r java_osc-master/src/main/com/ .
	rm -rf java_osc-master

	echo "building dependencies..."

	shopt -s globstar
	$JAVAC jssc/**/*.java
	$JAVAC com/**/*.java
	rm -f jssc/**/*.java
	rm -f com/**/*.java

	echo "creating jar..."

	jar cfm SerialReader_$NOW.jar Manifest.txt *.class libs/ jssc/ com/

	mv SerialReader_$NOW.jar "$build"

	echo "build_jar done."

	echo "starting --help"
	echo "java -jar "$build"/SerialReader_$NOW.jar -h"

	java -jar "$build"/SerialReader_$NOW.jar -h

	cd "$cur"
}

for tool in java javac jar javadoc; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

#create_javadoc
compile_serial_reader
build_jar

echo "done."

echo "list serial ports:"
echo "java -cp "$build"/SerialReader_$NOW.jar ListSerialPorts"

#echo "list serial ports: java -cp "$build":"$lib"/jssc-2.8.0.jar ListSerialPorts"
#echo ""
#echo "use serial reader: java -cp "$build":"$lib"/jssc-2.8.0.jar SerialReader <serial portname> (baudrate)"
#echo ""
#echo "use serial reader osc: java -cp "$build":"$lib"/jssc-2.8.0.jar:"$lib"/JavaOSC_1456673189.jar SerialReaderOSC <serial portname> (baudrate)"

