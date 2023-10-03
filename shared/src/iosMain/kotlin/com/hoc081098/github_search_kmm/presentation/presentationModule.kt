package com.hoc081098.github_search_kmm.presentation

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val PresentationModule = module {
  factoryOf(GithubSearchViewModel::create)
}
