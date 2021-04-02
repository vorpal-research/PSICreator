package com.spbpu.mppconverter.kootstrap

import com.intellij.lang.java.JavaLanguage
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.pom.PomModel
import com.intellij.pom.PomTransaction
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import com.spbpu.mppconverter.kootstrap.FooBarCompiler.setupMyCfg
import com.spbpu.mppconverter.kootstrap.FooBarCompiler.setupMyEnv
import com.spbpu.mppconverter.kootstrap.util.opt
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File


@Suppress("DEPRECATION")
class PSICreator() {

    private fun setupMyEnv(cfg: CompilerConfiguration): KotlinCoreEnvironment {

        val disposable = Disposer.newDisposable()
        //Use for windows
        System.setProperty("idea.use.native.fs.for.win", "false")

        val env = KotlinCoreEnvironment.createForProduction(
            disposable,
            cfg,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val project = env.project as MockProject
        project.registerService(
            TreeAspect::class.java
        )

        class MyPomModelImpl(env: KotlinCoreEnvironment) : PomModelImpl(env.project) {
            override fun runTransaction(pt: PomTransaction) = pt.run()
        }


        val pomModel = MyPomModelImpl(env)

        project.registerService(
            PomModel::class.java,
            pomModel
        )
        return env
    }


    fun getJavaFiles(projectDir: String): List<PsiFile> {
        findAndCreateJavaFiles(projectDir)
        return javaFiles
    }

    private fun findAndCreateJavaFiles(projectDir: String) {
        val folder = File(projectDir)
        for (entry in folder.listFiles()) {
            if (entry.isDirectory) {
                findAndCreateJavaFiles(entry.absolutePath)
            } else if (entry.name.endsWith(".java")) {
                val javaFile =
                    PsiFileFactory.getInstance(env.project).createFileFromText(JavaLanguage.INSTANCE, entry.readText())
                javaFiles.add(javaFile)
            }
        }
    }

    fun getPsiForJava(text: String, proj: Project = Factory.file.project) =
        PsiFileFactory.getInstance(proj).createFileFromText(JavaLanguage.INSTANCE, text)


    fun getPSIForFile(path: String, generateCtx: Boolean = true): KtFile {
        val newArgs = arrayOf("-t", path)

        val cmd = opt.parse(newArgs)

        cfg = setupMyCfg(cmd)
        env = setupMyEnv(cfg)

        if (!Extensions.getRootArea().hasExtensionPoint(TreeCopyHandler.EP_NAME.name)) {
            Extensions.getRootArea().registerExtensionPoint(
                TreeCopyHandler.EP_NAME.name,
                TreeCopyHandler::class.java.canonicalName,
                ExtensionPoint.Kind.INTERFACE
            )
        }

        targetFiles = env.getSourceFiles().map {
            val f = KtPsiFactory(it).createFile(it.virtualFile.path, it.text)
            f.originalFile = it
            f
        }


        val file = targetFiles.first()
        val configuration = env.configuration.copy()

        configuration.put(CommonConfigurationKeys.MODULE_NAME, "sample")

        if (generateCtx) {
            try {
                val tmpCtx =
                    TopDownAnalyzerFacadeForJS.analyzeFiles(
                        listOf(file),
                        JsConfig(env.project, configuration)
                    ).bindingContext
                ctx = tmpCtx
            } catch (e: Throwable) {
                ctx = null
                return targetFiles.first()
            }
        }
        return targetFiles.first()
    }


    companion object {
        fun analyze(psiFile: PsiFile): BindingContext? {
            val cmd = opt.parse(arrayOf())
            val cfg = setupMyCfg(cmd)
            val env = setupMyEnv(cfg)
            val configuration = env.configuration.copy()
            configuration.put(CommonConfigurationKeys.MODULE_NAME, "root")
            return TopDownAnalyzerFacadeForJS.analyzeFiles(
                (listOf(psiFile as KtFile)),
                JsConfig(env.project, configuration)
            ).bindingContext
        }
    }

    var targetFiles: List<KtFile> = listOf()
    private var javaFiles = mutableListOf<PsiFile>()
    private lateinit var cfg: CompilerConfiguration
    private lateinit var env: KotlinCoreEnvironment
    var ctx: BindingContext? = null
}