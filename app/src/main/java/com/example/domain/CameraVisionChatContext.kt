package com.example.domain

internal fun buildCameraVisionChatContext(
    modeTitle: String,
    modelUsed: String,
    analysis: String,
    maxChars: Int = 11_900
): String {
    require(maxChars > 0) { "Maximum chat context length must be positive." }
    return (
        "[CAMERA EYES · ${modeTitle}]\n" +
            "User-approved visual analysis from ${modelUsed}:\n${analysis}"
        ).take(maxChars)
}
