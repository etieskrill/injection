package org.etieskrill.engine.time

import kotlin.time.Duration

expect fun setSystemTimeResolution(resolution: Duration)

expect fun resetSystemTimeResolution(resolution: Duration)
