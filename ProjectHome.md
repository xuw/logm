## Introduction ##
This project is one of the [RAD Lab](http://radlab.cs.berkeley.edu) research project of [UC Berkeley](http://www.berkeley.edu), and it is [my](http://www.cs.berkeley.edu/~xuw) Ph.d. dissertation project.

When a datacenter-scale service consisting of hundreds of software components running on thousands of computers misbehaves, developer-operators need every tool at their disposal to troubleshoot and diagnose operational problems. Ironically, there is one source of information that is built into almost every piece of software that provides detailed information that reflects the original developers’ ideas about noteworthy or unusual events, but is typically ignored: the humble console log.

Since the dawn of programming, developers have used everything from printf to complex logging and monitoring libraries to record program variable values, trace execution, report runtime statistics, and even print out full-sentence messages designed to be read by a human—usually by the developer herself.


We show that we can automatically discover abnormal and potentially interesting messages from the vast amount of free text logs. Different from exiting solutions, we analyze these messages, especially program trace messages, in a fully automatic way.


We have implemented a set of tools using a combination of program analysis, information retrieval and machine learning techniques to perform the analysis:

1) Parse textual logs and extract semi-structured information;

2) Automatically group related log messages to reconstruct execution traces;

3) Use machine learning methods to detect abnormal traces.

One key observation is that the typical console message is much more structured than it appears: the definition of its ``schema is implicit in the log printing statements, which can be recovered from program source code. This observation is key to our log parsing approach, which yields detailed and accurate message structure recovery, feature construction and problem detection. The parsing makes it very easy and flexible to create a variety of (generic or application-specific) features, so that powerful machine learning methods can be applied to perform high quality pattern mining and accurate problem detection.

Our approach can run either online to provide near-real-time log analysis or in batch to analyze large archives of logs in short time.


## Please see the following publications ##

Online system problem detection by mining patterns of console logs, Wei Xu, Ling Huang, Armando Fox, David Patterson, and Michael Jordan. To appear in the IEEE International Conference on Data Mining (ICDM’ 09), Miami, FL, December 2009

http://www.cs.berkeley.edu/~xuw/files/icdm09.pdf

Large-scale system problem detection by mining console logs, Wei Xu, Ling Huang, Armando Fox, David Patterson, and Michael Jordan. In Proceedings of the 22nd ACM Symposium on Operating Systems Principles (SOSP’ 09), Big Sky, MT, October 2009

http://www.cs.berkeley.edu/~xuw/files/sosp09.pdf