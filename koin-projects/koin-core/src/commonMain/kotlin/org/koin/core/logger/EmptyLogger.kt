package org.koin.core.logger

/**
 * No loggings Logger
 */
class EmptyLogger : Logger(Level.NONE) {

    override fun log(level: Level, msg: MESSAGE) {
        println("should see this - $level - $msg")
    }
}