package com.methodica.app.domain.ai

import com.methodica.app.domain.ai.model.ExamScopeInference
import com.methodica.app.domain.ai.model.ParsedDocument
import com.methodica.app.domain.model.Assessment

interface ExamScopeInferenceService {
    suspend fun infer(
        assessment: Assessment,
        parsedDocument: ParsedDocument
    ): ExamScopeInference
}
