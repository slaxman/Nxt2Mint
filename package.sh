#!/bin/sh

######################
# Package Nxt2Mint   #
######################

if [ -z "$1" ] ; then
  echo "You must specify the version to package"
  exit 1
fi

VERSION="$1"

if [ ! -d package ] ; then
  mkdir package
fi

cd package
rm -R *
cp ../ChangeLog.txt ../LICENSE ../README.md ../Nxt2Mint.conf ../mint.bat ../mint.sh .
cp ../target/Nxt2Mint-$VERSION.jar .
cp -R ../target/lib lib
zip -r Nxt2Mint-$VERSION.zip ChangeLog.txt LICENSE README.md Nxt2Mint.conf mint.bat mint.sh Nxt2Mint-$VERSION.jar lib
dos2unix ChangeLog.txt LICENSE README.md Nxt2Mint.conf mint.bat mint.sh
tar zcf Nxt2Mint-$VERSION.tar.gz ChangeLog.txt LICENSE README.md Nxt2Mint.conf mint.bat mint.sh Nxt2Mint-$VERSION.jar lib
exit 0

