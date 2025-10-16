package xyz.block.uuidv7

import java.util.UUID

val UUID.timestamp: Long
    get() = UUIDv7.getTimestamp(this)
