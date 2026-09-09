# Executors Monitor Plugin for Jenkins

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/executors-monitor.svg)](https://plugins.jenkins.io/executors-monitor)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/executors-monitor.svg?color=blue&label=installations)](https://plugins.jenkins.io/executors-monitor)
[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Fexecutors-monitor-plugin%2Fmaster)](https://ci.jenkins.io/job/Plugins/job/executors-monitor-plugin/job/master/)
[![Jenkins Security Scan](https://github.com/jenkinsci/executors-monitor-plugin/actions/workflows/jenkins-security-scan.yml/badge.svg)](https://github.com/jenkinsci/executors-monitor-plugin/actions/workflows/jenkins-security-scan.yml)
[![Changelog](https://img.shields.io/github/release/jenkinsci/executors-monitor-plugin.svg?label=changelog)](https://github.com/jenkinsci/executors-monitor-plugin/releases/latest)

This is a simple [NodeMonitor](http://javadoc.jenkins-ci.org/hudson/node_monitors/NodeMonitor.html)
to report the "busy/total/configured" executor counts on the Nodes overview page.

It does not define any thresholds to offline "misbehaving" nodes.

Includes a configurable option for the reported column to be colorized:

* blue for "0 busy",
* green for "busy < total",
* unmodified for "busy == total ( == configured)",
* red for "busy > configured" if that ever occurs (e.g. config change during run-time)

NOTE: The "busy" value represents executors actually processing some work,
the "configured" value is set in the computer (build agent) connfiguration
as the goal for maximum parallelized workload on that agent, and the "total"
value represents the actual momentary maximum (usually same as "configured",
or larger than it if the configuration was reduced but more executors were
running and still remain active -- then the "total" value would shrink over
time until it matches "configured").

## Example of colorized output

![Colorized "busy/total" example from an earlier version](doc/images/screenshot2.png "Colorized busy/total example from an earlier version")
* *Colorized "busy/total" example from an earlier version*

## Contributing

To build, check out the code, experiment, and run `mvn package` to fire all
the required tests. Keep watch on `mvn spotless:apply` formatting standard.

All source code is licensed under the MIT license.
