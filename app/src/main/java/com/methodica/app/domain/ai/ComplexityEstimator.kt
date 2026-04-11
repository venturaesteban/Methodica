package com.methodica.app.domain.ai

import com.methodica.app.domain.ai.model.ComplexityAnalysis
import com.methodica.app.domain.ai.model.ParsedDocument
import com.methodica.app.domain.model.Topic

interface ComplexityEstimator {
    suspend fun estimate(
        parsedDocument: ParsedDocument,
        topics: List<Topic>
    ): ComplexityAnalysis
}
