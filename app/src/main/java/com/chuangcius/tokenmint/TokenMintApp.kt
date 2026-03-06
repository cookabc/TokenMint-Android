package com.chuangcius.tokenmint

import android.app.Application
import com.chuangcius.tokenmint.data.repository.VaultRepository

class TokenMintApp : Application() {

    lateinit var vaultRepository: VaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        vaultRepository = VaultRepository(this)
    }
}
