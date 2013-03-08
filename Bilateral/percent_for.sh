#!/bin/bash
grep [0-9]% $1 | grep $2 | cut -c 6-10 | paste -s -d + - | bc
