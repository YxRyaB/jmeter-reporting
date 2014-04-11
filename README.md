JMeter-Reporting [![Build Status](https://lucaspouzac.ci.cloudbees.com/buildStatus/icon?job=jmeter-reporting)](https://lucaspouzac.ci.cloudbees.com/job/jmeter-reporting/)
================

In development

Pre-requisite : Default local MongoDB installation (port 27017) or add -Dmongo.uri=[path] for remote connection.

Run tests YAML : mvn clean test (add -Dhttp.proxyHost=host -Dhttp.proxyPort=port if you are behind a proxy to download MongoDB)

Import eclipse : mvn eclipse:clean eclipse:eclipse

Run App : Run As Java Application org.jmeter.reporting.AppServer

Admin console (admin/admin) : http://localhost:8080/api/@/ui/

Games : http://localhost:8080/api/@/ui/api-docs/
