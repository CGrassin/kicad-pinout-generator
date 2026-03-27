#!/bin/sh

rm -f ./output/* &&
python3 abc2db.py && 
rm -f ../Irish_Whistle/app/src/main/res/raw/* && 
mv ./output/* ../Irish_Whistle/app/src/main/res/raw/