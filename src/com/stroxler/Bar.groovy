#!/usr/bin/env groovy
package com.stroxler

import java.lang.Thread

class Globals {
  public static int numTimesCalled = 0
}

def squareOfTwo() {
    return 2 * 2
}


def printSquareOfTwo() {
    Globals.numTimesCalled += 1
    println("------------")
    println(squareOfTwo())
    println("------------")
    println("called " + Globals.numTimesCalled + " times")
    println("------------")
    Thread.sleep(10 * 1000)
}
