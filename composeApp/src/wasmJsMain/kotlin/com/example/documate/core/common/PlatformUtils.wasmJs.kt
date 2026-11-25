package com.example.documate.core.common

import kotlin.js.ExperimentalJsExport

@OptIn(ExperimentalJsExport::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun currentTimeMillis(): Long = jsDateNow().toLong()
