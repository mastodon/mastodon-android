#!/bin/bash

version=$(grep 'versionCode' mastodon/build.gradle | cut -w -f 3)
pushd "fastlane/metadata/android/en-US/changelogs"
newFile="$version.txt"
vim "$newFile" || exit 1
rm default.txt
ln -s "$newFile" default.txt
popd
