#!/bin/bash

#g++ -g -ggdb -c *.cpp
#g++ *.o -static-libstdc++ -o BilateralBFS

g++-4.5 -g -ggdb -c *.cpp
g++-4.5 *.o -static-libstdc++ -o BilateralBFS
