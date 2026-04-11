package com.methodica.app.feature.materials

import android.net.Uri
import com.methodica.app.core.ai.AiDocumentProcessingPolicy
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.methodica.app.core.navigation.MethodicaDestination
import com.methodica.app.domain.model.Material
import com.methodica.app.domain.model.MaterialType
import com.methodica.app.domain.usecase.material.GetMaterialUseCase
import com.methodica.app.domain.usecase.material.UpsertMaterialUseCase
import com.methodica.app.domain.usecase.subject.ObserveSubjectsUseCase
import com.methodica.app.domain.usecase.topic.ObserveTopicsBySubjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MaterialFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeSubjectsUseCase: ObserveSubjectsUseCase,
    private val observeTopicsBySubjectUseCase: ObserveTopicsBySubjectUseCase,
    private val getMaterialUseCase: GetMaterialUseCase,
    private val upsertMaterialUseCase: UpsertMaterialUseCase
) : ViewModel() {

    private val materialId: Long? =
        savedStateHandle.get<Long>(MethodicaDestination.MaterialForm.ARG_MATERIAL_ID)
            ?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(MaterialFormUiState())
    val uiState: StateFlow<MaterialFormUiState> = _uiState.asStateFlow()

    private var topicsJob: Job? = null

    init {
        observeSubjects()
    }

    private fun observeSubjects() {
        viewModelScope.launch {
            observeSubjectsUseCase().collect { subjects ->
                val previous = _uiState.value
                val selectedSubject = subjects.firstOrNull { it.id == previous.selectedSubjectId }
                val selectedDegreeId = selectedSubject?.degreeId ?: previous.selectedDegreeId
                    ?: subjects.firstOrNull()?.degreeId
                val selectedCourseYear = selectedSubject?.courseYear ?: previous.selectedCourseYear
                    ?: subjects.firstOrNull { it.degreeId == selectedDegreeId }?.courseYear
                val resolvedSubject = selectedSubject ?: subjects.firstOrNull {
                    it.degreeId == selectedDegreeId && it.courseYear == selectedCourseYear
                }

                _uiState.update { state ->
                    state.copy(
                        subjects = subjects,
                        selectedDegreeId = selectedDegreeId,
                        selectedCourseYear = selectedCourseYear,
                        selectedSubjectId = resolvedSubject?.id,
                        isLoading = false
                    )
                }

                val selectedSubjectId = resolvedSubject?.id
                if (selectedSubjectId != null) {
                    observeTopics(selectedSubjectId)
                    if (materialId != null && _uiState.value.title.isBlank()) {
                        loadMaterial(materialId)
                    }
                }
            }
        }
    }

    private fun loadMaterial(id: Long) {
        viewModelScope.launch {
            val material = getMaterialUseCase(id) ?: return@launch
            val subject = _uiState.value.subjects.firstOrNull { it.id == material.subjectId }
            _uiState.update {
                it.copy(
                    title = material.title,
                    uri = material.uri,
                    type = material.type,
                    selectedDegreeId = subject?.degreeId,
                    selectedCourseYear = subject?.courseYear,
                    selectedSubjectId = material.subjectId,
                    selectedTopicId = material.topicId,
                    isLoading = false
                )
            }
            observeTopics(material.subjectId)
        }
    }

    private fun observeTopics(subjectId: Long) {
        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            observeTopicsBySubjectUseCase(subjectId).collect { topics ->
                _uiState.update { state ->
                    val selectedTopic = state.selectedTopicId
                    val validTopic = if (selectedTopic != null && topics.any { it.id == selectedTopic }) {
                        selectedTopic
                    } else {
                        null
                    }
                    state.copy(topics = topics, selectedTopicId = validTopic)
                }
            }
        }
    }

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(title = value, titleError = null) }

    fun onUriChange(value: String) =
        _uiState.update { it.copy(uri = value, uriError = null, pickedFileMimeType = null) }

    fun onPickedLocalFile(uri: String, mimeType: String?) {
        if (!isSupportedUpload(mimeType, uri)) {
            _uiState.update {
                it.copy(
                    uriError = "Solo se aceptan archivos de texto, imagen con texto o PDF",
                    error = "Tipo de archivo no compatible para analisis IA"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                uri = uri,
                uriError = null,
                error = null,
                pickedFileMimeType = mimeType,
                type = MaterialType.FILE_URI
            )
        }
    }

    fun onTypeChange(value: MaterialType) =
        _uiState.update {
            it.copy(
                type = value,
                pickedFileMimeType = if (value == MaterialType.FILE_URI) it.pickedFileMimeType else null
            )
        }

    fun onSelectDegree(degreeId: Long) {
        _uiState.update { state ->
            val nextCourseYear = state.subjects
                .firstOrNull { it.degreeId == degreeId }
                ?.courseYear
            val nextSubjectId = state.subjects.firstOrNull {
                it.degreeId == degreeId && it.courseYear == nextCourseYear
            }?.id
            state.copy(
                selectedDegreeId = degreeId,
                selectedCourseYear = nextCourseYear,
                selectedSubjectId = nextSubjectId,
                selectedTopicId = null
            )
        }
        _uiState.value.selectedSubjectId?.let { observeTopics(it) }
    }

    fun onSelectCourseYear(courseYear: Int) {
        _uiState.update { state ->
            val degreeId = state.selectedDegreeId
            val nextSubjectId = state.subjects.firstOrNull {
                (degreeId == null || it.degreeId == degreeId) && it.courseYear == courseYear
            }?.id
            state.copy(
                selectedCourseYear = courseYear,
                selectedSubjectId = nextSubjectId,
                selectedTopicId = null
            )
        }
        _uiState.value.selectedSubjectId?.let { observeTopics(it) }
    }

    fun onSelectSubject(subjectId: Long) {
        _uiState.update { state ->
            val selected = state.subjects.firstOrNull { it.id == subjectId }
            state.copy(
                selectedDegreeId = selected?.degreeId ?: state.selectedDegreeId,
                selectedCourseYear = selected?.courseYear ?: state.selectedCourseYear,
                selectedSubjectId = subjectId,
                selectedTopicId = null
            )
        }
        observeTopics(subjectId)
    }

    fun onSelectTopic(topicId: Long?) =
        _uiState.update { it.copy(selectedTopicId = topicId) }

    fun onSave() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "El título no puede estar vacío") }
            return
        }
        if (state.uri.isBlank()) {
            _uiState.update { it.copy(uriError = "Debes indicar un enlace o URI") }
            return
        }
        if (state.type == MaterialType.FILE_URI && !isSupportedUpload(state.pickedFileMimeType, state.uri)) {
            _uiState.update {
                it.copy(uriError = "Solo se aceptan archivos de texto, imagen con texto o PDF")
            }
            return
        }
        val subjectId = state.selectedSubjectId
        if (subjectId == null || subjectId <= 0L) {
            _uiState.update { it.copy(error = "Debes seleccionar una asignatura") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val material = Material(
                id = materialId ?: 0L,
                subjectId = subjectId,
                topicId = state.selectedTopicId,
                title = state.title.trim(),
                uri = state.uri.trim(),
                type = state.type
            )
            val result = upsertMaterialUseCase(material)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    private fun isSupportedUpload(mimeType: String?, uri: String): Boolean {
        val normalizedMime = mimeType?.lowercase().orEmpty()
        val byMime = normalizedMime.isNotBlank() && (
            AiDocumentProcessingPolicy.SUPPORTED_UPLOAD_MIME_EXACT.contains(normalizedMime) ||
                AiDocumentProcessingPolicy.SUPPORTED_UPLOAD_MIME_PREFIXES.any { prefix ->
                    normalizedMime.startsWith(prefix)
                }
            )

        val extension = Uri.parse(uri)
            .lastPathSegment
            .orEmpty()
            .substringAfterLast('.', "")
            .lowercase()
        val byExtension = extension in AiDocumentProcessingPolicy.SUPPORTED_UPLOAD_EXTENSIONS

        return byMime || byExtension
    }
}
