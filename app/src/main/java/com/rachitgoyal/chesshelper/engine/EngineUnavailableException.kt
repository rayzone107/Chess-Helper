package com.rachitgoyal.chesshelper.engine

class EngineUnavailableException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
