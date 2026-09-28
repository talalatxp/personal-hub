package com.talania.personalhub.core.platform

import com.talania.personalhub.core.model.HubId
import java.util.UUID
import javax.inject.Inject

fun interface IdGenerator {
    fun next(): HubId
}

class UuidGenerator @Inject constructor() : IdGenerator {
    override fun next(): HubId = HubId(UUID.randomUUID().toString())
}
