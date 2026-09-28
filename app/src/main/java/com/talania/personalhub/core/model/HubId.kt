package com.talania.personalhub.core.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class HubId(val value: String) {
    init {
        require(UUID.fromString(value).toString() == value) { "Expected canonical UUID" }
    }
}
