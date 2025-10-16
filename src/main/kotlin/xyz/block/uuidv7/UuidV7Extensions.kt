package xyz.block.uuidv7

import java.util.UUID

val UUID.timestamp: Long
    get() = UuidV7.getTimestamp(this)
