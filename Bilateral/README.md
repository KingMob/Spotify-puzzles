Compile
=======
./build_bilateral.sh

*NB: At the time I wrote these, the Spotify puzzle verifier used a particular version of GCC, 4.5.2.* 

As a result, certain modifications had to be made to load in the particular libraries needed. If you wish to run this with standard C++11, you may have to make some changes.

If you're on a Mac, double-check the version. On my machine running Lion, the default GCC is 4.2. It will compile correctly, but not run correctly.

Run
===
./BilateralBFS < test-1.txt
