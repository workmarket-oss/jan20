[![Build Status](https://travis-ci.org/workmarket-oss/jan20.svg?branch=master)](https://travis-ci.org/workmarket-oss/jan20)
[![codecov.io](https://codecov.io/gh/workmarket-oss/jan20/branch/master/graph/badge.svg?branch=master)](https://codecov.io/gh/workmarket-oss/jan20?branch=master)

## What is it?

Jan20 is a succession framework which allows for comparing results from various code execution paths. The framework is useful when equality cannot be defined simply (e.g. when comparing payloads when one is a superset of the other's data). Work Market uses Jan20 during extraction of functionality from one system (our monolith) or another (our microservice architecture).

For a more detailed explanation, see our [blog post](https://medium.com/@drewcsillag/a-succession-library-for-java-3beeab7acf9c#.c0a0eftpo). 

## How to use:

```groovy
repositories {
  jcenter()
}

dependencies {
  compile 'com.workmarket:jan20:1.0'
}
```

## Reasoning behind the name:

Jan 20 is a reference to when US presidential succession occurs.

## Contribution:

Please see [CONTRIBUTING.md](https://github.com/workmarket-oss/jan20/blob/master/CONTRIBUTING.md)
