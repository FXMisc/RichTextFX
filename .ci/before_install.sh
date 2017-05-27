#!/usr/bin/env bash

# Taken from TestFX and modified slightly
if [[ "${TRAVIS_OS_NAME}" == osx ]]; then
  brew update
  brew cask install java
  brew install gradle
  brew unlink python # fixes 'run_one_line' is not defined error in backtrace
fi