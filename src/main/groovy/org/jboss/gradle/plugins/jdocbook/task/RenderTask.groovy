/*
 * jDocBook, processing of DocBook sources
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */



package org.jboss.gradle.plugins.jdocbook.task

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.util.ObservableUrlClassLoader
import org.jboss.gradle.plugins.jdocbook.JDocBookPlugin
import org.jboss.gradle.plugins.jdocbook.book.Book
import org.jboss.jdocbook.render.RenderingSource
import org.gradle.api.tasks.*

/**
 * Task for performing DocBook rendering.
 *
 * @author Strong Liu
 */
class RenderTask extends BookTask implements RenderingSource {
    static final Logger log = Logging.getLogger(RenderTask);
    def format;

    public void configure(Book book, def language, def format) {
        configure(book, language)
        this.format = format
    }

    @Input public String getFormatName() { format.name }

    @Input @Optional public String getFormatFinalName() { format.finalName }

    @Input @Optional public String getStylesheet() { return format.stylesheet }


    @TaskAction
    public void render() {
        prepareForRendering()
        log.lifecycle("rendering Book({}) {}/{}", book.name, lang, format.name);
        book.componentRegistry.renderer.render(this, format)
    }

    private static boolean scriptClassLoaderExtended = false;

    private void prepareForRendering() {
        if (scriptClassLoaderExtended) {
            return;
        }
        scriptClassLoaderExtended = true;
        log.lifecycle("Extending script classloader with the {} dependencies", JDocBookPlugin.STYLES_CONFIG_NAME);
        ClassLoader classloader = project.buildscript.classLoader
        if (classloader instanceof ObservableUrlClassLoader) {
            ObservableUrlClassLoader scriptClassloader = (ObservableUrlClassLoader) classloader;
            for (File file: project.configurations.getByName(JDocBookPlugin.STYLES_CONFIG_NAME).getFiles()) {
                try {
                    scriptClassloader.addURL(file.toURI().toURL());
                }
                catch (MalformedURLException e) {
                    log.warn("Unable to retrieve file url [" + file.getAbsolutePath() + "]; ignoring");
                }
            }
            Thread.currentThread().setContextClassLoader(classloader)
        }
    }

    @Override
    @InputDirectory
    @Optional
    File getXslFoDirectory() {
        existsOrNull(book.environment.getWorkDirPerLang("xsl-fo"))
    }

    @InputFile
    public File getSourceDocument() {
        if (book.profiling.enabled) {
            return new File(book.environment.getProfileDirPerLang(lang), book.masterSourceDocumentName)
        }
        else if (lang == book.masterLanguage) {
            return book.environment.rootDocumentFile
        }
        else {
            return new File(book.environment.getWorkDirPerLang(lang), book.masterSourceDocumentName)
        }
    }

    public File resolveSourceDocument() { getSourceDocument() }

    @OutputDirectory
    File getPublishingBaseDirectory() { resolvePublishingBaseDirectory() }

    File resolvePublishingBaseDirectory() {
        book.environment.getPublishDirPerLang(lang)
    }
}
