#!/usr/bin/env groovy

import com.stroxler.Bar

def call() {
    echo "Hello from my shared library"
    Bar.printSquareOfTwo()
    echo "Goodbye from my shared library"
}


call()
