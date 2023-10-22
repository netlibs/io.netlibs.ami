# ARI Client

Designed for use with Java 21 virtual threads.


## Frames

Using ARI directly is complicated to get right.  So a "frame" based layer is provided which abstracts much of the complexity of common ARI type operations.  Instead of calling a set of imperative methods, the frame based system is declarative - you provide a definition of what you want the state of a channel to be (for example, playing a file) and the framing layer uses the state of the channels to calculate the operations to call, returning once the preconditions have been met.
