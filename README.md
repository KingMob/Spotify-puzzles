Intro
=====

This is a collection of solutions to the Spotify puzzles from 2012. 

As of this writing (Mar 2013), the puzzles have been changed. The original puzzles can be seen at [The Wayback Machine](http://web.archive.org/web/20120818125155/http://www.spotify.com/us/jobs/tech/).

Notes
=====

The first two puzzles, Lottery and BestBefore, were relatively easy, and written in Java. Instructions are in the individual READMEs for them.

The *Heavy Metal* puzzle, Bilateral, was quite a bit more involved. There are multiple versions in Java, then conformant C++11, and then finally, some modifications to work with GCC 4.5.2, which the Spotify verifier was using to build the executables.

An initial, naive solution quickly proved incorrect. After a bit of thinking and digging around, I realized that the problem was a bipartite maximum matching, minimum vertex cover problem.

I produced a working version in Java, but the verifier said it was too slow. Unfortunately, I violated the number one rule of optimization: profile first, code second.

Instead, I assumed the issue was an insufficiently fast algorithm. I ended up writing several versions trying to get it fast enough, using variants of simple breadth-first search, Hopcroft-Karp and push-relabel max flow algorithms.

After getting sufficiently exasperated (since I knew the core parts could complete the largest problem in under 200 ms), I decided to time it end-to-end, and lo and behold, the vast majority of the time taken was simple JVM start-up time (a few seconds). My algorithm choice was (probably) irrelevant.

After that, I chose the simplest algorithm, the breadth-first search for augmenting paths, and rewrote in C++, which was more than fast enough. I hadn't used C++ since college, so it was a welcome chance to see what had changed in the language. Unfortunately, the version of GCC used by Spotify's verifier was slightly older, and certain standard library names hadn't quite stabilized (e.g., .fill() vs .assign()), but after a last push of fiddling with compiler flags, it was done!
