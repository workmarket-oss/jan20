[![Build Status](https://travis-ci.org/workmarket-oss/jan20.svg?branch=master)](https://travis-ci.org/workmarket-oss/jan20)
[![codecov.io](https://codecov.io/gh/workmarket-oss/jan20/branch/master/graph/badge.svg?branch=master)](https://codecov.io/gh/workmarket-oss/jan20?branch=master)
[![Download](https://api.bintray.com/packages/workmarketossdev/maven/jan20/images/download.svg) ](https://bintray.com/workmarketossdev/maven/jan20/_latestVersion)

## What is it?

Jan20 is a succession framework which allows for comparing results from various code execution paths. It's much like Github Scientist. The framework is useful when equality cannot be defined simply (e.g. when comparing payloads when one is a superset of the other's data). Work Market uses Jan20 during extraction of functionality from one system (our monolith) to another (our microservice architecture).

For a more detailed explanation, see our [blog post](https://medium.com/@drewcsillag/a-succession-library-for-java-3beeab7acf9c#.c0a0eftpo). 

## How to use:

```groovy
repositories {
  jcenter()
}

dependencies {
  compile 'com.workmarket:jan20:1.0.1'
}
```

## Demo code
A demonstration of how to use it and the circumstances it reports the various metrics
is [here](https://github.com/workmarket-oss/jan20/blob/master/demo/src/main/java/com/workmarket/jan20/demo/Demo.java)

## JavaDoc
Javadocs are [here](https://workmarket-oss.github.io/jan20/javadoc/)

## Reasoning behind the name:

Jan 20 is a reference to when US presidential succession occurs.

## Contribution:

Please see [CONTRIBUTING.md](https://github.com/workmarket-oss/jan20/blob/master/CONTRIBUTING.md)

