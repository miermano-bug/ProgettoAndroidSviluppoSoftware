package it.unisannio.soscity.soscity_app.util

import it.unisannio.soscity.soscity_app.data.repository.FakeRepository
import it.unisannio.soscity.soscity_app.data.repository.RealRepository
import it.unisannio.soscity.soscity_app.data.repository.Repository

object RepositoryProvider {

    // CAMBIA QUESTO A false PER USARE IL BACKEND REALE
    private const val USE_FAKE_REPOSITORY = false

    fun provideRepository(): Repository {
        return if (USE_FAKE_REPOSITORY) {
            FakeRepository()
        } else {
            RealRepository()
        }
    }
}