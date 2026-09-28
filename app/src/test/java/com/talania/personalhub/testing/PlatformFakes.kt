package com.talania.personalhub.testing

import com.talania.personalhub.core.model.DomainError
import com.talania.personalhub.core.model.DomainResult
import com.talania.personalhub.core.model.HubId
import com.talania.personalhub.core.platform.IdGenerator
import com.talania.personalhub.core.platform.PrivateFiles

class FakeIdGenerator : IdGenerator {
    private var sequence = 0L
    override fun next(): HubId = HubId(java.util.UUID(0, ++sequence).toString())
}

class FakePrivateFiles : PrivateFiles {
    private val content = mutableMapOf<String, ByteArray>()
    var unavailable = false

    override suspend fun read(relativePath: String): DomainResult<ByteArray> = when {
        unavailable -> DomainResult.Failure(DomainError.Storage)

        else -> content[relativePath]?.let { DomainResult.Success(it.copyOf()) }
            ?: DomainResult.Failure(DomainError.NotFound)
    }

    override suspend fun write(relativePath: String, bytes: ByteArray): DomainResult<Unit> {
        if (unavailable) return DomainResult.Failure(DomainError.Storage)
        content[relativePath] = bytes.copyOf()
        return DomainResult.Success(Unit)
    }
}
