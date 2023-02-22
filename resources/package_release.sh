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
zip -r ../release.zip .
cd ..
rm -R release