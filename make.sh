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
	unzip -q jssc_2.8.0.zip
	cd java-simple-serial-connector-2.8.0/src/java/jssc/
	javadoc -quiet -private -linksource -sourcetab 2 -d "$doc" -classpath "$lib"/jssc-2.8.0.jar -sourcepath . *.java
	cd "$DIR"
}

for tool in java javac jar javadoc; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

#create_javadoc
compile_serial_reader

echo "done."
echo "list serial ports: java -cp "$build":"$lib"/jssc-2.8.0.jar ListSerialPorts"
echo ""
echo "use serial reader: java -cp "$build":"$lib"/jssc-2.8.0.jar SerialReader <serial portname> (baudrate)"
echo ""
echo "use serial reader osc: java -cp "$build":"$lib"/jssc-2.8.0.jar:"$lib"/JavaOSC_1456673189.jar SerialReaderOSC <serial portname> (baudrate)"
