package com.methodica.app.domain.ai.local

data class ReasoningScope(
    val estimatedScope: String,
    val justification: String,
    val confidence: Float,
    val requiresUserConfirmation: Boolean
)

data class ReasoningTopicComplexity(
    val topicName: String,
    val complexityLevel: Int,
    val recommendedHours: Int,
    val priority: Int,
    val isIncludedInScope: Boolean,
    val requiresPractice: Boolean,
    val requiresSpacedReview: Boolean,
    val rationale: String
)

data class ReasoningSequencingStep(
    val order: Int,
    val topicName: String,
    val why: String
)

data class ReasoningPlanOutput(
    val scope: ReasoningScope,
    val topicComplexities: List<ReasoningTopicComplexity>,
    val sequencing: List<ReasoningSequencingStep>,
    val risks: List<String>,
    val summary: String,
    val confidence: Float
)
