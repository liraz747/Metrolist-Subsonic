package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.subsonic.Subsonic
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor() : ViewModel() {
    private val _chartsPage = MutableStateFlow<ChartsPage?>(null)
    val chartsPage = _chartsPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadCharts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val sections = mutableListOf<ChartsPage.ChartSection>()

            Subsonic.getAlbumList2("newest").onSuccess {
                sections.add(ChartsPage.ChartSection(title = "Newest", items = it, chartType = ChartsPage.ChartType.NEW_RELEASES))
            }.onFailure {
                reportException(it)
            }

            Subsonic.getAlbumList2("frequent").onSuccess {
                sections.add(ChartsPage.ChartSection(title = "Most Played", items = it, chartType = ChartsPage.ChartType.TOP))
            }.onFailure {
                reportException(it)
            }

            Subsonic.getAlbumList2("random").onSuccess {
                sections.add(ChartsPage.ChartSection(title = "Random", items = it, chartType = ChartsPage.ChartType.GENRE))
            }.onFailure {
                reportException(it)
            }

            _chartsPage.value = ChartsPage(sections = sections, continuation = null)
            _isLoading.value = false
        }
    }
}
