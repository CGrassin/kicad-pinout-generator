#!/bin/sh

mkdir release
cp metadata.json release/

mkdir release/resources
cp icon.png release/resources/

mkdir release/plugins
cp ../__init__.py release/plugins/
cp ../logo.png release/plugins/
cp ../pinout_generator_result.py release/plugins/
cp ../pinout_plugin.py release/plugins/

cd release
version=$(gawk '/"version"/{print gensub(/.*:.*"([0-9.]+)".*/,"\\1","g")}' metadata.json)
zip -r "../kicad-pinout-generator_$version.zip" .
cd ..
rm -R release
