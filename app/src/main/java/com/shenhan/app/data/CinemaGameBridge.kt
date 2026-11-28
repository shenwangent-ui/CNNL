package com.shenhan.app.data

import com.shenhan.app.model.QuestZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CinemaGameBridge {
    private val _questZones = MutableStateFlow<List<QuestZone>>(emptyList())
    val questZones: StateFlow<List<QuestZone>> = _questZones.asStateFlow()

    private val _activeCinemaNodeId = MutableStateFlow<String?>(null)
    val activeCinemaNodeId: StateFlow<String?> = _activeCinemaNodeId.asStateFlow()

    private val _isLiveSupabaseData = MutableStateFlow(false)
    val isLiveSupabaseData: StateFlow<Boolean> = _isLiveSupabaseData.asStateFlow()

    private val _customPacks = MutableStateFlow<List<CustomVocabPack>>(emptyList())
    val customPacks: StateFlow<List<CustomVocabPack>> = _customPacks.asStateFlow()

    private val _selectedCustomPackId = MutableStateFlow<String?>(null)
    val selectedCustomPackId: StateFlow<String?> = _selectedCustomPackId.asStateFlow()

    private val _requestedModeIndex = MutableStateFlow<Int?>(null)
    val requestedModeIndex: StateFlow<Int?> = _requestedModeIndex.asStateFlow()

    fun updateQuestZones(zones: List<QuestZone>) {
        _questZones.value = zones
    }

    fun updateCustomPacks(packs: List<CustomVocabPack>) {
        _customPacks.value = packs
        if (_selectedCustomPackId.value != null && packs.none { it.id == _selectedCustomPackId.value }) {
            _selectedCustomPackId.value = null
        }
    }

    fun selectCustomPackFilter(packId: String?) {
        _selectedCustomPackId.value = packId
    }

    fun openCustomPackInCinema(packId: String, modeIndex: Int) {
        _selectedCustomPackId.value = packId
        _activeCinemaNodeId.value = "${CustomVocabPackManager.CUSTOM_NODE_PREFIX}$packId"
        _requestedModeIndex.value = modeIndex
    }

    fun clearRequestedMode() {
        _requestedModeIndex.value = null
    }

    fun syncQuestZones(zones: List<QuestZone>, isLive: Boolean = true) {
        _questZones.value = zones
        if (isLive) {
            _isLiveSupabaseData.value = true
        }
    }

    fun openNodeInCinema(nodeId: String) {
        _activeCinemaNodeId.value = nodeId
    }

    fun clearActiveCinemaNode() {
        _activeCinemaNodeId.value = null
    }

    fun completeQuestNode(nodeId: String, stars: Int) {
        val currentZones = _questZones.value
        val clampedStars = stars.coerceIn(1, 3)
        var unlockedNext = false
        val updatedZones = currentZones.map { zone ->
            val updatedNodes = zone.nodes.map { node ->
                when {
                    node.id == nodeId -> {
                        unlockedNext = true
                        node.copy(
                            starsEarned = maxOf(node.starsEarned, clampedStars),
                            isUnlocked = true,
                            isCurrent = false
                        )
                    }
                    unlockedNext -> {
                        unlockedNext = false
                        node.copy(
                            isUnlocked = true,
                            isCurrent = node.starsEarned == 0
                        )
                    }
                    else -> node
                }
            }
            zone.copy(
                isUnlocked = zone.isUnlocked || updatedNodes.any { it.isUnlocked },
                nodes = updatedNodes
            )
        }
        _questZones.value = updatedZones
    }
}
