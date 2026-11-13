package com.nikii321.fieldpilot.model.response

data class AnalysisResponse(
    val project: String,
    val units: String,
    val horizonDays: Int,
    val request: com.nikii321.fieldpilot.model.request.AnalysisRequest,
    val ahp: AhpResult,
    val wells: List<WellEvaluation>,
    val plan: PlanResult,
    val uncertainty: ScenarioResult,
    val recommendations: List<Recommendation>,
    val model: com.nikii321.fieldpilot.ml.model.ModelArtifact,
    val knowledgeRules: List<KnowledgeRule>,
    val alternativeMatrices: List<List<List<Double>>>,
    val warnings: List<String>,
)
