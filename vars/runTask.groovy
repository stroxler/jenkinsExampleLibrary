#!/usr/bin/env groovy

import com.stroxler.Bar

def call(taskName, f) {
    new Bar().runTask(taskName, f)
}
