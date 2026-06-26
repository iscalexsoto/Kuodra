package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.KuodraSeedSource
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SummaryRepository

class SummaryRepositoryImpl(
    private val seed: KuodraSeedSource,
) : SummaryRepository {

    override fun people(useCase: UseCase): List<Person> = seed.people(useCase)

    override fun categories(): List<Category> = seed.categories
}
