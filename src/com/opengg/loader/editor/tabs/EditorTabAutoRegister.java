package com.opengg.loader.editor.tabs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to automatically register the annotated class as an editor panel.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface EditorTabAutoRegister {
}
