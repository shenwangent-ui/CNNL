package com.shenhan.app.data

import com.shenhan.app.model.QuestZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CinemaGameBridge {
    private val _activeCinemaNodeId = MutableStateFlow<String?>(null)
    val activeCinemaNodeId: StateFlow<String?> = _activeCinemaNodeId.asStateFlow()

    private val _questZones = MutableStateFlow<List<QuestZone>>(emptyList())
    val questZones: StateFlow<List<QuestZone>> = _questZones.asStateFlow()

    private val _isLiveSupabaseData = MutableStateFlow(false)
    val isLiveSupabaseData: StateFlow<Boolean> = _isLiveSupabaseData.asStateFlow()

    fun updateQuestZones(zones: List<QuestZone>, isLive: Boolean = true) {
        if (zones.isNotEmpty()) {
            _questZones.value = zones
            if (isLive) {
                _isLiveSupabaseData.value = true
            }
        }
    }

    fun syncQuestZones(zones: List<QuestZone>) {
        updateQuestZones(zones, isLive = true)
    }

    fun openNodeInCinema(nodeId: String) {
        _activeCinemaNodeId.value = nodeId
    }

    fun clearActiveCinemaNode() {
        _activeCinemaNodeId.value = null
    }

    fun completeQuestNode(nodeId: String, stars: Int) {
        val currentZones = _questZones.value
        if (currentZones.isEmpty()) return
        var unlockedNext = false
        val updatedZones = currentZones.map { zone ->
            val updatedNodes = zone.nodes.map { node ->
                when {
                    node.id == nodeId -> {
                        unlockedNext = true
                        node.copy(
                            starsEarned = maxOf(node.starsEarned, stars.coerceIn(1, 3)),
                            isUnlocked = true,
                            isCurrent = false
                        )
                    }
                    unlockedNext -> {
                        unlockedNext = false
                        node.copy(isUnlocked = true, isCurrent = true)
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
