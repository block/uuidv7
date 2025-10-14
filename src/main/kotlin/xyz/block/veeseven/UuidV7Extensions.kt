package xyz.block.veeseven

import java.util.UUID

val UUID.timestamp: Long
    get() = UuidV7.getTimestamp(this)
