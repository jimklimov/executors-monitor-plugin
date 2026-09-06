# Executors Monitor Plugin

This is a simple link:http://javadoc.jenkins-ci.org/hudson/node_monitors/NodeMonitor.html[NodeMonitor]
to report the "busy/total" executor counts on the Nodes overview page.

It does not define any thresholds to offline "misbehaving" nodes.

The reported column is colorized:

* blue for "0 busy",
* green for "busy < total",
* unmodified for "busy == total",
* red for "busy > total" if that ever occurs (e.g. config change during run-time)

