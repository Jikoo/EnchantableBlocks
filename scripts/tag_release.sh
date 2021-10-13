#!/bin/bash

if [[ ! $1 ]]; then
  echo "Please provide a version string."
  return
fi

version="$1"
snapshot="${version%.*}.$((${version##*.} + 1))-SNAPSHOT"

mvn versions:set -DnewVersion="$version"

git add .
git commit -S -m "Bump version to $version for release"
git tag -s "$version" -m "Release $version"

mvn clean package -am

mvn versions:set -DnewVersion="$snapshot"

git add .
git commit -S -m "Bump version to $snapshot for development"
