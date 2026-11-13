package com.nikii321.fieldpilot.model.request

import com.nikii321.fieldpilot.domain.WellAction

data class ActionSetRequest(val wellVersion: Int, val actions: List<WellAction>)
