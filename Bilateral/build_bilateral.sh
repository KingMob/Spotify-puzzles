#!/bin/bash
g++ -g -ggdb -c *.cpp
g++ *.o -static-libstdc++ -o BilateralBFS
