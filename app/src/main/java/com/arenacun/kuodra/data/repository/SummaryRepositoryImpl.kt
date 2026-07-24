package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.repository.CategoryRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository

class SummaryRepositoryImpl(
    private val categoryRepository: CategoryRepository,
) : SummaryRepository {

    /** Catálogo vigente desde Room (snapshot síncrono del [CategoryRepository]). */
    override fun categories(): List<Category> = categoryRepository.categories.value
}
