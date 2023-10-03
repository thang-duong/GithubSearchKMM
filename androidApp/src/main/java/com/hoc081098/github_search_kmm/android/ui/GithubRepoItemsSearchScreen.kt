package com.hoc081098.github_search_kmm.android.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoc081098.github_search_kmm.android.R
import com.hoc081098.github_search_kmm.android.compose_utils.CollectWithLifecycleEffect
import com.hoc081098.github_search_kmm.android.compose_utils.rememberStableCoroutineScope
import com.hoc081098.github_search_kmm.android.core_ui.AppBackground
import com.hoc081098.github_search_kmm.android.core_ui.AppTheme
import com.hoc081098.github_search_kmm.android.core_ui.LoadingIndicator
import com.hoc081098.github_search_kmm.android.core_ui.RetryButton
import com.hoc081098.github_search_kmm.android.core_ui.getReadableMessage
import com.hoc081098.github_search_kmm.domain.model.Owner
import com.hoc081098.github_search_kmm.domain.model.RepoItem
import com.hoc081098.github_search_kmm.presentation.DaggerGithubSearchViewModel
import com.hoc081098.github_search_kmm.presentation.GithubSearchAction
import com.hoc081098.github_search_kmm.presentation.GithubSearchSingleEvent
import com.hoc081098.github_search_kmm.presentation.GithubSearchState
import com.hoc081098.github_search_kmm.presentation.GithubSearchState.Companion.FIRST_PAGE
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalLayoutApi::class,
)
@Composable
fun GithubRepoItemsSearchScreen(
  modifier: Modifier = Modifier,
  vm: DaggerGithubSearchViewModel = hiltViewModel(),
) {
  val snackbarHostState = remember { SnackbarHostState() }

  val context = LocalContext.current
  val scope = rememberStableCoroutineScope()

  vm.eventFlow.CollectWithLifecycleEffect { event ->
    when (event) {
      is GithubSearchSingleEvent.SearchFailure -> {
        scope.launch {
          snackbarHostState.showSnackbar(
            event
              .appError
              .getReadableMessage(context)
          )
        }
      }
      GithubSearchSingleEvent.ReachedMaxItems -> {
        scope.launch {
          snackbarHostState.showSnackbar(
            context.getString(R.string.loaded_all_items)
          )
        }
      }
    }
  }

  val state by vm.stateFlow.collectAsStateWithLifecycle()
  val term by vm.termStateFlow.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)

  val dispatch: (GithubSearchAction) -> Unit = remember { { vm.dispatch(it) } }

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(text = stringResource(id = R.string.app_name))
        }
      )
    },
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .systemBarsPadding()
          .fillMaxWidth()
          .wrapContentHeight(Alignment.Bottom)
      )
    }
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        GithubSearchTermBox(
          modifier = Modifier.fillMaxWidth(),
          term = term,
          onTermChanged = { dispatch(GithubSearchAction.Search(term = it)) },
        )

        if (state.term.isNotBlank()) {
          Text(
            modifier = Modifier
              .fillMaxWidth()
              .padding(
                start = 16.dp,
                bottom = 16.dp,
                end = 16.dp
              ),
            text = "Search results for '${state.term}'",
            style = MaterialTheme.typography.bodySmall
          )
        }

        GithubRepoItemsSearchContent(
          modifier = Modifier.weight(1f),
          state = state,
          onRetry = { dispatch(GithubSearchAction.Retry) },
          onLoadNextPage = { dispatch(GithubSearchAction.LoadNextPage) }
        )
      }
    }
  }
}

@SuppressLint("ComposeModifierReused")
@Composable
internal fun GithubRepoItemsSearchContent(
  state: GithubSearchState,
  onRetry: () -> Unit,
  onLoadNextPage: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    if (state.isFirstPage && state.isLoading) {
      return LoadingIndicator()
    }

    val error = state.error
    if (state.isFirstPage && error != null) {
      return RetryButton(
        errorMessage = error.getReadableMessage(),
        onRetry = onRetry
      )
    }

    if (state.items.isEmpty()) {
      return Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = if (state.term.isNotBlank()) {
            "Empty results"
          } else {
            "Search github repositories..."
          },
          style = MaterialTheme.typography.titleLarge
        )
      }
    }

    GithubRepoItemsList(
      items = state.items,
      isLoading = state.isLoading,
      error = state.error,
      hasReachedMax = state.hasReachedMax,
      onRetry = onRetry,
      onLoadNextPage = onLoadNextPage
    )
  }
}

@Preview(name = "phone", device = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=480")
@Composable
private fun SearchScreenContentPreview() {
  AppTheme {
    AppBackground {
      GithubRepoItemsSearchContent(
        state = GithubSearchState(
          page = FIRST_PAGE,
          term = "term",
          items = persistentListOf<RepoItem>()
            .addAll(
              (0 until 50).map {
                RepoItem(
                  id = it,
                  fullName = "ReactiveX/rxdart $it",
                  language = null,
                  starCount = 0,
                  name = "rxdart $it",
                  repoDescription = null,
                  languageColor = null,
                  htmlUrl = "",
                  owner = Owner(
                    id = 0,
                    username = "",
                    avatar = ""
                  ),
                  updatedAt = Clock.System.now()
                )
              }
            ),
          isLoading = false,
          error = null,
          hasReachedMax = false
        ),
        onRetry = {},
        onLoadNextPage = {},
      )
    }
  }
}
